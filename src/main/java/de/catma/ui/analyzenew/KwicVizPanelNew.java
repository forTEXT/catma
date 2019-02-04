package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.DataChangeEvent;
import com.vaadin.data.provider.DataProviderListener;
import com.vaadin.data.provider.GridSortOrder;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.ContextClickEvent;
import com.vaadin.event.SortEvent;
import com.vaadin.event.SortEvent.SortListener;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.event.selection.SingleSelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ClientConnector.AttachEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.TagQueryResult;
import de.catma.queryengine.result.TagQueryResultRow;

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
	private Panel treeGridPanelKwic;
	private HorizontalLayout mainContentPanel;
	private Panel rightSide;
	private ArrayList<CurrentTreeGridData> currentTreeGridDatas;
	private TreeData<TagRowItem> resultsTreeGridData;
	private TreeGrid<TagRowItem> resultsTreeGrid;
	//private Grid<TagRowItem>selectedItemsGrid;
	private TreeData<TagRowItem> selectedItemsTreeGridData;
	private TreeGrid<TagRowItem>selectedItemsTreeGrid;
	private TreeDataProvider<TagRowItem> selectedDataProvider;
	private KwicPanelNew kwicNew;
	private Button arrowLeftBt;

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
		treeGridPanelKwic = new Panel();
		treeGridPanelKwic.setHeight("350px");
		resultsTreeGrid= new TreeGrid<>();
		selectedItemsTreeGrid = new TreeGrid<TagRowItem>();
		selectedItemsTreeGrid.addColumn(TagRowItem::getTreePath).setCaption("tag");
		selectedItemsTreeGrid.addColumn(TagRowItem::getPhrase).setCaption("phrase");
	
		selectedItemsTreeGridData= new 	TreeData<TagRowItem>();
		selectedDataProvider = new TreeDataProvider<>(selectedItemsTreeGridData);
		selectedItemsTreeGrid.setDataProvider(selectedDataProvider);
		selectedItemsTreeGrid.setWidth("100%");
		//selectedItemsGrid.addColumn(TagRowItem::getPhrase).setCaption("Phrase");
		selectedItemsPanel= new Panel();
		selectedItemsPanel.setCaption("selected items for the kwic visualization");

		selectedItemsPanel.setWidth("100%");
		
		selectedItemsPanel.setContent(selectedItemsTreeGrid);
		leftSide.addComponent(comboBox);
		leftSide.addComponent(treeGridPanelKwic);
		leftSide.addComponent(selectedItemsPanel);
		
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
		
		selectedDataProvider.addDataProviderListener(new DataProviderListener<TagRowItem>() {
			
			@Override
			public void onDataChange(DataChangeEvent<TagRowItem> event) {
				if(comboBox.getValue().contains("tag")) {
					updateKwicView();
					System.out.println("TreeGrid hat sich geändert");
					
				}
				

				
			}
		});
	}
	
	private void updateKwicView() {
		QueryResult queryResult = createQueryResultFromTreeGridData();
		try {
			
			kwicNew.addQueryResultRows(queryResult);
			
		} catch (Exception e) {		
			e.printStackTrace();
		}
	}
	
	
	
	
	private QueryResult createQueryResultFromTreeGridData() {
		TagQueryResult tagQueryResult = new TagQueryResult("some Tags");

		List<TagRowItem> rootElements = selectedDataProvider.getTreeData().getRootItems();

		if (!rootElements.isEmpty()) {
			for (TagRowItem root : rootElements) {
				List<TagRowItem> children= new ArrayList<TagRowItem>();
			   children	= selectedItemsTreeGridData.getChildren(root);
				if(!children.isEmpty()) {
					for (TagRowItem child : children) 		{
						TagQueryResultRow tagQueryResultRow = (TagQueryResultRow) child.getQueryResultRow();
						tagQueryResult.add(tagQueryResultRow);
					}	
				}				
			}
		} else {
		}

		return tagQueryResult;
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
		resultsTreeGridData = new TreeData<TagRowItem>();
		ViewID selectedGridView = null;
		while (allResultsIterator.hasNext()) {
			CurrentTreeGridData currentData = allResultsIterator.next();
			if (currentData.getQuery().equalsIgnoreCase(queryAsString)) {
				resultsTreeGridData = currentData.getCurrentTreeData();
				selectedGridView = currentData.getViewID();
			}
		}
		resultsTreeGrid = createTreeGridFromData(resultsTreeGridData, selectedGridView);
		resultsTreeGrid.setSelectionMode(SelectionMode.MULTI);
		resultsTreeGrid.setWidth("100%");
		resultsTreeGrid.setHeight("100%");
		
		// first try to add a selection logic to the tree
/*		resultsTreeGrid.addSelectionListener(new SelectionListener<TagRowItem>() {
			@Override
			public void selectionChange(SelectionEvent<TagRowItem> event) {
				Iterable<TagRowItem> selectedItems = event.getAllSelectedItems();
				selectedItems.forEach(item -> {
					System.out.println(" TgaPath :" + item.getTreePath() + " Collection :" + item.getCollectionName()
							+ " Tag ID:" + item.getTagInstanceID());
				});
				for (TagRowItem item : selectedItems) {
					TagRowItem parent = resultsTreeGridData.getParent(item);
					// can have siblings
					if (parent != null) {
						checkIfAllSiblingsAreSelectedAndSelectParent(item);
						// setChildrenSelected(item);
						// is root= no siblings
					} else {
						// setChildrenSelected(item);
					}
				}
			}
		});*/
		// add selected items to the slected-panel
	     resultsTreeGrid.addSelectionListener(new SelectionListener<TagRowItem>() {
				
				@Override
				public void selectionChange(SelectionEvent<TagRowItem> event) {
					
					Optional<String> currentQuery = comboBox.getSelectedItem();
					
					
					Collection<TagRowItem> allRootItems = selectedItemsTreeGridData.getRootItems();
					
					
				Iterable<TagRowItem> selectedItems=	event.getAllSelectedItems();
		
				
				selectedItems.forEach(item -> { System.out.println(" Tag__Path :"+item.getTreePath()+ 
						" Collection :"+item.getCollectionName()+"Tag__ID:"+item.getTagInstanceID());});
				
				//selectedItems.forEach(item-> addItemToSelectedPanel(item));
				addItemsToSelectedPanel(currentQuery.toString(), (Collection<TagRowItem>) selectedItems, allRootItems);
				
				}
			});
		treeGridPanelKwic.setContent(resultsTreeGrid);
	}

	
	private void checkIfAllSiblingsAreSelectedAndSelectParent(TagRowItem item) {
		boolean allChildrenSelected = true;
		TagRowItem parent = resultsTreeGridData.getParent(item);
		List<TagRowItem> children = resultsTreeGridData.getChildren(item);
		List<TagRowItem> siblings = resultsTreeGridData.getChildren(parent);

		/*
		 * if (siblings.size() == 1) { treeGridTag.asMultiSelect().select(parent); }
		 * else {
		 */

		for (TagRowItem sibl : siblings) {
			if (resultsTreeGrid.asMultiSelect().isSelected(sibl)) {
				// allChildrenSelected remains true
			} else {
				allChildrenSelected = false;
			}
		}
		if (allChildrenSelected) {
			resultsTreeGrid.asMultiSelect().select(parent);

		} else {
			resultsTreeGrid.asMultiSelect().deselect(parent);
		}
	}

	
	private TreeGrid<TagRowItem> createTreeGridFromData(TreeData<TagRowItem> treeData, ViewID currentView) {
		TreeGrid<TagRowItem> selectedTreeGrid = new TreeGrid<>();
		switch (currentView) {
		case tag:
			selectedTreeGrid = addDataTagStyle(treeData);
			break;
		case property:
			selectedTreeGrid = addDataPropertyStyle(treeData);
			break;
		case phrase:
			selectedTreeGrid = addDataPhraseStyle(treeData);
			break;
		case phraseProperty:
			selectedTreeGrid = addDataPhraseStyle(treeData);
			break;
		case phraseTag:
			selectedTreeGrid = addDataPhraseStyle(treeData);
			break;
		}
		return selectedTreeGrid;
	}

	
	private TreeGrid<TagRowItem> addDataTagStyle(TreeData<TagRowItem> treeData) {
		TreeGrid<TagRowItem> selectedTreeGrid = new TreeGrid<>();
		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(treeData);
		selectedTreeGrid.addColumn(TagRowItem::getTreePath).setCaption("Tag").setId("tagID");
		selectedTreeGrid.getColumn("tagID").setExpandRatio(7);
		selectedTreeGrid.addColumn(TagRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		selectedTreeGrid.getColumn("freqID").setExpandRatio(1);
		dataProvider.refreshAll();
		selectedTreeGrid.setDataProvider(dataProvider);
		selectedTreeGrid.setWidth("100%");
		return selectedTreeGrid;
	}

	
	private TreeGrid<TagRowItem> addDataPhraseStyle(TreeData<TagRowItem> treeData) {
		TreeGrid<TagRowItem> selectedTreeGrid = new TreeGrid<>();
		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(treeData);
		selectedTreeGrid.addColumn(TagRowItem::getTreePath).setCaption("Phrase").setId("phraseID");
		selectedTreeGrid.getColumn("phraseID").setExpandRatio(7);
		selectedTreeGrid.addColumn(TagRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		selectedTreeGrid.getColumn("freqID").setExpandRatio(1);
		dataProvider.refreshAll();
		selectedTreeGrid.setDataProvider(dataProvider);
		return selectedTreeGrid;
	}

	
	private TreeGrid<TagRowItem> addDataPropertyStyle(TreeData<TagRowItem> treeData) {
		TreeGrid<TagRowItem> selectedTreeGrid = new TreeGrid<>();
		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(treeData);
		selectedTreeGrid.addColumn(TagRowItem::getTreePath).setCaption("Tag").setId("tagID");
		selectedTreeGrid.getColumn("tagID").setExpandRatio(3);
		selectedTreeGrid.addColumn(TagRowItem::getPropertyName).setCaption("Property name").setId("propNameID");
		selectedTreeGrid.getColumn("propNameID").setExpandRatio(3);
		selectedTreeGrid.addColumn(TagRowItem::getPropertyValue).setCaption("Property value").setId("propValueID");
		selectedTreeGrid.getColumn("propValueID").setExpandRatio(3);
		selectedTreeGrid.addColumn(TagRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		selectedTreeGrid.getColumn("freqID").setExpandRatio(1);
		dataProvider.refreshAll();
		selectedTreeGrid.setDataProvider(dataProvider);
		selectedTreeGrid.setWidth("100%");
		return selectedTreeGrid;
	}
	// here and actual only tag queries
	private void addItemsToSelectedPanel(String query, Collection<TagRowItem> items,Collection<TagRowItem> allRootItemsIncoming) {
		
		TagRowItem newRoot = new TagRowItem();
		int length=query.length();

		newRoot.setTreePath(	query.substring(20, length-1));
		final Collection<TagRowItem> allRootItems= allRootItemsIncoming;
		boolean contains= false;
		
		if (allRootItems.isEmpty()) {
			// rootItem is first item in the Grid
			selectedItemsTreeGridData.addItems(null, newRoot);
			selectedItemsTreeGridData.addItems(newRoot, items);
			selectedDataProvider.refreshAll();
		} else {
					
			for (TagRowItem rootItem : allRootItems) {
				// rootItem exists, therefore renew root+branch
				if (rootItem.getTreePath().equalsIgnoreCase(newRoot.getTreePath())) {
					contains=true;		
					selectedItemsTreeGridData.removeItem(rootItem);
					selectedItemsTreeGridData.addItem(null, newRoot);
					selectedItemsTreeGridData.addItems(newRoot, items);
					selectedItemsTreeGrid.setWidth("100%");
					selectedDataProvider.refreshAll();
					break;
				}
			}		
				if(!contains) {	
					// rootItem is new in the grid
					selectedItemsTreeGridData.addItems(null,newRoot);
					selectedItemsTreeGridData.addItems(newRoot, items);
					selectedItemsTreeGrid.setWidth("100%");
					selectedDataProvider.refreshAll();
				}
			}
		}
	

	@Override
	public void setQueryResults() {
		// TODO Auto-generated method stub

	}

}
