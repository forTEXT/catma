package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.Set;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.ValoTheme;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;

public class ResultPanelNew extends Panel {

	private static enum TreePropertyName {
		caption, frequency, visibleInKwic,;
	}
	private static enum ViewID {
		phrase, tag, property,phraseTag, phraseProperty;
	}

	private VerticalLayout contentVerticalLayout;
	private TreeGrid<TagRowItem> treeGridTag;
	private TreeGrid<TagRowItem> treeGridPhrase;
	private TreeGrid<TagRowItem> treeGridProperty;
	private Label queryInfo;
	private HorizontalLayout groupedIcons;
	private Button caretDownBt;
	private Button caretUpBt;
	private Button trashBt;
	private Button optionsBt;
	private Panel treeGridPanel;
	private QueryResult queryResult;
	private String queryAsString;
	private Repository repository;
	private boolean twoGridViews;
	private boolean tagView;
	private boolean propView;
	private ViewID currentView;

	public ResultPanelNew(Repository repository, QueryResult result, String queryAsString) throws Exception {

		this.repository = repository;
		this.queryResult = result;
		this.queryAsString = queryAsString;

		initComponents();
		initListeners();

		if (queryAsString.contains("tag=")) {
			setDataTagStyle();
			setCurrentView(ViewID.tag);

			treeGridPanel.setContent(treeGridTag);
		}

		if (queryAsString.contains("property=")) {

			setDataPropertyStyle();
			setCurrentView(ViewID.property);

			treeGridPanel.setContent(treeGridProperty);
		}
		if (queryAsString.contains("wild=")) {

			setDataPhraseStyle();
			setCurrentView(ViewID.phrase);
			treeGridPanel.setContent(treeGridPhrase);
		}

	}

	private ViewID getCurrentView() {
		return currentView;
	}

	private void setCurrentView(ViewID currentView) {
		this.currentView = currentView;
	}

	private void initComponents() {
		contentVerticalLayout = new VerticalLayout();
		setContent(contentVerticalLayout);

		treeGridTag = new TreeGrid<TagRowItem>();
		treeGridTag.setSelectionMode(SelectionMode.MULTI);

		treeGridPhrase = new TreeGrid<TagRowItem>();
		treeGridPhrase.setSelectionMode(SelectionMode.MULTI);

		treeGridProperty = new TreeGrid<TagRowItem>();
		treeGridProperty.setSelectionMode(SelectionMode.MULTI);

		createResultInfoBar();
		createButtonBar();
		treeGridPanel = new Panel();
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

	private void initListeners() {

		caretDownBt.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				contentVerticalLayout.addComponent(treeGridPanel);
				groupedIcons.replaceComponent(caretDownBt, caretUpBt);

			}
		});

		caretUpBt.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				contentVerticalLayout.removeComponent(treeGridPanel);
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
				// remove this instance of resultPanelNew
			}
		});
	}

	private void setDataTagStyle() throws Exception {

		TreeData<TagRowItem> tagData = new TreeData<>();
		tagData = populateTreeDataWithTags(repository, tagData, queryResult);
		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(tagData);

		treeGridTag.addColumn(TagRowItem::getTreePath).setCaption("Tag").setId("tagID");
		treeGridTag.getColumn("tagID").setExpandRatio(7);

		treeGridTag.addColumn(TagRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		treeGridTag.getColumn("freqID").setExpandRatio(1);

		dataProvider.refreshAll();
		treeGridTag.setDataProvider(dataProvider);
		treeGridTag.setWidth("100%");
		treeGridPanel.setContent(treeGridTag);
		setDataPhraseStyle();
	}

	private void setDataPhraseStyle() throws Exception {

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

		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(phraseData);

		treeGridPhrase.addColumn(TagRowItem::getTreePath).setCaption("Phrase").setId("phraseID");
		treeGridPhrase.getColumn("phraseID").setExpandRatio(7);

		treeGridPhrase.addColumn(TagRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		treeGridPhrase.getColumn("freqID").setExpandRatio(1);

		dataProvider.refreshAll();

		treeGridPhrase.setDataProvider(dataProvider);
		treeGridPhrase.setWidth("100%");

	}

	private void setDataPropertyStyle() throws Exception {
		TreeData<TagRowItem> propData = new TreeData<>();

		propData = populateTreeDataWithProperties(repository, propData, queryResult); // TODO !!!!!!

		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(propData);

		treeGridProperty.addColumn(TagRowItem::getTreePath).setCaption("Tag").setId("tagID");
		treeGridProperty.getColumn("tagID").setExpandRatio(3);

		treeGridProperty.addColumn(TagRowItem::getPropertyName).setCaption("Property name").setId("propNameID");
		treeGridProperty.getColumn("propNameID").setExpandRatio(3);

		treeGridProperty.addColumn(TagRowItem::getPropertyValue).setCaption("Property value").setId("propValueID");
		treeGridProperty.getColumn("propValueID").setExpandRatio(3);
		
		 treeGridProperty.addColumn(TagRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		 treeGridProperty.getColumn("freqID").setExpandRatio(1);

		dataProvider.refreshAll();
		treeGridProperty.setDataProvider(dataProvider);
		treeGridProperty.setWidth("100%");

		treeGridPanel.setContent(treeGridProperty);
		
		setDataPhraseStyle();
	}

	private TreeData<TagRowItem> populateTreeDataWithTags(Repository repository, TreeData<TagRowItem> treeData,
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

			// ... adding collections as children for documents
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

	private TreeData<TagRowItem> populateTreeDataWithProperties(Repository repository, TreeData<TagRowItem> treeData,
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

			// ... adding collections as children for documents
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

				// adding tag-property instances as children for a collection
				for (TagRowItem oneCollection : collectionsForADocument) {
		
					for (QueryResultRow queryResultRow : queryResult) {

						TagQueryResultRow tagQueryResultRow = (TagQueryResultRow) queryResultRow;
						// search for tags (in that query result all have properties) in the collection
						if ((tag.getTreePath().equalsIgnoreCase(tagQueryResultRow.getTagDefinitionPath()))
								&& (oneDoc.getSourceDocumentID()
										.equalsIgnoreCase(tagQueryResultRow.getSourceDocumentId()))
								&& (oneCollection.getCollectionID()
										.equalsIgnoreCase(tagQueryResultRow.getMarkupCollectionId()))) {
							oneCollection.setFrequencyOneUp();

							TagRowItem propItem = new TagRowItem();
							propItem.setTreePath(tagQueryResultRow.getTagDefinitionPath());
							propItem.setPropertyName(tagQueryResultRow.getPropertyName());
							propItem.setPropertyValue(tagQueryResultRow.getPropertyValue());

							treeData.addItem(oneCollection, propItem);
						}

					}

				}

			}

		}

		return treeData;

	}

	// get docs as children for a phrase
	private ArrayList<TagRowItem> retrieveChilderen(GroupedQueryResult groupedQueryResult) throws Exception {

		Set<String> docsForAPhrase = groupedQueryResult.getSourceDocumentIDs();
		ArrayList<TagRowItem> docItems = new ArrayList<>();
		for (String doc : docsForAPhrase) {
			TagRowItem rowItem = new TagRowItem();
			// get SourceDoc name from sorceDocID
			String docName = retrieveDocumentName(this.repository, doc);
			rowItem.setTreePath(docName);
			rowItem.setFrequency(groupedQueryResult.getFrequency(doc));
			docItems.add(rowItem);

		}
		return docItems;
	}

	private String retrieveDocumentName(Repository repository, String docID) throws Exception {

		return repository.getSourceDocument(docID).toString();
	}

	private void swichView() throws Exception {
		
		switch(currentView){
		
		case tag: 	setCurrentView(ViewID.phraseTag);
		treeGridPanel.setContent(treeGridPhrase);
		break;
		
		case property: 	setCurrentView(ViewID.phraseProperty);
		treeGridPanel.setContent(treeGridPhrase);
		break;
		
		case phrase: Notification.show("no tag view available for that query", Notification.Type.HUMANIZED_MESSAGE);
		break;
		
		case phraseProperty: setCurrentView(ViewID.property);
		treeGridPanel.setContent(treeGridProperty);
		break;
		
		case phraseTag: setCurrentView(ViewID.tag);
		treeGridPanel.setContent(treeGridTag);
		break;
		
		

			

	
		default:
			Notification.show("no view available ", Notification.Type.HUMANIZED_MESSAGE);
			break;
		

						
		
       }
	}
}


