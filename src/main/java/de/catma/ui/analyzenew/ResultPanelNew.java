package de.catma.ui.analyzenew;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.RandomStringUtils;

import com.google.common.cache.LoadingCache;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
//import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
//import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.themes.ValoTheme;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.analyzenew.treegridhelper.CollectionItem;
import de.catma.ui.analyzenew.treegridhelper.DocumentItem;
import de.catma.ui.analyzenew.treegridhelper.RootItem;
import de.catma.ui.analyzenew.treegridhelper.SingleItem;
import de.catma.ui.analyzenew.treegridhelper.TreeRowItem;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;

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

	private TreeData<TreeRowItem> propData;
	private TreeGrid<TreeRowItem> treeGridProperty;
	
	private TreeData<TreeRowItem> propDataFlat;
	private TreeGrid <TreeRowItem> treeGridPropertyFlatTable;
	
	private final ContextMenu optionsMenu;

	private Label queryInfo;
	private LoadingCache<String , KwicProvider> kwicProviderCache;
	private HorizontalLayout groupedIcons;
	private Button caretRightBt;
	private Button caretDownBt;
	private Button removeBt;
	private Button optionsBt;
	private Panel treeGridPanel;
	private QueryResult queryResult;
	private String queryAsString;
	private String creationTime;
	private Repository repository;
	private ViewID currentView;
	private ResultPanelCloseListener resultPanelCloseListener;
	private boolean phraseBased= false;
	private boolean tagBased= false;
	private boolean	propertyBased=false;

	public ResultPanelNew(Repository repository, QueryResult result, String queryAsString, 
			LoadingCache<String, KwicProvider> kwicProviderCache,
			ResultPanelCloseListener resultPanelCloseListener) throws Exception {

		this.repository = repository;
		this.queryResult = result;
		this.queryAsString = queryAsString;
		this.kwicProviderCache= kwicProviderCache;
		this.resultPanelCloseListener = resultPanelCloseListener;
		initComponents();
		initListeners();
		optionsMenu = new ContextMenu(optionsBt,true);
		
		detectViewFromQueryResult(result);

		if (currentView == ViewID.tag) {
			setDataTagStyle();
			//setCurrentView(ViewID.tag);
			treeGridPanel.setContent(treeGridTag);
			optionsMenu.addItem("Switch to Phrase View", clickEvent -> {
				try {
					switchToPhraseView();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
			optionsMenu.addItem("Switch to Tag View", e -> {
				try {
					switchToTagView();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
			
		}
		if (currentView == ViewID.phrase) {
			setDataPhraseStyle();
			//setCurrentView(ViewID.phrase);
			treeGridPanel.setContent(treeGridPhrase);
			optionsMenu.addItem("No other View for that query available");

		}

		if (currentView == ViewID.property) {
			setDataPropertyStyle();
			treeGridPanel.setContent(treeGridProperty);
			
			optionsMenu.addItem("Switch to Phrase View", clickEvent -> {
				try {
					switchToPhraseView();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
			optionsMenu.addItem("Switch to Tag/Property View", e -> {
				try {
					//switchToTagView();
					setCurrentView(ViewID.property);
					treeGridPanel.setContent(treeGridProperty);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
			optionsMenu.addItem("Switch to Flat Table View", e -> {
				try {
					setCurrentView(ViewID.flatTableProperty);
					treeGridPanel.setContent(treeGridPropertyFlatTable);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
		}
		
		if (currentView == ViewID.mixedTagPhrase) {
			setDataTagStyle();
			currentView = ViewID.tag;
			treeGridPanel.setContent(treeGridTag);
			
			optionsMenu.addItem("Switch to Phrase View", clickEvent -> {
				try {
					switchToPhraseView();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
			optionsMenu.addItem("Switch to Tag View", e -> {
				try {
					switchToTagView();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});	
		}
		
		if (currentView == ViewID.mixedPropertyPhrase) {
			setDataPropertyStyle();
			currentView = ViewID.property;
			treeGridPanel.setContent(treeGridProperty);
			
			optionsMenu.addItem("Switch to Phrase View", clickEvent -> {
				try {
					switchToPhraseView();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
			optionsMenu.addItem("Switch to Flat Table View", e -> {
				try {
					switchToFlatTableView();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
			optionsMenu.addItem("Switch to Tag/Property View", e -> {
				try {	
					switchToPropertyView();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
			
		}
		
	}



	private void initComponents() {
		this.setWidth(80, Unit.PERCENTAGE);
		contentVerticalLayout = new VerticalLayout();
		contentVerticalLayout.addStyleName("analyze_queryresultpanel__card");

		addStyleName("analyze_queryresultpanel__card_frame");
		setContent(contentVerticalLayout);

		treeGridTag = new TreeGrid<TreeRowItem>();
		treeGridTag.addStyleNames("annotation-details-panel-annotation-details-grid",
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");

		treeGridPhrase = new TreeGrid<TreeRowItem>();
		treeGridPhrase.addStyleNames("annotation-details-panel-annotation-details-grid",
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");

		treeGridProperty = new TreeGrid<TreeRowItem>();
		treeGridProperty.addStyleNames("annotation-details-panel-annotation-details-grid",
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");
		
		treeGridPropertyFlatTable= new TreeGrid<TreeRowItem>();
		treeGridPropertyFlatTable.addStyleNames("annotation-details-panel-annotation-details-grid",
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");

		createResultInfoBar();
		createButtonBar();
		treeGridPanel = new Panel();
	}

	private void createResultInfoBar() {
		QueryResultRowArray resultRowArrayArrayList = queryResult.asQueryResultRowArray();
		int resultSize = resultRowArrayArrayList.size();
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		creationTime = timestamp.toString().substring(0, 19);
		queryInfo = new Label(queryAsString + "(" + resultSize + ")"+" created: "+creationTime);
		queryInfo.addStyleName("analyze_queryresultpanel_infobar");
		contentVerticalLayout.addComponent(queryInfo);
	}

	private void createButtonBar() {
		groupedIcons = new HorizontalLayout();

		caretRightBt = new Button(VaadinIcons.CARET_RIGHT);
		caretRightBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);

		caretDownBt = new Button(VaadinIcons.CARET_DOWN);
		caretDownBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);

		optionsBt = new Button(VaadinIcons.ELLIPSIS_V);
		optionsBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);

		removeBt = new Button(VaadinIcons.ERASER);
		removeBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);

		groupedIcons.addComponents(removeBt, optionsBt, caretRightBt);
		groupedIcons.addStyleName("analyze_queryresultpanel_buttonbar");
		contentVerticalLayout.addComponent(groupedIcons);
	}

	private void initListeners() {
		caretRightBt.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				contentVerticalLayout.addComponent(treeGridPanel);
				groupedIcons.replaceComponent(caretRightBt, caretDownBt);

			}
		});

		caretDownBt.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				contentVerticalLayout.removeComponent(treeGridPanel);
				groupedIcons.replaceComponent(caretDownBt, caretRightBt);
			}
		});
		
		optionsBt.addClickListener((evt) ->  optionsMenu.open(evt.getClientX(), evt.getClientY()));

		removeBt.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				resultPanelCloseListener.closeRequest(ResultPanelNew.this);
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public TreeData<TreeRowItem> getCurrentTreeGridData() {

		TreeGrid<TreeRowItem> currentTreeGrid = (TreeGrid<TreeRowItem>) treeGridPanel.getContent();
		TreeDataProvider<TreeRowItem> dataProvider = (TreeDataProvider<TreeRowItem>) currentTreeGrid.getDataProvider();
		TreeData<TreeRowItem> treeData = (TreeData<TreeRowItem>) dataProvider.getTreeData();
		return copyTreeData(treeData);
	}

	private TreeData<TreeRowItem> copyTreeData(TreeData<TreeRowItem> treeData) {
		if(this.currentView==ViewID.flatTableProperty) { // need no dummies
			return treeData;
			
		}else {
			TreeData<TreeRowItem> toReturn = new TreeData<TreeRowItem>();
			List<TreeRowItem> roots = treeData.getRootItems();
			for (TreeRowItem root : roots) {
				toReturn.addItem(null, root);
				List<TreeRowItem> childrenOne = treeData.getChildren(root);
				List<TreeRowItem> copyOfChildrenOne = new ArrayList<>(childrenOne);
				toReturn.addItems(root, copyOfChildrenOne);
				// add dummy on doclevel for phrase query
				if (treeData.getChildren(childrenOne.get(0)).isEmpty()) {

					for (TreeRowItem childOne : copyOfChildrenOne) {
						SingleItem dummy = new SingleItem();
						dummy.setTreeKey(RandomStringUtils.randomAlphanumeric(7));
						toReturn.addItem(childOne, dummy);
					}

				} else {
					for (TreeRowItem childOne : copyOfChildrenOne) {
						List<TreeRowItem> childrenTwo = treeData.getChildren(childOne);
						List<TreeRowItem> copyOfChildrenTwo = new ArrayList<>(childrenTwo);
						toReturn.addItems(childOne, copyOfChildrenTwo);
						for (TreeRowItem childTwo : copyOfChildrenTwo) {
							SingleItem dummy = new SingleItem();
							dummy.setTreeKey(RandomStringUtils.randomAlphanumeric(7));
							toReturn.addItem(childTwo, dummy);

						}

					}
				}
			}
			return toReturn;			
		}

	}

	private void setCurrentView(ViewID currentView) {
		this.currentView = currentView;
	}

	public ViewID getCurrentView() {
		return this.currentView;
	}
	
	private void detectViewFromQueryResult(QueryResult queryResult) {
		ViewID Viewid = null;
		for (QueryResultRow queryResultRow : queryResult) {
			
			if (queryResultRow instanceof TagQueryResultRow) {

				tagBased = true;
				Viewid = ViewID.tag;
				TagQueryResultRow tagQRR = (TagQueryResultRow) queryResultRow;

				if (tagQRR.getPropertyDefinitionId() != null) {
					propertyBased=true;
					tagBased=false;
					Viewid = ViewID.property;
				}
			}
			if (!(queryResultRow instanceof TagQueryResultRow)) {
				phraseBased = true;
				Viewid = ViewID.phrase;
			}
		}
		if (tagBased && phraseBased) {
			Viewid = ViewID.mixedTagPhrase;
		}		
		if (propertyBased && phraseBased) {
			Viewid = ViewID.mixedPropertyPhrase;
		}
		currentView = Viewid;
	}

	private void setDataTagStyle() throws Exception {
		tagData = new TreeData<>();
		tagData = populateTreeDataWithTags(tagData, queryResult);
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
		
		/*
		  Set<SourceDocument> toBeUnloaded = new HashSet<SourceDocument>();
		  LoadingCache<String, SourceDocument> documentCache =
		  CacheBuilder.newBuilder() .maximumSize(10) .removalListener(new
		  RemovalListener<String, SourceDocument>() {
		  
		  @Override public void onRemoval(RemovalNotification<String, SourceDocument>
		  notification) { if (toBeUnloaded.contains(notification.getValue())) {
		  notification.getValue().unload(); } } }) .build(new CacheLoader<String,
		  SourceDocument>() {
		  
		  @Override public SourceDocument load(String key) throws Exception { return
		  repository.getSourceDocument(key); } });
		 */
    	
    	
		Set<GroupedQueryResult> resultAsSet = queryResult.asGroupedSet();

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
				KwicProvider kwicProvider= null;
				try {
				kwicProvider=	kwicProviderCache.get(docID);
				docItem.setTreeKey(kwicProvider.getSourceDocumentName());
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				/*
				 * try { SourceDocument sd = documentCache.get(docID); if (!sd.isLoaded()) {
				 * toBeUnloaded.add(sd); } String docName = sd.toString(); //
				 * docItem.setTreeKey(docName); } catch (Exception e) { e.printStackTrace(); }
				 */
				docItem.setRows(transformGroupedResultToArray(oneDocGroupedQueryResult));
				allDocuments.add(docItem);
			}
			phraseData.addItems(rootPhrase, allDocuments);
		}
		
		/*
		 * for (SourceDocument sd : toBeUnloaded) { sd.unload(); }
		 */

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
		propData = new TreeData<>();
		propData = populateTreeDataWithProperties(propData, queryResult); 

		TreeDataProvider<TreeRowItem> propertyDataProvider = new TreeDataProvider<>(propData);

		treeGridProperty.addColumn(TreeRowItem::getShortenTreeKey).setCaption("Tag").setId("tagID");
		treeGridProperty.getColumn("tagID").setExpandRatio(3);

		treeGridProperty.addColumn(TreeRowItem::getPropertyName).setCaption("Property name").setId("propNameID");
		treeGridProperty.getColumn("propNameID").setExpandRatio(3);

		treeGridProperty.addColumn(TreeRowItem::getPropertyValue).setCaption("Property value").setId("propValueID");
		treeGridProperty.getColumn("propValueID").setExpandRatio(3);

		treeGridProperty.addColumn(TreeRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		treeGridProperty.getColumn("freqID").setExpandRatio(1);

		propertyDataProvider.refreshAll();
		treeGridProperty.setDataProvider(propertyDataProvider);
		treeGridProperty.setWidth("100%");
		treeGridProperty.setCaption(queryAsString);

		treeGridPanel.setContent(treeGridProperty);
		setDataFlatTableStyle();
		setDataPhraseStyle();
	}
	
	private void setDataFlatTableStyle() {
		propDataFlat = new TreeData<TreeRowItem>();
		TreeDataProvider<TreeRowItem> propFlatDataProvider = new TreeDataProvider<>(propDataFlat);

		treeGridPropertyFlatTable.setWidth("100%");
		treeGridPropertyFlatTable.setHeight("100%");
		treeGridPropertyFlatTable.addColumn(TreeRowItem::getShortenTreeKey).setCaption("Tag").setId("tagID");
		treeGridPropertyFlatTable.getColumn("tagID").setExpandRatio(3);
		
		treeGridPropertyFlatTable.addColumn(TreeRowItem::getPhrase).setCaption("Phrase").setId("phraseID");
		treeGridPropertyFlatTable.getColumn("phraseID").setExpandRatio(3);
		

		treeGridPropertyFlatTable.addColumn(TreeRowItem::getPropertyName).setCaption("Name").setId("nameID");
		treeGridPropertyFlatTable.getColumn("nameID").setExpandRatio(3);

		treeGridPropertyFlatTable.addColumn(TreeRowItem::getPropertyValue).setCaption("Value").setId("valueID");
		treeGridPropertyFlatTable.getColumn("valueID").setExpandRatio(3);

		ArrayList<TreeRowItem> flatTableList = new ArrayList<>();
		QueryResultRowArray qrra = queryResult.asQueryResultRowArray();

		for (QueryResultRow queryResultRow : qrra) {
			if(queryResultRow instanceof TagQueryResultRow) {
				TagQueryResultRow tagQueryResultRow = (TagQueryResultRow) queryResultRow;
				SingleItem propItem = new SingleItem();
				propItem.setTreeKey(tagQueryResultRow.getTagDefinitionPath());
				propItem.setPropertyName(tagQueryResultRow.getPropertyName());
				propItem.setPropertyValue(tagQueryResultRow.getPropertyValue());
				propItem.setPhrase(tagQueryResultRow.getPhrase());
		
				QueryResultRowArray queryResultRowArray = new QueryResultRowArray();
				queryResultRowArray.add(queryResultRow);
				propItem.setRows(queryResultRowArray);
				propItem.setQueryResultRowArray(queryResultRowArray);
				flatTableList.add((TreeRowItem) propItem);
				if (!propDataFlat.contains(propItem))
					propDataFlat.addItem(null, propItem);				
			}else {			
			}
		}	
		propFlatDataProvider.refreshAll();
		treeGridPropertyFlatTable.setDataProvider(propFlatDataProvider);
	}

	private TreeData<TreeRowItem> populateTreeDataWithTags(TreeData<TreeRowItem> treeData,
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

			for (String docID : sourceDocs) {

				QueryResultRowArray oneDocArray = docsForARoot.get(docID);

				DocumentItem docItem = new DocumentItem();
						
				KwicProvider kwicProvider=kwicProviderCache.get(docID);
				String sourceDocName=kwicProvider.getSourceDocumentName()
;				docItem.setTreeKey(sourceDocName);
				docItem.setRows(oneDocArray);
				treeData.addItem(root, docItem);
				// adding collections

				QueryResultRowArray itemsForADoc = docItem.getRows();
				HashMap<String, QueryResultRowArray> collectionsForADoc = new HashMap<String, QueryResultRowArray>();

				for (QueryResultRow queryResultRow : itemsForADoc) {

					TagQueryResultRow tRow = (TagQueryResultRow) queryResultRow;

					QueryResultRowArray queryResultRowArray = new QueryResultRowArray();

					String collID = tRow.getMarkupCollectionId();
					SourceDocument sourceDoc=kwicProvider.getSourceDocument();
					
					
					String collName = sourceDoc.getUserMarkupCollectionReference(collID).getName();
				

					if (collectionsForADoc.containsKey(collName)) {
						queryResultRowArray = collectionsForADoc.get(collName);
						queryResultRowArray.add(queryResultRow);
					} else {
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

				}

			}

		}

		return treeData;
	}

	private HashMap<String, QueryResultRowArray> groupDocumentsForRoot(RootItem root) {
		HashMap<String, QueryResultRowArray> documentsForARoot = new HashMap<String, QueryResultRowArray>();
		QueryResultRowArray allDocsArray = root.getRows();

		for (QueryResultRow queryResultRow : allDocsArray) {
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

	private TreeData<TreeRowItem> populateTreeDataWithProperties(TreeData<TreeRowItem> treeData,
			QueryResult queryResult) throws Exception {
		
		TreeData<TreeRowItem> data = populateTreeDataWithTags(treeData, queryResult);
		return data;
	}
		
	public String getQueryAsString() {
		return this.queryAsString+ "("+ creationTime+")";
	}
	
	private void switchToPhraseView() {
		setCurrentView(ViewID.phrase);
		treeGridPanel.setContent(treeGridPhrase);	
	}
	
	private void switchToTagView() {
		setCurrentView(ViewID.tag);
		treeGridPanel.setContent(treeGridTag);	
	}
	
	private void switchToFlatTableView() {
		setCurrentView(ViewID.flatTableProperty);
		treeGridPanel.setContent(treeGridPropertyFlatTable);		
	}
	
	private void switchToPropertyView() {
		setCurrentView(ViewID.property);
		treeGridPanel.setContent(treeGridProperty);		
	}

}
