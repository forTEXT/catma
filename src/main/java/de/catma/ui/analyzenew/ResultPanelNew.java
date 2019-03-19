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
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResult;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.analyzenew.treehelper.CollectionItem;
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
	
	private TreeData<TreeRowItem>phraseData;
	private TreeGrid<TreeRowItem> treeGridPhrase;
	
	private TreeData<TreeRowItem>propertyData;
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
			setDataPhraseStyle();
			setCurrentView(ViewID.phrase);
			// treeGridPanel.setContent(treeGridPhrase);
			treeGridPanel.setContent(treeGridPhrase);
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
		treeGridTag.addStyleNames(
				"annotation-details-panel-annotation-details-grid", 
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");

	//	treeGridPhrase = new TreeGrid<TreeRowItem>();
	//	treeGridPhrase.addStyleName( "flat-undecorated-icon-buttonrenderer");
		

		treeGridPhrase = new TreeGrid<TreeRowItem>();
		treeGridPhrase.addStyleNames(
				"annotation-details-panel-annotation-details-grid", 
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");

		treeGridProperty = new TreeGrid<TreeRowItem>();
		treeGridProperty.addStyleNames(
				"annotation-details-panel-annotation-details-grid", 
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");

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
		
		treeGridTag.addColumn(TreeRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		treeGridTag.getColumn("freqID").setExpandRatio(1);

		dataProvider.refreshAll();
		treeGridTag.setDataProvider(dataProvider);
		treeGridTag.recalculateColumnWidths();
		treeGridTag.setWidth("100%");
		treeGridTag.setCaption(queryAsString);

		treeGridPanel.setContent(treeGridTag);
		
		setDataPhraseStyle();
		
	}



	
	
	private void setDataPhraseStyle() {
	
		phraseData = new TreeData<>();
	
		Set<GroupedQueryResult> resultAsSet= queryResult.asGroupedSet();
	
		for (GroupedQueryResult onePhraseGroupedQueryResult : resultAsSet) {

			String phrase = (String) onePhraseGroupedQueryResult.getGroup();
			RootItem rootPhrase = new RootItem();

			Set<String> allDocsForThatPhrase = onePhraseGroupedQueryResult.getSourceDocumentIDs();

			rootPhrase.setTreeKey(phrase);

			QueryResultRowArray queryResultArray = transformGroupedResultToArray(onePhraseGroupedQueryResult);

			rootPhrase.setRows(queryResultArray);
			phraseData.addItem(null, rootPhrase);
			ArrayList<TreeRowItem> allDocuments = new ArrayList<>();

			for (String docID : allDocsForThatPhrase) {
				GroupedQueryResult oneDocGroupedQueryResult = onePhraseGroupedQueryResult.getSubResult(docID);
				DocumentItem docItem = new DocumentItem();

				try {
					String docName = repository.getSourceDocument(docID).toString();
					docItem.setTreeKey(docName);
				} catch (Exception e) {

					e.printStackTrace();
				}

				docItem.setRows(transformGroupedResultToArray(oneDocGroupedQueryResult));
				allDocuments.add(docItem);
			}

			phraseData.addItems(rootPhrase, allDocuments);
			for (TreeRowItem doc : allDocuments) {
				SingleItem fakeChild = new SingleItem();
				phraseData.addItems(doc, fakeChild);
				
			}
			
		}
		
		TreeDataProvider<TreeRowItem> phraseDataProvider = new TreeDataProvider<>(phraseData);
		treeGridPhrase.setDataProvider(phraseDataProvider);
		treeGridPanel.setContent(treeGridPhrase);
		phraseDataProvider.refreshAll();
		treeGridPhrase.addColumn(TreeRowItem::getTreeKey).setCaption("Phrase").setId("phraseID");
		treeGridPhrase.getColumn("phraseID").setExpandRatio(7);
		treeGridPhrase.addColumn(TreeRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		treeGridPhrase.getColumn("freqID").setExpandRatio(1);
		treeGridPhrase.setWidth("100%");
		
	}
	


	private void setDataPropertyStyle() throws Exception {
		
		TreeData<TreeRowItem> propData = new TreeData<>();
		propData = populateTreeDataWithProperties(repository, propData, queryResult); // TODO !!!!!!

		TreeDataProvider<TreeRowItem> dataProvider = new TreeDataProvider<>(propData);

		treeGridProperty.addColumn(TreeRowItem::getShortenTreeKey).setCaption("Tag").setId("tagID");
		treeGridProperty.getColumn("tagID").setExpandRatio(3);

		treeGridProperty.addColumn(TreeRowItem::getPropertyName).setCaption("Property name").setId("propNameID");
		treeGridProperty.getColumn("propNameID").setExpandRatio(3);

		treeGridProperty.addColumn(TreeRowItem::getPropertyValue).setCaption("Property value").setId("propValueID");
		treeGridProperty.getColumn("propValueID").setExpandRatio(3);

		treeGridProperty.addColumn(TreeRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		treeGridProperty.getColumn("freqID").setExpandRatio(1);

		dataProvider.refreshAll();
		treeGridProperty.setDataProvider(dataProvider);
		treeGridProperty.setWidth("100%");
		treeGridProperty.setCaption(queryAsString);

		treeGridPanel.setContent(treeGridProperty);

		setDataPhraseStyle();
		
	}
	

	
	private TreeData<TreeRowItem> populateTreeDataWithTags(Repository repository, TreeData<TreeRowItem> treeData,
			QueryResult queryResult) throws Exception {

		HashMap<String, QueryResultRowArray> allRoots = groupRootsGroupedByTagDefinitionPath(queryResult);

		Set<String> keys = allRoots.keySet();

		for (String key : keys) {
			RootItem root = new RootItem();
			root.setTreeKey(key);
			root.setRows(allRoots.get(key));
			treeData.addItems(null, (TreeRowItem) root);

			HashMap<String, QueryResultRowArray> docsForARoot = new HashMap<String, QueryResultRowArray>();
			docsForARoot = groupDocumentsForRoot(root);
			Set<String> sourceDocs = root.getRows().getSourceDocumentIDs();

			for (String doc : sourceDocs) {

				QueryResultRowArray oneDocArray = docsForARoot.get(doc);

				DocumentItem docItem = new DocumentItem();
				SourceDocument sourceDoc = repository.getSourceDocument(doc);
				docItem.setTreeKey(sourceDoc.toString());
				docItem.setRows(oneDocArray);
				treeData.addItem(root, docItem);
				// adding collections

				QueryResultRowArray itemsForADoc = docItem.getRows();
				HashMap<String, QueryResultRowArray> collectionsForADoc = new HashMap<String, QueryResultRowArray>();

				for (QueryResultRow queryResultRow : itemsForADoc) {

					TagQueryResultRow tRow = (TagQueryResultRow) queryResultRow;
					
					QueryResultRowArray queryResultRowArray = new QueryResultRowArray();

					String collID = tRow.getMarkupCollectionId();
					String collName=sourceDoc.getUserMarkupCollectionReference(collID).getName();

					if (collectionsForADoc.containsKey(collName)) {
					 queryResultRowArray = collectionsForADoc.get(collName);
						queryResultRowArray.add(queryResultRow);
					}else {
						queryResultRowArray.add(queryResultRow);
						collectionsForADoc.put(collName, queryResultRowArray);
						
					}
				}
				
				Set<String> collections = collectionsForADoc.keySet();
				for (String coll : collections) {
					CollectionItem collItem = new CollectionItem();
					collItem.setTreeKey(coll);
					collItem.setRows(collectionsForADoc.get(coll));
					treeData.addItem(docItem, collItem);
					
					TreeRowItem dummyItem = new SingleItem();
					treeData.addItem(collItem, dummyItem);
				}

			}

		}

		return treeData;
	}	
	
	
	private HashMap<String, QueryResultRowArray> groupDocumentsForRoot(RootItem root) {
		HashMap<String, QueryResultRowArray> documentsForARoot = new HashMap<String, QueryResultRowArray>();
		QueryResultRowArray allDocsArray=root.getRows();
	
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
	
	private HashMap<String, QueryResultRowArray> groupRootsGroupedByTagDefinitionPath(QueryResult queryResults)
			throws Exception {
	
		HashMap<String, QueryResultRowArray> rowsGroupedByTagDefinitionPath = new HashMap<String, QueryResultRowArray>();

		for (QueryResultRow row : queryResult) {

			if (row instanceof TagQueryResultRow) {
				TagQueryResultRow tRow = (TagQueryResultRow) row;
				QueryResultRowArray rows = rowsGroupedByTagDefinitionPath.get(tRow.getTagDefinitionPath());

				if (rows == null) {
					rows = new QueryResultRowArray();
					rowsGroupedByTagDefinitionPath.put(tRow.getTagDefinitionPath(), rows);
				}
				rows.add(tRow);
			}
		}
		return rowsGroupedByTagDefinitionPath;
	}

	
	private QueryResultRowArray transformGroupedResultToArray(GroupedQueryResult groupedQueryResult) {
		QueryResultRowArray queryResultRowArray = new QueryResultRowArray();
		
		for (QueryResultRow queryResultRow : groupedQueryResult) {
			queryResultRowArray.add(queryResultRow);		
		}
		return queryResultRowArray;
	
	}
	
	
	private TreeData<TreeRowItem> populateTreeDataWithProperties(Repository repository, TreeData<TreeRowItem> treeData,
			QueryResult queryResult) throws Exception {
	return	populateTreeDataWithTags(repository, treeData, queryResult);
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
