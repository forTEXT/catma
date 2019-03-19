package de.catma.ui.analyzenew;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.hamcrest.core.IsInstanceOf;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.DataChangeEvent;
import com.vaadin.data.provider.DataProviderListener;
import com.vaadin.data.provider.GridSortOrder;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.ContextClickEvent;
import com.vaadin.event.ExpandEvent;
import com.vaadin.event.ExpandEvent.ExpandListener;
import com.vaadin.event.SortEvent;
import com.vaadin.event.SortEvent.SortListener;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.event.selection.SingleSelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ClientConnector.AttachEvent;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;

import de.catma.document.repository.Repository;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResult;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.analyzenew.treehelper.CollectionItem;
import de.catma.ui.analyzenew.treehelper.DocumentItem;
import de.catma.ui.analyzenew.treehelper.QueryRootItem;
import de.catma.ui.analyzenew.treehelper.RootItem;
import de.catma.ui.analyzenew.treehelper.SingleItem;
import de.catma.ui.analyzenew.treehelper.TreeRowItem;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.tagger.annotationpanel.AnnotationTreeItem;
import de.catma.ui.tagger.annotationpanel.TagsetTreeItem;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

public class KwicVizPanelNew extends HorizontalLayout implements VizPanel {

	private VerticalLayout leftSide;
	private Repository repository;
	private Panel header;
	private Button arrowLeft;
	private CloseVizViewListener leaveViewListener;
	private Iterator<Component> allResultsIterator;
	private HorizontalLayout mainLayout;
	private Panel left;
	private Panel right;
	private Button test;
	private ComboBox<String> comboBox;
	private List<String> availableResultSets;
	private VerticalLayout vertical;
	private Panel queryResultsPanel;
	private HorizontalLayout mainContentPanel;
	private Panel rightSide;
	private ArrayList<CurrentTreeGridData> currentTreeGridDatas;
	private TreeData<TreeRowItem> resultsTreeGridData;
	private TreeGrid<TreeRowItem> resultsTreeGrid;
	//private Grid<TagRowItem>selectedItemsGrid;
	private TreeGrid<TreeRowItem> phraseTreeGrid;
	
	private TreeDataProvider<TreeRowItem> dataProvider;
	private TreeData<TreeRowItem> selectedItemsTreeGridData;
	private TreeGrid<TreeRowItem>selectedItemsTreeGrid;
	private TreeDataProvider<TreeRowItem> selectedDataProvider;
	private KwicPanelNew kwicNew;
	private Button arrowLeftBt;
	private ViewID selectedGridView;
	private Panel selectedItemsPanel;

	public KwicVizPanelNew(CloseVizViewListener leaveVizListener, ArrayList<CurrentTreeGridData> currentTreeGridDatas,Repository repository) {
		this.currentTreeGridDatas = currentTreeGridDatas;
		this.leaveViewListener = leaveVizListener;
		this.repository= repository;
		initComponents();
		initActions();
		initListeners();
	}

	private void initComponents() {
		leftSide = new VerticalLayout();
		rightSide = new Panel("KWIC Visualisation");
		rightSide.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		kwicNew=new KwicPanelNew(repository);
		kwicNew.setHeight("100%");
		kwicNew.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		rightSide.setContent(kwicNew);
		rightSide.setHeight("100%");
		header = new Panel();
		
		arrowLeftBt = new Button(VaadinIcons.ARROW_LEFT);
		arrowLeftBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		
		//arrowLeft = new Button("<");
		header.setContent(arrowLeftBt);
		header.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		leftSide.addComponent(header);
		mainContentPanel = new HorizontalLayout();
	
		addComponent(mainContentPanel);
		mainContentPanel.setSizeFull();
		setSpacing(false);
		
		//mainContentPanel.setHeight("100%");
		//mainContentPanel.setWidth("100%");
		comboBox = new ComboBox<String>();
		comboBox.setWidth("100%");
		comboBox.setCaption("select one resultset");
		availableResultSets = new ArrayList<>();
		availableResultSets = getQueriesForAvailableResults();
		comboBox.setItems(availableResultSets);
		comboBox.addSelectionListener(new SingleSelectionListener<String>() {
			@Override
			public void selectionChange(SingleSelectionEvent<String> event) {
				String queryAsString = event.getSource().getValue();
				swichToResultTree(queryAsString);
			}
		});
		queryResultsPanel = new Panel();
		queryResultsPanel.setHeight("230px");
		resultsTreeGrid= new TreeGrid<>();
		
		
		selectedItemsTreeGrid = new TreeGrid<TreeRowItem>();
	

		//selectedItemsTreeGrid.addColumn(TreeRowItem::getPropertyName).setCaption("property");
	
		selectedItemsTreeGridData= new 	TreeData<TreeRowItem>();
		selectedDataProvider = new TreeDataProvider<>(selectedItemsTreeGridData);
		selectedItemsTreeGrid.setDataProvider(selectedDataProvider);
		selectedItemsTreeGrid.addColumn(TreeRowItem::getTreeKey).setCaption("phrase").setId("treeKeyID");
		 selectedItemsTreeGrid.getColumn("treeKeyID").setExpandRatio(8);
		
	    ButtonRenderer<TreeRowItem> removeItemsRenderer = new ButtonRenderer<TreeRowItem>( removeClickEvent-> handleRemoveClickEvent(removeClickEvent));
	    removeItemsRenderer.setHtmlContentAllowed(true);		
	    selectedItemsTreeGrid.addColumn(TreeRowItem::getRemoveIcon, removeItemsRenderer).setCaption("remove").setId("removeID");
	    selectedItemsTreeGrid.getColumn("removeID").setExpandRatio(2);
		
		
		selectedItemsTreeGrid.setWidth("100%");
	
		selectedItemsPanel= new Panel();
		selectedItemsPanel.setCaption("selected items for the kwic visualization");

		selectedItemsPanel.setWidth("100%");
		selectedItemsPanel.setHeight("250px");
		
		selectedItemsPanel.setContent(selectedItemsTreeGrid);
		leftSide.addComponent(comboBox);
		leftSide.addComponent(queryResultsPanel);
		//leftSide.addComponent(selectedItemsPanel);
		//leftSide.addComponent(selectedItemsTreeGrid);
		setActionGridComponenet();
		
		mainContentPanel.addComponents(leftSide, rightSide);
		float left= 0.4f;
		float right =0.6f;
		mainContentPanel.setExpandRatio(leftSide, left);
		mainContentPanel.setExpandRatio(rightSide, right);
		//mainContentSplitPanel.setExpandRatio(leftSide, 0.4f);
				
	}

	private void initActions() {
		arrowLeftBt.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				leaveViewListener.onClose();
				VizSnapshot kwicSnapshot = new VizSnapshot("KWIC Snapshot");

			}
		});

	}

	private void initListeners() {
		selectedDataProvider.addDataProviderListener(new DataProviderListener<TreeRowItem>() {	
			@Override
			public void onDataChange(DataChangeEvent<TreeRowItem> event) {
				if(comboBox.getValue().contains("wild")) {		
					updateKwicView();	
				}
			}
		});
			
	}
	
	private void updateKwicView() {
		// QueryResult queryResult = createQueryResultFromTreeGridDataTags();
		ArrayList<QueryResultRow> queryResult = createQueryResultFromTreeGridData();
		try {

			 kwicNew.addQueryResultRows(queryResult);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private QueryResult createQueryResultFromTreeGridDataTags() {	
		TagQueryResult tagQueryResult = new TagQueryResult("some Tags");
		List<TreeRowItem> rootElements = selectedDataProvider.getTreeData().getRootItems();
		if (!rootElements.isEmpty()) {
			for (TreeRowItem root : rootElements) {
				List<TreeRowItem> children= new ArrayList<TreeRowItem>();
			   children	= selectedItemsTreeGridData.getChildren(root);
				if(!children.isEmpty()) {
					for (TreeRowItem child : children) 		{
						TagQueryResultRow tagQueryResultRow = (TagQueryResultRow) child.getQueryResultRow();
						tagQueryResult.add(tagQueryResultRow);
					}	
				}				
			}
		} else {
			// TODO
		}
		return tagQueryResult;
	}
	
	private ArrayList<QueryResultRow> createQueryResultFromTreeGridData() {	
		ArrayList<QueryResultRow> queryResult = new ArrayList<QueryResultRow>();
		List<TreeRowItem> rootElements = selectedDataProvider.getTreeData().getRootItems();
		if (!rootElements.isEmpty()) {
			for (TreeRowItem root : rootElements) {
				//
				List<TreeRowItem> children= new ArrayList<TreeRowItem>();
			   children	= selectedItemsTreeGridData.getChildren(root);
				if(!children.isEmpty()) {
					for (TreeRowItem child : children) 		{
						QueryResultRowArray queryResultRow = child.getRows();
						queryResult.addAll(queryResultRow);
					}	
				}				
			}
		} else {
		}

		return  queryResult;
	

	}

	private ArrayList<String> getQueriesForAvailableResults() {
		ArrayList<String> allQueries = new ArrayList<>();
		Iterator<CurrentTreeGridData> queriesIterator = currentTreeGridDatas.iterator();
		while (queriesIterator.hasNext()) {
			allQueries.add(queriesIterator.next().getQuery());
		}
		return allQueries;
	}

	@SuppressWarnings("unchecked")
	private void swichToResultTree(String queryAsString) {
		Iterator<CurrentTreeGridData> allResultsIterator = currentTreeGridDatas.iterator();
		resultsTreeGridData = new TreeData<TreeRowItem>();
		selectedGridView = null;
		while (allResultsIterator.hasNext()) {
			CurrentTreeGridData currentData = allResultsIterator.next();
			if (currentData.getQuery().equalsIgnoreCase(queryAsString)) {
				resultsTreeGridData = currentData.getCurrentTreeData();
				selectedGridView = currentData.getViewID();
			}
		}
		resultsTreeGrid = createResultsTreeGridFromData(resultsTreeGridData, selectedGridView);
		//resultsTreeGrid.setSelectionMode(SelectionMode.NONE);
		resultsTreeGrid.addStyleNames(
				"annotation-details-panel-annotation-details-grid", 
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");

		resultsTreeGrid.setWidth("100%");
		resultsTreeGrid.setHeight("230px");
		
		queryResultsPanel.setContent(resultsTreeGrid);
	}

	
	
	private TreeGrid<TreeRowItem> createResultsTreeGridFromData(TreeData<TreeRowItem> resultsTreeGridData2, ViewID currentView) {
		TreeGrid<TreeRowItem> resultSetTreeGrid = new TreeGrid<>();
		switch (currentView) {

		case phrase:
			resultSetTreeGrid = addDataPhraseStyle(resultsTreeGridData2);
			break;
		case phraseProperty:
			resultSetTreeGrid = addDataPhraseStyle(resultsTreeGridData2);
			break;
		case phraseTag:
			resultSetTreeGrid = addDataPhraseStyle(resultsTreeGridData2);
			break;
		case tag:
			resultSetTreeGrid = addDataTagStyle(resultsTreeGridData2);
			break;
		case property:
			resultSetTreeGrid = addDataPropertyStyle(resultsTreeGridData2);
			break;
		}
		return resultSetTreeGrid;
	}

	
	private TreeGrid<TreeRowItem> addDataTagStyle(TreeData<TreeRowItem> treeData) {
		TreeGrid<TreeRowItem> tagTreeGrid = new TreeGrid<>();
		TreeDataProvider<TreeRowItem> dataProvider = new TreeDataProvider<>(treeData);
		tagTreeGrid.addColumn(TreeRowItem::getTreeKey).setCaption("Tag").setId("tagID");
		tagTreeGrid.getColumn("tagID").setExpandRatio(7);
		tagTreeGrid.getColumn("tagID").setDescriptionGenerator(e-> e.getTreeKey() , ContentMode.HTML);
		tagTreeGrid.addColumn(TreeRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		tagTreeGrid.getColumn("freqID").setExpandRatio(1);
		
	    ButtonRenderer<TreeRowItem> selectItemsRenderer = new ButtonRenderer<TreeRowItem>(rendererClickEvent-> handleSelectClickEvent(rendererClickEvent));
	    selectItemsRenderer.setHtmlContentAllowed(true);		
	    tagTreeGrid.addColumn(TreeRowItem::getSelectIcon, selectItemsRenderer).setCaption("select").setId("selectIconID");
	    tagTreeGrid.getColumn("selectIconID").setExpandRatio(1);
		
		dataProvider.refreshAll();
		tagTreeGrid.setDataProvider(dataProvider);
		tagTreeGrid.setWidth("100%");
		
		tagTreeGrid.addExpandListener(new ExpandListener<TreeRowItem>() { 
			public void itemExpand(ExpandEvent<TreeRowItem> event) {
	    	handleExpandClickEventTag(event, dataProvider);
							
			}
		});
	
		return tagTreeGrid;
	}
	
	

	
	private TreeGrid<TreeRowItem> addDataPhraseStyle(TreeData<TreeRowItem> treeData) {
		 phraseTreeGrid = new TreeGrid<>();
	     dataProvider = new TreeDataProvider<>(treeData);
		
		phraseTreeGrid.addColumn(TreeRowItem::getTreeKey).setCaption("Phrase").setId("phraseID");
		phraseTreeGrid.getColumn("phraseID").setExpandRatio(7);
		phraseTreeGrid.addColumn(TreeRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		phraseTreeGrid.getColumn("freqID").setExpandRatio(1);
			
	    ButtonRenderer<TreeRowItem> selectItemsRenderer = new ButtonRenderer<TreeRowItem>( rendererClickEvent-> handleSelectClickEvent(rendererClickEvent));
	    selectItemsRenderer.setHtmlContentAllowed(true);		
	    phraseTreeGrid.addColumn(TreeRowItem::getSelectIcon, selectItemsRenderer).setCaption("select").setId("selectID");
		phraseTreeGrid.getColumn("selectID").setExpandRatio(1);
		
/*		ButtonRenderer<TreeRowItem> unfoldRenderer = new ButtonRenderer<TreeRowItem>( unfoldClickEvent-> handleUnfoldClickEvent(unfoldClickEvent, phraseTreeGrid));
		unfoldRenderer.setHtmlContentAllowed(true);
	    phraseTreeGrid.addColumn(TreeRowItem::getArrowIcon, unfoldRenderer).setId("arrowID");
	    phraseTreeGrid.getColumn("arrowID").setExpandRatio(1);*/
		
		dataProvider.refreshAll();
		phraseTreeGrid.setDataProvider(dataProvider);
		phraseTreeGrid.addExpandListener(new ExpandListener<TreeRowItem>() {
			
			private static final long serialVersionUID = 1L;

			public void itemExpand(ExpandEvent<TreeRowItem> event) {
				handleExpandClickEvent(event, phraseTreeGrid);
							
			}
		});
		
		return phraseTreeGrid;
	}

	
	private TreeGrid<TreeRowItem> addDataPropertyStyle(TreeData<TreeRowItem> treeData) {
		TreeGrid<TreeRowItem> propertyTreeGrid = new TreeGrid<>();
		TreeDataProvider<TreeRowItem> dataProvider = new TreeDataProvider<>(treeData);
		propertyTreeGrid.addColumn(TreeRowItem::getShortenTreeKey).setCaption("Tag").setId("tagID");
		propertyTreeGrid.getColumn("tagID").setExpandRatio(3);
		propertyTreeGrid.addColumn(TreeRowItem::getPropertyName).setCaption("Property name").setId("propNameID");
		propertyTreeGrid.getColumn("propNameID").setExpandRatio(3);
		propertyTreeGrid.addColumn(TreeRowItem::getPropertyValue).setCaption("Property value").setId("propValueID");
		propertyTreeGrid.getColumn("propValueID").setExpandRatio(3);
		propertyTreeGrid.addColumn(TreeRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		propertyTreeGrid.getColumn("freqID").setExpandRatio(1);
		dataProvider.refreshAll();
		propertyTreeGrid.setDataProvider(dataProvider);
		propertyTreeGrid.setWidth("100%");
		return propertyTreeGrid;
	}
	
 private void handleRemoveClickEvent(RendererClickEvent<TreeRowItem> removeClickEvent) {
	TreeRowItem toRemove= removeClickEvent.getItem();
	 selectedItemsTreeGridData.removeItem(toRemove);
	 selectedDataProvider.refreshAll();	 
   }
 
 
	private void handleUnfoldClickEvent(RendererClickEvent<TreeRowItem> arrowClickEvent,
			TreeGrid<TreeRowItem> phraseTreeGrid) {

		DocumentItem selectedItem = (DocumentItem) arrowClickEvent.getItem();
		if (!selectedItem.isUnfold()) {
			selectedItem.setUnfold(true);
			phraseTreeGrid.getDataProvider().refreshItem(selectedItem);
			System.out.println("TreeRowItems grouped : " + selectedItem.getRows().toString());
			ArrayList<TreeRowItem> children = createSingleItemRowsArrayList(selectedItem);
			dataProvider.getTreeData().addItems(selectedItem, children);
		} else {
			selectedItem.setUnfold(false);
			List<TreeRowItem> children = dataProvider.getTreeData().getChildren(selectedItem);
			dataProvider.getTreeData().getChildren(selectedItem).removeAll(children);
			dataProvider.refreshAll();

		}

	}
	
	private void handleExpandClickEvent(ExpandEvent<TreeRowItem> expandClickEvent,
			TreeGrid<TreeRowItem> phraseTreeGrid) {

		TreeRowItem clickedItem = expandClickEvent.getExpandedItem();
		TreeRowItem dummyItem= dataProvider.getTreeData().getChildren(clickedItem).get(0);

		if (clickedItem.getClass().equals(DocumentItem.class)&&(dummyItem.getTreeKey()==null)) {

			DocumentItem selectedItem = (DocumentItem) clickedItem;
			dataProvider.getTreeData().removeItem(dataProvider.getTreeData().getChildren(selectedItem).get(0));
			ArrayList<TreeRowItem> children = createSingleItemRowsArrayList(selectedItem);
			dataProvider.getTreeData().addItems(selectedItem, children);

		} else {
			
		}
		dataProvider.refreshAll();

	}
	
	private void handleExpandClickEventTag(ExpandEvent<TreeRowItem> expandClickEvent,
			TreeDataProvider<TreeRowItem> dataProvider) {

		TreeRowItem clickedItem = expandClickEvent.getExpandedItem();
		TreeRowItem dummyItem = dataProvider.getTreeData().getChildren(clickedItem).get(0);

		if (clickedItem.getClass().equals(CollectionItem.class) && (dummyItem.getTreeKey() == null)) {

			CollectionItem selectedItem = (CollectionItem) clickedItem;
			dataProvider.getTreeData().removeItem(dataProvider.getTreeData().getChildren(selectedItem).get(0));
			ArrayList<TreeRowItem> children = createSingleItemRowsArrayList(selectedItem);
			dataProvider.getTreeData().addItems(selectedItem, children);

		} else {

		}
		dataProvider.refreshAll();

	}

	
	
	private ArrayList <TreeRowItem> createSingleItemRowsArrayList(TreeRowItem selectedItem) {
		ArrayList <TreeRowItem> children = new ArrayList<>();
			
		QueryResultRowArray groupedChildren= selectedItem.getRows();
		
		Iterator<QueryResultRow> resultIterator=groupedChildren.iterator();
		
		while (resultIterator.hasNext()) {
			QueryResultRow queryResultRow = (QueryResultRow) resultIterator.next();
			QueryResultRowArray itemAsQRRA= new QueryResultRowArray();
			itemAsQRRA.add(queryResultRow);
	
			SingleItem item = new SingleItem();
			if(queryResultRow.getClass().equals(TagQueryResultRow.class)) {
				TagQueryResultRow tQRR=	(TagQueryResultRow)queryResultRow;
				item.setTreeKey(tQRR.getTagDefinitionPath());
			}else {
				item.setTreeKey(queryResultRow.getPhrase());
				
			}
				
			item.setRows(itemAsQRRA);
			
		    children.add((TreeRowItem)item);	
		}
		return children;
		
	}
	
	private void handleSelectClickEvent(RendererClickEvent<TreeRowItem> rendererClickEvent) {
		TreeRowItem selectedItem = rendererClickEvent.getItem();
	
		
		if(comboBox.getValue().contains("wild")) {
			addPhraseItemsToSelectedPanel(selectedItem);
		}
			
	
	
		addTagItemsToSelectedPanel(selectedItem);
		
	
	    
	    
	    
	    
	    

/*		List<TreeRowItem> childList = resultsTreeGridData.getChildren(selectedItem);
		List<TreeRowItem> childChildList;
		TreeRowItem parent = resultsTreeGridData.getParent(selectedItem);
		Optional<String> currentQuery = comboBox.getSelectedItem();	
		Collection<TreeRowItem> allRootItems = selectedItemsTreeGridData.getRootItems();	
         ArrayList<TreeRowItem> selectedItems=new ArrayList<TagRowItem>();
         selectedItems.add(selectedItem);
		
		// item is single-phrase-rowitems
		if (childList.isEmpty()) {
			System.out.println("rowitems:" + selectedItem.getTreePath());
			addItemsToSelectedPanel(currentQuery.toString(), (Collection<TagRowItem>) selectedItems, allRootItems);

		} else {
			// item is on document level
			childList = resultsTreeGridData.getChildren(selectedItem);
			childChildList = resultsTreeGridData.getChildren(childList.get(0));
			if (childChildList.isEmpty()) {
				System.out.println("document:" + selectedItem.getTreePath());
				addItemsToSelectedPanel(currentQuery.toString(), (Collection<TagRowItem>) childList, allRootItems);

			} else {
				// item is on resultPhrase level because the roots of the resultTree are the resultPhrase and not the query like in the selectedTree
				if (resultsTreeGridData.getParent(selectedItem) == null) {
					ArrayList <TagRowItem> allItems = new ArrayList<>();
					for (TagRowItem document :childList) {
						allItems.addAll(resultsTreeGridData.getChildren(document));
					}
					System.out.println("root, single phrase result:" + selectedItem.getTreePath());
					addItemsToSelectedPanel(currentQuery.toString(), (Collection<TagRowItem>) allItems, allRootItems);

				}

			}

		}*/

		// childList.stream().forEach(v -> System.out.println(v.getPhrase()));

	}

	
	
	
	
	
/*	private void addItemsToSelectedPanel(String query, Collection<TagRowItem> newItems,Collection<TagRowItem> currentRootItems) {	
		
		TagRowItem newRoot = new TagRowItem();
		TagRowItem phraseItem = new TagRowItem();
		int length=query.length();
		String queryString = query.substring(20, length-1);
		newRoot.setTreePath(queryString);
		Collection<TagRowItem> allRootItems= currentRootItems;
		Iterator<TagRowItem> rootsIterator =allRootItems.iterator();
		phraseItem.setTreePath(newItems.stream().findFirst().get().getTreePath());
	
		
		// all is deselected--- currently irelevant
		if (newItems.isEmpty()) {
			
		while (rootsIterator.hasNext()) { 
				TagRowItem emptyRootItem = rootsIterator.next();

				if (emptyRootItem.getTreePath().equals(queryString)) {
					selectedItemsTreeGridData.removeItem(emptyRootItem);
					selectedDataProvider.refreshAll();
					break;
				}
			}
		}
		
		else {
					
		boolean contains= false;
		
		if (allRootItems.isEmpty()) {
			// rootItem is the first/only item in the Grid
			//selectedItemsTreeGridData.addItems(null, newRoot);
			
	
			//selectedItemsTreeGridData.addItems(newRoot, phraseItem);
			
			if(selectedGridView==ViewID.phrase) {
				selectedItemsTreeGridData.clear();
				selectedItemsTreeGridData=	sortIncomingRowItemsToPhraseTreeData(newRoot, phraseItem,selectedItemsTreeGridData, newItems);
			}
			
			if(selectedGridView==ViewID.tag) {
				selectedItemsTreeGridData=	sortIncomingRowItemsToTagTreeData(newRoot,selectedItemsTreeGridData, newItems);
			}
			//selectedItemsTreeGridData.addItems(newRoot, items);
			selectedDataProvider.refreshAll();
		} else {
					
			for (TagRowItem rootItem : allRootItems) {
				// rootItem exists, therefore renew root+branch
				if (rootItem.getTreePath().equalsIgnoreCase(newRoot.getTreePath())) {
					
					contains=true;		
					//selectedItemsTreeGridData.removeItem(rootItem);	
					//selectedDataProvider.refreshAll();
					//selectedItemsTreeGridData.addItem(null, newRoot);
					
					ArrayList<TagRowItem> completeItemList = new ArrayList<>();
					completeItemList.addAll(newItems);
					
					List<TagRowItem> docList =selectedItemsTreeGridData.getChildren(rootItem);
					for(TagRowItem doc : docList) {
					completeItemList.addAll(selectedItemsTreeGridData.getChildren(doc));
						
					}
			
					
					if(selectedGridView==ViewID.phrase) {
						//selectedItemsTreeGridData.clear();
						selectedItemsTreeGridData=	sortIncomingRowItemsToPhraseTreeData(newRoot,phraseItem,selectedItemsTreeGridData, completeItemList);
						selectedDataProvider.refreshAll();
					}					
					if(selectedGridView==ViewID.tag) {
						selectedItemsTreeGridData=	sortIncomingRowItemsToTagTreeData(newRoot,selectedItemsTreeGridData, newItems);
					}
					//selectedItemsTreeGridData.addItems(newRoot, items);
					selectedItemsTreeGrid.setWidth("100%");
					selectedDataProvider.refreshAll();
					break;
				}
			}		
				if(!contains) {	
					// rootItem is new, but not the first in the grid
					selectedItemsTreeGridData.addItems(null,newRoot);
					if(selectedGridView==ViewID.phrase) {
						selectedItemsTreeGridData=	sortIncomingRowItemsToPhraseTreeData(newRoot,phraseItem,selectedItemsTreeGridData, newItems);
					}
					
					if(selectedGridView==ViewID.tag) {
						selectedItemsTreeGridData=	sortIncomingRowItemsToTagTreeData(newRoot,selectedItemsTreeGridData, newItems);
					}
					//selectedItemsTreeGridData.addItems(newRoot, items);
					selectedItemsTreeGrid.setWidth("100%");
					selectedDataProvider.refreshAll();
				}
			}
		}*/
		
	private void addTagItemsToSelectedPanel(TreeRowItem selectedItem) {
		QueryResultRowArray subresultGrouped = selectedItem.getRows();

		Collection<TreeRowItem> allRootItems = selectedItemsTreeGridData.getRootItems();
		Optional<String> currentQuery = comboBox.getSelectedItem();

		int length = currentQuery.toString().length();
		String queryString = currentQuery.toString().substring(20, length - 1);
		QueryRootItem queryRoot = new QueryRootItem();
		queryRoot.setTreeKey(queryString);

		if (allRootItems.isEmpty()) {
		
		} else {
			if (!allRootItems.contains(queryRoot)) {
		
			}
			else {
				if (allRootItems.contains(queryRoot)) {
			
				}
			}
		}
		selectedDataProvider.refreshAll();
		
	}

	private void addPhraseItemsToSelectedPanel(TreeRowItem selectedItem) {

		Collection<TreeRowItem> allRootItems = selectedItemsTreeGridData.getRootItems();
		Optional<String> currentQuery = comboBox.getSelectedItem();

		int length = currentQuery.toString().length();
		String queryString = currentQuery.toString().substring(20, length - 1);
		QueryRootItem queryRoot = new QueryRootItem();
		queryRoot.setTreeKey(queryString);

		if (allRootItems.isEmpty()) {
			selectedItemsTreeGridData.addItem(null, queryRoot);

			if (selectedItem.getClass().equals(DocumentItem.class)) {
				TreeRowItem root = resultsTreeGridData.getParent(selectedItem);
				selectedItemsTreeGridData.addItem(queryRoot, root);
				selectedItemsTreeGridData.addItem(root, selectedItem);
			}
			if (selectedItem.getClass().equals(RootItem.class)) {	
				List<TreeRowItem> children = resultsTreeGridData.getChildren(selectedItem);
				selectedItemsTreeGridData.addItem(queryRoot, selectedItem);
				selectedItemsTreeGridData.addItems(selectedItem, children);
			}
		} else {
			if (!allRootItems.contains(queryRoot)) {
				selectedItemsTreeGridData.addItem(null, queryRoot);
				if (selectedItem.getClass().equals(DocumentItem.class)) {
					TreeRowItem root = resultsTreeGridData.getParent(selectedItem);
					selectedItemsTreeGridData.addItem(queryRoot, root);
					selectedItemsTreeGridData.addItem(root, selectedItem);
				}
				if (selectedItem.getClass().equals(RootItem.class)) {
					List<TreeRowItem> children = resultsTreeGridData.getChildren(selectedItem);
					selectedItemsTreeGridData.addItem(queryRoot, selectedItem);
					selectedItemsTreeGridData.addItems(selectedItem, children);
				}
			}
			else {
				if (allRootItems.contains(queryRoot)) {
					if (selectedItem.getClass().equals(DocumentItem.class)) {
						TreeRowItem root = resultsTreeGridData.getParent(selectedItem);
						selectedItemsTreeGridData.addItem(queryRoot, root);
						selectedItemsTreeGridData.addItem(root, selectedItem);
					}
					if (selectedItem.getClass().equals(RootItem.class)) {
						List<TreeRowItem> children = resultsTreeGridData.getChildren(selectedItem);
						selectedItemsTreeGridData.addItem(queryRoot, selectedItem);
						selectedItemsTreeGridData.addItems(selectedItem, children);
					}
				}
			}
		}
		selectedDataProvider.refreshAll();
		// selectedItemsTreeGridData.addItem(parent, item)
	}
		
	
	private TreeData<TagRowItem> sortIncomingRowItemsToPhraseTreeData(TagRowItem root,TagRowItem phrase, TreeData<TagRowItem> treeData,
			Collection<TagRowItem> items) {
		// phrase is not first, root ( query) already inside
		//if(root == null)
		
			
		treeData.addItem(null, root);
		treeData.addItem(root, phrase);

		Collection<TagRowItem> allItems = items;

		ArrayList<TagRowItem> allDocs = new ArrayList<TagRowItem>();
		for (TagRowItem item : allItems) {
			String sourceDocName = item.getSourceDocName();
			TagRowItem doc = new TagRowItem();
			doc.setSourceDocName(sourceDocName);
			doc.setTreePath(sourceDocName);

			if (!allDocs.stream().anyMatch(var -> var.getSourceDocName().equalsIgnoreCase(sourceDocName))) {
				allDocs.add(doc);
			}
		}
		treeData.addItems(phrase, allDocs);
		

		for (TagRowItem doc : allDocs) {
			String sourceDoc = doc.getSourceDocName();
			ArrayList <TagRowItem>itemList = new ArrayList<>();
			for (TagRowItem item : allItems) {
				if (item.getSourceDocName().equalsIgnoreCase(sourceDoc)) {
					itemList.add(item);
				}
			}
			treeData.addItems(doc, itemList);
		}

		return treeData;
	}

	private TreeData<TagRowItem> sortIncomingRowItemsToTagTreeData(TagRowItem root, TreeData<TagRowItem> treeData,
			Collection<TagRowItem> items) {
		Collection<TagRowItem> allItems = items;
		// adding tags as children for the query
		ArrayList<TagRowItem> allTags = new ArrayList<TagRowItem>();
		for (TagRowItem item : allItems) {
			String tagName = item.getTagDefinitionPath();
			TagRowItem tag = new TagRowItem();
			tag.setSourceDocName(item.getSourceDocName());
			tag.setCollectionName(item.getCollectionName());
			tag.setTreePath(tagName);
			tag.setTagDefinitionPath(tagName);

			if (!allTags.stream().anyMatch(var -> var.getTagDefinitionPath().equalsIgnoreCase(tagName))) {
				allTags.add(tag);
			}

		}
		treeData.addItems(root, allTags);

		// adding documents as children for the tags
		for (TagRowItem oneTag : allTags) {
			// String sourceDocName = tag.getSourceDocName();
			ArrayList<TagRowItem> documentsForATag = new ArrayList<TagRowItem>();
			for (TagRowItem item : allItems) {
				// add document if not already inside
				TagRowItem doc = new TagRowItem();
				doc.setSourceDocName(item.getSourceDocName());
				doc.setTagDefinitionPath(oneTag.getTagDefinitionPath());

				if (!documentsForATag.stream()
						.anyMatch(var -> var.getTagDefinitionPath().equalsIgnoreCase(doc.getTagDefinitionPath()))
						&& (!documentsForATag.stream()
								.anyMatch(var -> var.getSourceDocName().equalsIgnoreCase(doc.getSourceDocName())))) {

					/*
					 * if (!(documentsForATag.stream() .anyMatch(var ->
					 * var.getTagDefinitionPath().equalsIgnoreCase(tag.getTagDefinitionPath())&&
					 * var.getSourceDocName().equalsIgnoreCase(doc.getSourceDocName())))) {
					 */

					//doc.setTagDefinitionPath(item.getTagDefinitionPath());
					doc.setTreePath(item.getSourceDocName());
					doc.setSourceDocName(item.getSourceDocName());
					documentsForATag.add(doc);

					treeData.addItem(oneTag, doc);
					
					//search for collections for that document where oneTag is used

					ArrayList<TagRowItem> collectionsForADocument = new ArrayList<TagRowItem>();

					for (TagRowItem oneItem : allItems) {
						TagRowItem oneCollection = new TagRowItem();
						//oneCollection.setCollectionName(oneItem.getCollectionName());
						oneCollection.setSourceDocName(doc.getSourceDocName());
						oneCollection.setTagDefinitionPath(oneTag.getTagDefinitionPath());
						oneCollection.setCollectionName(oneItem.getCollectionName());
						
						if((oneItem.getTagDefinitionPath().equalsIgnoreCase(oneCollection.getTagDefinitionPath()))&&((oneItem.getSourceDocName().equalsIgnoreCase(oneCollection.getSourceDocName())))) {
							

						if (!collectionsForADocument.stream()
								.anyMatch(var -> var.getCollectionName().equalsIgnoreCase(oneCollection.getCollectionName()))){
									oneCollection.setTreePath(oneItem.getCollectionName());
									collectionsForADocument.add(oneCollection);

									treeData.addItem(doc, oneCollection);
									selectedDataProvider.refreshAll();
							
						}else {
							
							//create rowitems for the annotations
							
						}
							

						
						}
					}

				}
			}

		}

		return treeData;

	}

	
	private void setActionGridComponenet() {
		
		selectedItemsTreeGrid.addStyleNames(
				"annotation-details-panel-annotation-details-grid", 
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");

		ActionGridComponent<TreeGrid<TreeRowItem>> selectedGridComponent = 
				new ActionGridComponent<>(new Label("Selected resultrows for the kwic visualization"), selectedItemsTreeGrid);
		leftSide.addComponent(selectedGridComponent);
	}
	

	@Override
	public void setQueryResults() {
		// TODO Auto-generated method stub

	}

}
