package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
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
import de.catma.queryengine.result.TagQueryResult;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.analyzenew.treehelper.DocumentItem;
import de.catma.ui.analyzenew.treehelper.RootItem;
import de.catma.ui.analyzenew.treehelper.SingleItem;
import de.catma.ui.analyzenew.treehelper.TreeRowItem;

public class ResultPanelNew extends Panel {

	private static enum TreePropertyName {
		caption, frequency, visibleInKwic,;
	}


	public static interface ResultPanelCloseListener {
		public void closeRequest(ResultPanelNew resultPanelNew);
	}

	private VerticalLayout contentVerticalLayout;
	private TreeData<TreeRowItem> tagData;
	private TreeGrid<TreeRowItem> treeGridTag;
	
	private TreeData<TreeRowItem> phraseData;
	private TreeGrid<TreeRowItem> treeGridPhrase;
	
	private TreeData<TreeRowItem>phraseItemData;
	private TreeGrid<TreeRowItem> phraseItemTreeGrid;
	private TreeDataProvider<TreeRowItem>  phraseItemDataProvider;
	
	private TreeGrid<TreeRowItem> treeGridProperty;
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
	private ViewID currentView;
	private ResultPanelCloseListener resultPanelCloseListener;

	public ResultPanelNew(Repository repository, QueryResult result, String queryAsString,
			ResultPanelCloseListener resultPanelCloseListener) throws Exception {

		this.repository = repository;
		this.queryResult = result;
		this.queryAsString = queryAsString;
		this.resultPanelCloseListener = resultPanelCloseListener;

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
			// setDataPhraseStyleLazy();
			//setDataPhraseStyle();
			setDataPhraseItemStyle();
			setCurrentView(ViewID.phrase);
			// treeGridPanel.setContent(treeGridPhrase);
			treeGridPanel.setContent(phraseItemTreeGrid);
		}

	}

	public TreeData<TreeRowItem> getCurrentTreeGridData() {
		TreeGrid<TreeRowItem> currentTreeGrid = (TreeGrid<TreeRowItem>) treeGridPanel.getContent();
		TreeDataProvider<TreeRowItem> dataProvider = (TreeDataProvider<TreeRowItem>) currentTreeGrid.getDataProvider();
		return (TreeData<TreeRowItem>) dataProvider.getTreeData();
	}

	private void setCurrentView(ViewID currentView) {
		this.currentView = currentView;
	}

	public ViewID getCurrentView() {
		return this.currentView;
	}

	private void initComponents() {
		contentVerticalLayout = new VerticalLayout();

		setContent(contentVerticalLayout);

		treeGridTag = new TreeGrid<TreeRowItem>();
		treeGridTag.addStyleNames("annotate-resource-grid", "flat-undecorated-icon-buttonrenderer");

	//	treeGridPhrase = new TreeGrid<TreeRowItem>();
	//	treeGridPhrase.addStyleName( "flat-undecorated-icon-buttonrenderer");
		

		phraseItemTreeGrid = new TreeGrid<TreeRowItem>();
		phraseItemTreeGrid.addStyleNames("annotate-resource-grid", "flat-undecorated-icon-buttonrenderer");

		treeGridProperty = new TreeGrid<TreeRowItem>();
		treeGridProperty.addStyleName("annotate-resource-grid");

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
					e.printStackTrace();
				}
			}
		});

		trashBt.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				resultPanelCloseListener.closeRequest(ResultPanelNew.this);
			}
		});


		 
	}

	private void setDataTagStyle() throws Exception {

		tagData = new TreeData<>();
		tagData = populateTreeDataWithTags(repository, tagData,  queryResult);
		TreeDataProvider<TreeRowItem> dataProvider = new TreeDataProvider<>(tagData);

		treeGridTag.addColumn(TreeRowItem::getShortenTreeKey).setCaption("Tag").setId("tagID");
		treeGridTag.getColumn("tagID").setExpandRatio(5);
		
		treeGridTag.addColumn(TreeRowItem::getTreeKey).setCaption("Phrase").setId("tagPhraseID");
		treeGridTag.getColumn("tagPhraseID").setExpandRatio(7);

		treeGridTag.addColumn(TreeRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		treeGridTag.getColumn("freqID").setExpandRatio(1);

		dataProvider.refreshAll();
		treeGridTag.setDataProvider(dataProvider);
		treeGridTag.recalculateColumnWidths();
		treeGridTag.setWidth("100%");
		treeGridTag.setCaption(queryAsString);

		treeGridPanel.setContent(treeGridTag);
		
		//setDataPhraseStyle();
		// setDataPhraseStyleLazy();
	}

	/*private void setDataPhraseStyle() throws Exception {	
		
		Set<GroupedQueryResult> groupedQueryResults = queryResult.asGroupedSet();
		TreeData<TagRowItem> phraseData = new TreeData<>();
		//phraseData = new TreeData<>();
		ArrayList<TagRowItem> phraseAsRoots = new ArrayList<>();
		// add phrases as roots
		for (GroupedQueryResult groupedQueryResult : groupedQueryResults) {

			String phrase = (String) groupedQueryResult.getGroup();
			TagRowItem rootPhrase = new TagRowItem();
			rootPhrase.setTreePath(phrase);
			rootPhrase.setFrequency(groupedQueryResult.getTotalFrequency());
			phraseAsRoots.add(rootPhrase);
			phraseData.addItems(null, rootPhrase);
			// add documents and collections
			ArrayList<TagRowItem> documentsForAPhrase = retrieveDocumentsAsChildren(groupedQueryResult);
			phraseData.addItems(rootPhrase, documentsForAPhrase);
			
			for(TagRowItem doc : documentsForAPhrase) {
				ArrayList<TagRowItem> phraseItems = new ArrayList<>();
				phraseItems=retrievePhraseItemsAsChildren(groupedQueryResult,doc);			
				phraseData.addItems(doc,phraseItems);
			}
		}

		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(phraseData);
		treeGridPhrase.addColumn(TagRowItem::getShortenTreePath).setCaption("Phrase").setId("phraseID");
		treeGridPhrase.getColumn("phraseID").setExpandRatio(7);
		treeGridPhrase.addColumn(TagRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		treeGridPhrase.getColumn("freqID").setExpandRatio(1);
		dataProvider.refreshAll();	
		treeGridPhrase.setDataProvider(dataProvider);
		treeGridPhrase.setWidth("100%");
	}*/
	private QueryResultRowArray transformGroupedResultToArray(GroupedQueryResult groupedQueryResult) {
		QueryResultRowArray queryResultRowArray = new QueryResultRowArray();
		
		for (QueryResultRow queryResultRow : groupedQueryResult) {
			queryResultRowArray.add(queryResultRow);		
		}
		return queryResultRowArray;
		
	}
	
	
	private void setDataPhraseItemStyle() {
	
		phraseItemData = new TreeData<>();
		//QueryResultRowArray groupedQueryResultArray = queryResult.asQueryResultRowArray();
		Set<GroupedQueryResult> resultAsSet= queryResult.asGroupedSet();
		// add phrases as roots
		for (GroupedQueryResult onePhraseGroupedQueryResult : resultAsSet) {

			String phrase = (String) onePhraseGroupedQueryResult.getGroup();
			RootItem rootPhrase =   new RootItem();
		
			Set<String> allDocsForThatPhrase= onePhraseGroupedQueryResult.getSourceDocumentIDs();
			
			rootPhrase.setTreeKey(phrase); 
			
			QueryResultRowArray queryResultArray=transformGroupedResultToArray(onePhraseGroupedQueryResult);
			
			rootPhrase.setRows(queryResultArray);
			phraseItemData.addItem(null,  rootPhrase);
			ArrayList <TreeRowItem> allDocuments= new ArrayList<>();
			
			for(String docID: allDocsForThatPhrase) {
			GroupedQueryResult oneDocGroupedQueryResult=onePhraseGroupedQueryResult.getSubResult(docID);
			DocumentItem docItem = new DocumentItem();
			try {
				String docName = repository.getSourceDocument(docID).toString();
				docItem.setTreeKey(docName);
			} catch (Exception e) {
				
				e.printStackTrace();
			}
			docItem.setRows(transformGroupedResultToArray(oneDocGroupedQueryResult));
			allDocuments.add( docItem);		
			}
			phraseItemData.addItems( rootPhrase, allDocuments);
		}
	    phraseItemDataProvider = new TreeDataProvider<>(phraseItemData);
		phraseItemTreeGrid.setDataProvider(phraseItemDataProvider);
		treeGridPanel.setContent(phraseItemTreeGrid);
		phraseItemDataProvider.refreshAll();
		phraseItemTreeGrid.addColumn(TreeRowItem::getTreeKey).setCaption("Phrase").setId("phraseID");
		phraseItemTreeGrid.getColumn("phraseID").setExpandRatio(7);
		phraseItemTreeGrid.addColumn(TreeRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		phraseItemTreeGrid.getColumn("freqID").setExpandRatio(1);
		
	}
	


	private void setDataPropertyStyle() throws Exception {
		TreeData<TagRowItem> propData = new TreeData<>();

		propData = populateTreeDataWithProperties(repository, propData, queryResult); // TODO !!!!!!

		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(propData);

		treeGridProperty.addColumn(TagRowItem::getShortenTreePath).setCaption("Tag").setId("tagID");
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
		treeGridProperty.setCaption(queryAsString);

		treeGridPanel.setContent(treeGridProperty);

		//setDataPhraseStyle();
		// setDataPhraseStyleLazy();
	}
	
	private TreeData<TreeRowItem> populateTreeDataWithTagsOld2(Repository repository, TreeData<TreeRowItem> treeData,
			QueryResult queryResult) throws Exception {
		
		TreeData<TreeRowItem> currentData= treeData;
		
	
		
		QueryResultRowArray currentResult= (QueryResultRowArray)queryResult;
	  	Set<GroupedQueryResult> currentSet=	currentResult.asGroupedSet(); // das gibt ein nach phrase gruppiertes set !!! nix mit tagresult danach 
	  	
	  	
	  	for(GroupedQueryResult groupedQueryResult: currentSet) {
	  	Object group=	groupedQueryResult.getGroup();

	  
	  		Set<String> currentDocuments=	groupedQueryResult.getSourceDocumentIDs();
	  		for(String doc : currentDocuments) {
	  		GroupedQueryResult oneDocResult=	groupedQueryResult.getSubResult(doc);
	  	int freq=	oneDocResult.getTotalFrequency();
	  	Iterator<QueryResultRow> docIterator=	oneDocResult.iterator();
	  	
	  	while( docIterator.hasNext()) {
	  		TagQueryResultRow tagQueryResultRow = (TagQueryResultRow)docIterator.next();
	  		SingleItem singleTagItem = new SingleItem();
	  		singleTagItem.setTreeKey(tagQueryResultRow.getTagDefinitionPath());
	  	}
	  			
	  		}
	  		
	  	}

		return null;
		
	}
	
	private TreeData<TreeRowItem> populateTreeDataWithTags(Repository repository, TreeData<TreeRowItem> treeData,
			QueryResult queryResult) throws Exception {
		
		
	
		HashMap<String,QueryResultRowArray >allRoots =groupRootsGroupedByTagDefinitionPath(queryResult);
		
		Set<String > keys=allRoots.keySet();
		
		for(String key: keys) {
			RootItem root = new RootItem();
			root.setTreeKey(key);
			root.setRows(allRoots.get(key));	
			treeData.addItems(null, (TreeRowItem)root);
			
			HashMap<String , QueryResultRowArray> docsForARoot= new HashMap<String, QueryResultRowArray>();
			docsForARoot= groupDocumentsForRoot(root);
			Set<String > sourceDocs=root.getRows().getSourceDocumentIDs();
			
			
		
			
			
			

			for(String doc : sourceDocs) {
			QueryResultRowArray oneDocArray=	docsForARoot.get(doc);
				
				DocumentItem docItem = new DocumentItem();
				docItem.setTreeKey(doc);
				docItem.setRows(oneDocArray);
				treeData.addItem(root, docItem);
				
			}
			
			//treeData.addItems(root, root.getRows().getSourceDocumentIDs());
		}
		
		
		return treeData;
	}
	
	private HashMap<String, QueryResultRowArray> groupDocumentsForRoot(RootItem root) {
		HashMap<String, QueryResultRowArray> documentsForARoot = new HashMap<String, QueryResultRowArray>();
		QueryResultRowArray allDocsArray=root.getRows();
	
	Set<String > docs=	allDocsArray.getSourceDocumentIDs();
	for(QueryResultRow queryResultRow : allDocsArray) {
		if (queryResultRow instanceof TagQueryResultRow) {
			TagQueryResultRow tRow = (TagQueryResultRow) queryResultRow;
			
			QueryResultRowArray rows = documentsForARoot.get(tRow.getSourceDocumentId());
			
			if (rows == null) {
				rows = new QueryResultRowArray();
				documentsForARoot.put(tRow.getSourceDocumentId(), rows);
			}
			rows.add(tRow);
		}
		
	
			
		
			
		
	}
		return documentsForARoot;
		
	}
	
	private HashMap<String, QueryResultRowArray> groupRootsGroupedByTagDefinitionPath(QueryResult queryResults) throws Exception {
		int totalFreq = 0;
		HashMap<String, QueryResultRowArray> rowsGroupedByTagDefinitionPath = 
				new HashMap<String, QueryResultRowArray>();
		
		for (QueryResultRow row : queryResult) {
			
			if (row instanceof TagQueryResultRow) {
				TagQueryResultRow tRow = (TagQueryResultRow) row;
				QueryResultRowArray rows = 
						rowsGroupedByTagDefinitionPath.get(tRow.getTagDefinitionPath());
				
				if (rows == null) {
					rows = new QueryResultRowArray();
					rowsGroupedByTagDefinitionPath.put(tRow.getTagDefinitionPath(), rows);
				}
				rows.add(tRow);
			}	
	}
		return rowsGroupedByTagDefinitionPath;
	}


	

	private TreeData<TagRowItem> populateTreeDataWithProperties(Repository repository, TreeData<TagRowItem> treeData,
			QueryResult queryResult) throws Exception {

		ArrayList<TagRowItem> tagsAsRoot = new ArrayList<TagRowItem>();

		for (QueryResultRow queryResultRow : queryResult) {

			TagQueryResultRow tagQueryResultRow = (TagQueryResultRow) queryResultRow;
			TagRowItem tagRowItem = new TagRowItem();

			tagRowItem.setTagDefinitionPath(tagQueryResultRow.getTagDefinitionPath());

			if (!tagsAsRoot.stream()
					.anyMatch(var -> var.getTagDefinitionPath().equalsIgnoreCase(tagRowItem.getTagDefinitionPath()))) {

				tagRowItem.setTreePath(tagRowItem.getTagDefinitionPath());
				tagRowItem.setFrequencyOneUp();
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
						docItem.setFrequencyOneUp();
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
							tagRowItem.setFrequencyOneUp();
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

							TagRowItem propItem = new TagRowItem();
							propItem.setTreePath(tagQueryResultRow.getTagDefinitionPath());
							propItem.setPropertyName(tagQueryResultRow.getPropertyName());
							propItem.setPropertyValue(tagQueryResultRow.getPropertyValue());
							propItem.setQueryResultRow(tagQueryResultRow);

							treeData.addItem(oneCollection, propItem);
						}

					}

				}

			}

		}

		return treeData;

	}

	// get docs as children for a phrase- normal style
	private ArrayList<TagRowItem> retrieveDocumentsAsChildren(GroupedQueryResult groupedQueryResult) throws Exception {

		Set<String> docsForAPhrase = groupedQueryResult.getSourceDocumentIDs();
		ArrayList<TagRowItem> docItems = new ArrayList<>();
		for (String doc : docsForAPhrase) {
			TagRowItem oneDocItem = new TagRowItem();
			// get SourceDoc name from sorceDocID
			String docName = retrieveDocumentName(this.repository, doc);
			oneDocItem.setTreePath(docName);
			oneDocItem.setSourceDocName(docName);
			oneDocItem.setFrequency(groupedQueryResult.getFrequency(doc));
			docItems.add(oneDocItem);

		}
		return docItems;
	}
	
	private ArrayList<TagRowItem> retrievePhraseItemsAsChildren(GroupedQueryResult groupedQueryResult,TagRowItem document) throws Exception{
		ArrayList<TagRowItem> phraseItems = new ArrayList<>();
	for(QueryResultRow row:groupedQueryResult) {
		String docName = retrieveDocumentName(repository, row.getSourceDocumentId());
		if(document.getSourceDocName().equalsIgnoreCase(docName)) {
			TagRowItem phraseItem= new TagRowItem();
			phraseItem.setSourceDocName(docName);
			phraseItem.setPhrase(row.getPhrase());
			phraseItem.setTreePath(row.getPhrase());
			phraseItem.setQueryResultRow(row);
			phraseItems.add(phraseItem);
	
		}
	}
		
		return phraseItems;
	}



	// get docs as children for a phrase - lazy style
	private ArrayList<TagRowItem> getChilderenForSpecificPhrase(TagRowItem phraseItem,
			GroupedQueryResult onePhraseGroup) throws Exception {

		ArrayList<TagRowItem> docItems = new ArrayList<>();
		for (QueryResultRow resultRow : onePhraseGroup) {

			String docName = retrieveDocumentName(this.repository, resultRow.getSourceDocumentId());
			TagRowItem rowItem = new TagRowItem();
			rowItem.setTreePath(docName);

			if (!docItems.stream().anyMatch(var -> var.getTreePath().equalsIgnoreCase(rowItem.getTreePath()))) {
				docItems.add(rowItem);
			} else {
				docItems.stream().filter(var -> var.getTreePath().equalsIgnoreCase(rowItem.getTreePath())).findFirst()
						.get().setFrequencyOneUp();
			}
		}

		return docItems;
	}

	private String retrieveDocumentName(Repository repository, String docID) throws Exception {

		return repository.getSourceDocument(docID).toString();
	}

	public String getQueryAsString() {
		return this.queryAsString;
	}

	private void swichView() throws Exception {

		switch (currentView) {

		case tag:
			setCurrentView(ViewID.phraseTag);
			treeGridPanel.setContent(treeGridPhrase);
			break;

		case property:
			setCurrentView(ViewID.phraseProperty);
			treeGridPanel.setContent(treeGridPhrase);
			break;

		case phrase:
			Notification.show("no tag view available for that query", Notification.Type.HUMANIZED_MESSAGE);
			break;

		case phraseProperty:
			setCurrentView(ViewID.property);
			treeGridPanel.setContent(treeGridProperty);
			break;

		case phraseTag:
			setCurrentView(ViewID.tag);
			treeGridPanel.setContent(treeGridTag);
			break;

		default:
			Notification.show("no view available ", Notification.Type.HUMANIZED_MESSAGE);
			break;

		}
	}

}
