package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.mysql.fabric.xmlrpc.base.Array;
import com.vaadin.data.TreeData;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ComponentRenderer;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.shared.ui.label.ContentMode;
import com.vaadin.v7.ui.Table;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.queryengine.parser.CatmaQueryParser.phrase_return;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.PhraseResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResult;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

public class ResultPanelNew extends Panel {

	private static enum TreePropertyName {
		caption, frequency, visibleInKwic,;
	}

	private VerticalLayout contentVerticalLayout;
	private Table queryResultTable;
	private TreeGrid<GroupedQueryResult> phraseTreeGrid;
	private TreeGrid<TagRowItem> tagTreeGrid;
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

	public ResultPanelNew(Repository repository, QueryResult result, String queryAsString) {

		this.repository = repository;
		this.queryResult = result;
		// this.tagQueryResult= (TagQueryResult) result;
		this.queryAsString = queryAsString;

		initComponents();
		initListeners();
		// setData();
		// setDataPhraseStyle();
		try {
			setDataTagStyle();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void initComponents() {
		contentVerticalLayout = new VerticalLayout();
		setContent(contentVerticalLayout);
		createResultInfoBar();
		createButtonBar();
	}

	private void initListeners() {
		caretDownBt.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				contentVerticalLayout.addComponent(tagTreeGrid);
				groupedIcons.replaceComponent(caretDownBt, caretUpBt);

			}
		});

		caretUpBt.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				contentVerticalLayout.removeComponent(tagTreeGrid);
				groupedIcons.replaceComponent(caretUpBt, caretDownBt);

			}
		});
	}

	private void setData() {
		QueryResultRowArray resultRowArrayArrayList = queryResult.asQueryResultRowArray();
		queryResultTable = new Table("Results");
		queryResultTable.addContainerProperty("Phrase", String.class, null);
		queryResultTable.addContainerProperty("Range", String.class, null);

		for (QueryResultRow queryResultRow : resultRowArrayArrayList) {
			Object newItemId = queryResultTable.addItem();
			Item row1 = queryResultTable.getItem(newItemId);

			row1.getItemProperty("Phrase").setValue(queryResultRow.getPhrase());
			row1.getItemProperty("Range").setValue(queryResultRow.getRange().toString());
		}
		queryResultTable.setWidth("100%");

	}

	private void setDataTagStyle() throws Exception {

		TreeData<TagRowItem> tagData = new TreeData<>();
		tagTreeGrid = new TreeGrid<>();
		tagTreeGrid.setSelectionMode(SelectionMode.MULTI);

		// Set<GroupedQueryResult> groupedQueryResults=queryResult.asGroupedSet();
		// QueryResultRowArray tagRowsArray=queryResult.asQueryResultRowArray();

		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(
				populateTree(repository, tagData, queryResult));

		tagTreeGrid.addColumn(TagRowItem::getTreePath).setCaption("Tag");

		tagTreeGrid.addColumn(TagRowItem::getFrequency).setCaption("Frequency");
		
		dataProvider.refreshAll();
		tagTreeGrid.setDataProvider(dataProvider);
		tagTreeGrid.setWidth("100%");

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
			}else {
				tagsAsRoot.stream().filter(x -> x.getTagDefinitionPath().equals(tagRowItem.getTagDefinitionPath()))
		        .findFirst().get().setFrequencyOneUp();
			}
		}
		treeData.addItems(null, tagsAsRoot);

		// adding documents as children for tags
		for (TagRowItem tag : tagsAsRoot) {

			ArrayList<TagRowItem> docsForATag = new ArrayList<TagRowItem>();
			String rootTagPath = tag.getTagDefinitionPath();

			for (QueryResultRow queryResultRow : queryResult) {

				TagQueryResultRow tagQueryResultRow = (TagQueryResultRow) queryResultRow;

				if (rootTagPath.equalsIgnoreCase(tagQueryResultRow.getTagDefinitionPath())) {
					TagRowItem docItem = new TagRowItem();
					docItem.setSourceDocumentID(queryResultRow.getSourceDocumentId());
					docItem.setSourceDocName(repository.getSourceDocument(queryResultRow.getSourceDocumentId()).toString());
					docItem.setCollectionID(tagQueryResultRow.getMarkupCollectionId());
					
					
					docItem.setTagDefinitionPath(tagQueryResultRow.getTagDefinitionPath());
					docItem.setTreePath(docItem.getSourceDocName());
				//	docItem.setTagDefinitionPath(repository.getSourceDocument(queryResultRow.getSourceDocumentId()).toString());
					
					// Name des docs als tagpath.. 
					//docItem.setTagDefinitionPath(repository.getSourceDocument(queryResultRow.getSourceDocumentId()).toString());
					
					
					if (!docsForATag.stream().anyMatch(
							var -> var.getSourceDocumentID().equalsIgnoreCase(docItem.getSourceDocumentID()))) {
						docsForATag.add(docItem);
					}
				}
			}

			treeData.addItems(tag, docsForATag);

			// adding collections as children for documents
			for (TagRowItem oneDoc : docsForATag) {
				// query result holen in der die collection der tag und das doc zusammen
				// auftreten und dese collection als tagrowitem in die liste speichern
				ArrayList<TagRowItem> collectionsForADocument = new ArrayList<TagRowItem>();
				
				for (QueryResultRow queryResultRow : queryResult) {

					TagQueryResultRow tagQueryResultRow = (TagQueryResultRow) queryResultRow;

					if ((tagQueryResultRow.getTagDefinitionPath().equalsIgnoreCase(oneDoc.getTagDefinitionPath()))
						//	&& (tagQueryResultRow.getMarkupCollectionId().equalsIgnoreCase(oneDoc.getCollectionID()))
							&& (tagQueryResultRow.getSourceDocumentId()
									.equalsIgnoreCase(oneDoc.getSourceDocumentID()))) {

						// int tagiInstancesPerCollection=0;
						TagRowItem tagRowItem = new TagRowItem();
						// tagRowItem.setTagDefinitionPath(tagQueryResultRow.getTagDefinitionPath());
						// tagRowItem.setSourceDocumentID((tagQueryResultRow.getSourceDocumentId()));
						tagRowItem.setCollectionID(tagQueryResultRow.getMarkupCollectionId());
						
						SourceDocument sourceDoc=	repository.getSourceDocument(queryResultRow.getSourceDocumentId());
						tagRowItem.setCollectionName(sourceDoc.getUserMarkupCollectionReference(tagQueryResultRow.getMarkupCollectionId()).getName());
						tagRowItem.setTagDefinitionPath(sourceDoc.getUserMarkupCollectionReference(tagQueryResultRow.getMarkupCollectionId()).getName());
						tagRowItem.setTreePath(tagRowItem.getCollectionName());
					

						if (!collectionsForADocument.stream().anyMatch(
								var -> var.getCollectionID().equalsIgnoreCase(tagRowItem.getCollectionID()))) {
							collectionsForADocument.add(tagRowItem);
						}

					}

				}

				treeData.addItems(oneDoc, collectionsForADocument);
			}

		}

		return treeData;
	}

	private void setDataPhraseStyle() {

		Set<GroupedQueryResult> groupedQueryResults = queryResult.asGroupedSet();
		TreeData<GroupedQueryResult> data = new TreeData<>();
		phraseTreeGrid = new TreeGrid<>();
		data.addItems(null, groupedQueryResults);

		for (GroupedQueryResult groupedQueryResult : groupedQueryResults) {

			Set<String> sourceDocs = groupedQueryResult.getSourceDocumentIDs();
			// Iterator<QueryResultRow> rowIterator= groupedQueryResult.iterator();

			Iterator<String> docIterator = sourceDocs.iterator();
			// int docsize = sourceDocs.size();

			while (docIterator.hasNext()) {
				String surceDocID = docIterator.next().toString();
				GroupedQueryResult subResults = groupedQueryResult.getSubResult(surceDocID);
				Iterator<QueryResultRow> rowIterator = subResults.iterator();
				while (rowIterator.hasNext()) {
					data.addItem(groupedQueryResult, (GroupedQueryResult) rowIterator.next());
				}

			}

			phraseTreeGrid.setDetailsVisible(groupedQueryResult, true);
		}

		TreeDataProvider<GroupedQueryResult> dataProvider = new TreeDataProvider<>(data);
		phraseTreeGrid.addColumn(GroupedQueryResult::getGroup).setCaption("Phrase");
		phraseTreeGrid.addColumn(GroupedQueryResult::getSourceDocumentIDs).setCaption("SourceDocs");
		phraseTreeGrid.addColumn((GroupedQueryResult::getSubResult)).setCaption("Subresults");
		// phraseTreeGrid.addColumn((GroupedQueryResult::getFrequency)).setCaption("Subresults");
		// phraseTreeGrid.addColumn(getCheckbox).setCaption("select")

		phraseTreeGrid.setDataProvider(dataProvider);
		phraseTreeGrid.setWidth("100%");
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
