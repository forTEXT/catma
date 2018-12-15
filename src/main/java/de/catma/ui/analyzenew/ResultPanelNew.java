package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.ui.Table;


import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.PhraseResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResult;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.analyzer.Messages;
import de.catma.ui.component.HTMLNotification;

public class ResultPanelNew extends Panel {

	private static enum TreePropertyName {
		caption, frequency, visibleInKwic,;
	}

	private VerticalLayout contentVerticalLayout;
	private Table queryResultTable;
	private TreeGrid<TagRowItem> treeGrid;
	private Grid<QueryResultRow> queryResultGrid;
	private TextArea textArea;
	private Label queryInfo;
	private HorizontalLayout groupedIcons;
	private Button caretDownBt;
	private Button caretUpBt;
	private Button trashBt;
	private Button optionsBt;

	private QueryResult queryResult;
	private TagQueryResult tagQueryResult;
	private String queryAsString;
	private Repository repository;
	private boolean twoGridViews;
	private boolean tagView;

	public ResultPanelNew(Repository repository, QueryResult result, String queryAsString) throws Exception {

		this.repository = repository;
		this.queryResult = result;
		this.queryAsString = queryAsString;

		initComponents();
		initListeners();

		if (queryAsString.contains("tag=")) {
			setDataTagStyle();
			twoGridViews = true;
			tagView= true;
		} else {
			setDataPhraseStyle();
			twoGridViews = false;
		}

	}

	private void initComponents() {
		contentVerticalLayout = new VerticalLayout();
		setContent(contentVerticalLayout);
		treeGrid = new TreeGrid<TagRowItem>();
		treeGrid.setSelectionMode(SelectionMode.MULTI);
		createResultInfoBar();
		createButtonBar();
	}

	private void initListeners() {
		caretDownBt.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				contentVerticalLayout.addComponent(treeGrid);
				groupedIcons.replaceComponent(caretDownBt, caretUpBt);

			}
		});

		caretUpBt.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				contentVerticalLayout.removeComponent(treeGrid);
				groupedIcons.replaceComponent(caretUpBt, caretDownBt);

			}
		});
		
		optionsBt.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
			
				try {
					swichView();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			

			}
		});

		trashBt.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				// remove instance of resultPanelNew

			}
		});
	}
	
	private void swichView() throws Exception {
		
		if(twoGridViews) {
			if(tagView) {
				setDataPhraseStyle();
				tagView= false;
			}else {
				setDataTagStyle();
				tagView= true;
			}
		}
		else {
			 Notification.show("nix zum swichen",
		                Notification.Type.HUMANIZED_MESSAGE);
		}
	
	
	}

	private void setDataTagStyle() throws Exception {
		
		treeGrid.removeAllColumns();

		TreeData<TagRowItem> tagData = new TreeData<>();


		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(
				populateTree(repository, tagData, queryResult));

		treeGrid.addColumn(TagRowItem::getTreePath).setCaption("Tag");

		treeGrid.addColumn(TagRowItem::getFrequency).setCaption("Frequency");

		dataProvider.refreshAll();
		treeGrid.setDataProvider(dataProvider);
		treeGrid.setWidth("100%");

	}
	


	private TreeData<TagRowItem> populateTree(Repository repository, TreeData<TagRowItem> treeData,
			QueryResult queryResult) throws Exception {

		// adding tags as root items
		ArrayList<TagRowItem> tagsAsRoot = new ArrayList<TagRowItem>();

		for (QueryResultRow queryResultRow : queryResult) {

			TagQueryResultRow tagQueryResultRow = (TagQueryResultRow) queryResultRow;
			TagRowItem tagRowItem = new TagRowItem();

			tagRowItem.setTagDefinitionPath(tagQueryResultRow.getTagDefinitionPath());

			if (!tagsAsRoot.stream()
					.anyMatch(var -> var.getTagDefinitionPath().equalsIgnoreCase(tagRowItem.getTagDefinitionPath()))) {

				tagRowItem.setTreePath(tagRowItem.getTagDefinitionPath());
				tagsAsRoot.add(tagRowItem);
			} else {
				tagsAsRoot.stream().filter(x -> x.getTagDefinitionPath().equals(tagRowItem.getTagDefinitionPath()))
						.findFirst().get().setFrequencyOneUp();
			}
		}
		treeData.addItems(null, tagsAsRoot);

		// adding documents as children for tags and adding collections as children for
		// documents
		for (TagRowItem tag : tagsAsRoot) {

			ArrayList<TagRowItem> docsForATag = new ArrayList<TagRowItem>();
			String rootTagPath = tag.getTagDefinitionPath();

			for (QueryResultRow queryResultRow : queryResult) {

				TagQueryResultRow tagQueryResultRow = (TagQueryResultRow) queryResultRow;

				if (rootTagPath.equalsIgnoreCase(tagQueryResultRow.getTagDefinitionPath())) {

					TagRowItem docItem = new TagRowItem();
					docItem.setSourceDocumentID(queryResultRow.getSourceDocumentId());
					docItem.setSourceDocName(
							repository.getSourceDocument(queryResultRow.getSourceDocumentId()).toString());
					docItem.setCollectionID(tagQueryResultRow.getMarkupCollectionId());
					docItem.setTagDefinitionPath(tagQueryResultRow.getTagDefinitionPath());
					docItem.setTreePath(docItem.getSourceDocName());

					if (!docsForATag.stream().anyMatch(
							var -> var.getSourceDocumentID().equalsIgnoreCase(docItem.getSourceDocumentID()))) {
						docsForATag.add(docItem);
					} else {
						docsForATag.stream()
								.filter(var -> var.getTagDefinitionPath().equals(docItem.getTagDefinitionPath()))
								.findFirst().get().setFrequencyOneUp();
					}
				}
			}

			treeData.addItems(tag, docsForATag);

			// adding collections as children for documents
			for (TagRowItem oneDoc : docsForATag) {

				ArrayList<TagRowItem> collectionsForADocument = new ArrayList<TagRowItem>();

				for (QueryResultRow queryResultRow : queryResult) {

					TagQueryResultRow tagQueryResultRow = (TagQueryResultRow) queryResultRow;

					if ((tagQueryResultRow.getTagDefinitionPath().equalsIgnoreCase(oneDoc.getTagDefinitionPath()))
							&& (tagQueryResultRow.getSourceDocumentId()
									.equalsIgnoreCase(oneDoc.getSourceDocumentID()))) {

						TagRowItem tagRowItem = new TagRowItem();
						tagRowItem.setCollectionID(tagQueryResultRow.getMarkupCollectionId());

						SourceDocument sourceDoc = repository.getSourceDocument(queryResultRow.getSourceDocumentId());
						tagRowItem.setCollectionName(sourceDoc
								.getUserMarkupCollectionReference(tagQueryResultRow.getMarkupCollectionId()).getName());
						tagRowItem.setTagDefinitionPath(sourceDoc
								.getUserMarkupCollectionReference(tagQueryResultRow.getMarkupCollectionId()).getName());
						tagRowItem.setTreePath(tagRowItem.getCollectionName());

						if (!collectionsForADocument.stream().anyMatch(
								var -> var.getCollectionID().equalsIgnoreCase(tagRowItem.getCollectionID()))) {
							collectionsForADocument.add(tagRowItem);
						} else {
							collectionsForADocument.stream()
									.filter(x -> x.getTagDefinitionPath().equals(tagRowItem.getTagDefinitionPath()))
									.findFirst().get().setFrequencyOneUp();

						}

					}

				}

				treeData.addItems(oneDoc, collectionsForADocument);
			}

		}

		return treeData;
	}

	private void setDataPhraseStyle() throws Exception {
		treeGrid.removeAllColumns();
		

		Set<GroupedQueryResult> groupedQueryResults = queryResult.asGroupedSet();
		TreeData<TagRowItem> phraseData = new TreeData<>();
	

		ArrayList<TagRowItem> phraseAsRoots = new ArrayList<>();

		// add phrases as roots
		for (GroupedQueryResult groupedQueryResult : groupedQueryResults) {

			String phrase = (String) groupedQueryResult.getGroup();
			TagRowItem rootPhrase = new TagRowItem();
			rootPhrase.setTreePath(phrase);
			rootPhrase.setFrequency(groupedQueryResult.getTotalFrequency());
			phraseAsRoots.add(rootPhrase);
			phraseData.addItems(null, rootPhrase);

			phraseData.addItems(rootPhrase, retrieveChilderen(groupedQueryResult));

		}
	
		/*
		 * docItems= docsForAPhrase.stream().map( temp-> { TagRowItem docItemRow=new
		 * TagRowItem(); docItemRow.setTreePath(temp); }).collect(Collectors.toSet());
		 */

		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(phraseData);
		treeGrid.addColumn(TagRowItem::getTreePath).setCaption("Phrase");
		treeGrid.addColumn(TagRowItem::getFrequency).setCaption("Frequency");

		treeGrid.setDataProvider(dataProvider);
		dataProvider.refreshAll();
		treeGrid.setWidth("100%");
	}

	// get docs as children for a phrase
	private ArrayList<TagRowItem> retrieveChilderen(GroupedQueryResult groupedQueryResult) throws Exception {

		Set<String> docsForAPhrase = groupedQueryResult.getSourceDocumentIDs();
		ArrayList<TagRowItem> docItems = new ArrayList<>();
		for (String doc : docsForAPhrase) {
			TagRowItem rowItem = new TagRowItem();
			// get SourceDoc name from sorceDocID
			String docName= retrieveDocumentName( this.repository ,doc);
			rowItem.setTreePath(docName);
			rowItem.setFrequency(groupedQueryResult.getFrequency(doc));
			docItems.add(rowItem);

		}
		return docItems;
	}
	
	private String retrieveDocumentName(Repository repository ,String docID) throws Exception {
		 return repository.getSourceDocument(docID).toString();
	}

	private CheckBox createCheckbox(final GroupedQueryResult phraseResult) {
		final CheckBox checkBox = new CheckBox();
		// checkBox.setValue(false);

		checkBox.addValueChangeListener(event -> {
			boolean checked = checkBox.getValue();
			Notification.show("Is checked", "is : " + checked, Notification.Type.HUMANIZED_MESSAGE);

		});

		// synchronizeWithSelectedView(phraseResult, selected);

		return checkBox;
	}

	private void createResultInfoBar() {
		QueryResultRowArray resultRowArrayArrayList = queryResult.asQueryResultRowArray();
		int resultSize = resultRowArrayArrayList.size();
		queryInfo = new Label(queryAsString + "(" + resultSize + ")");
		queryInfo.setStyleName("body");
		contentVerticalLayout.addComponent(queryInfo);
	}

	private void createButtonBar() {
		groupedIcons = new HorizontalLayout();
		groupedIcons.setMargin(false);

		caretDownBt = new Button(VaadinIcons.CARET_DOWN);
		caretDownBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		caretUpBt = new Button(VaadinIcons.CARET_UP);
		caretUpBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		optionsBt = new Button(VaadinIcons.ELLIPSIS_V);
		optionsBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		trashBt = new Button(VaadinIcons.TRASH);
		trashBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);

		groupedIcons.addComponents(trashBt, optionsBt, caretDownBt);
		groupedIcons.setWidthUndefined();

		contentVerticalLayout.addComponent(groupedIcons);
		contentVerticalLayout.setComponentAlignment(groupedIcons, Alignment.MIDDLE_RIGHT);
	}

}
