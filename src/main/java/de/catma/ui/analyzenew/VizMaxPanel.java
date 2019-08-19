package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.ExpandEvent;
import com.vaadin.event.ExpandEvent.ExpandListener;
import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.event.selection.SingleSelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.themes.ValoTheme;

import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.source.KeywordInContext;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.analyzenew.kwic.KwicPanelNew;
import de.catma.ui.analyzenew.queryresultpanel.QueryResultPanel;
import de.catma.ui.analyzenew.queryresultpanel.QueryResultPanelSetting;
import de.catma.ui.analyzenew.treegridhelper.CollectionItem;
import de.catma.ui.analyzenew.treegridhelper.DocumentItem;
import de.catma.ui.analyzenew.treegridhelper.QueryRootItem;
import de.catma.ui.analyzenew.treegridhelper.RootItem;
import de.catma.ui.analyzenew.treegridhelper.SingleItem;
import de.catma.ui.analyzenew.treegridhelper.TreeRowItem;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.actiongrid.ActionGridComponent;

public class VizMaxPanel extends VerticalLayout  {
	
	private static class QuerySelection {
		private QueryResultPanelSetting setting;
		private QueryResultPanel panel;
		public QuerySelection(QueryResultPanelSetting setting) {
			super();
			this.setting = setting;
		}
		
		public void setPanel(QueryResultPanel panel) {
			this.panel = panel;
		}
		
		public QueryResultPanel getPanel() {
			return panel;
		}
	}
	
	interface LeaveListener {
		public void onLeave(VizMaxPanel vizMaxPanel);
	}

	private LoadingCache<String, KwicProvider> kwicProviderCache;

	private HorizontalSplitPanel mainContentSplitPanel;
	private Button arrowLeftBt;
	private ComboBox<QuerySelection> queryResultBox;
	private ArrayList<CurrentTreeGridData> currentTreeGridDatas;
	private TreeData<TreeRowItem> resultsTreeGridData;
	private TreeGrid<TreeRowItem> resultsTreeGrid;
	private TreeGrid<TreeRowItem> phraseTreeGrid;
	private TreeGrid<TreeRowItem> tagTreeGrid;
	
	private TreeDataProvider<TreeRowItem> phraseDataProvider;
	private TreeDataProvider<TreeRowItem> tagDataProvider;
	private TreeDataProvider<TreeRowItem> propertyFlatDataProvider;
	
	private TreeData<TreeRowItem> selectedItemsTreeGridData;
	private TreeGrid<TreeRowItem> selectedItemsTreeGrid;
	private TreeGrid<TreeRowItem> propertyTreeGrid;
	private TreeGrid <TreeRowItem> propertyFlatTreeGrid;
	private TreeDataProvider<TreeRowItem> propertyDataProvider;
	private TreeDataProvider<TreeRowItem> selectedDataProvider;
	
	private KwicPanelNew kwicNew;
	private ViewID selectedGridViewID;
	private int kwicSize = 5;

	public VizMaxPanel(ArrayList<CurrentTreeGridData> currentTreeGridDatas, 
			List<QueryResultPanelSetting> queryResultPanelSettings, Repository repository,
			LoadingCache<String, KwicProvider> kwicProviderCache, LeaveListener leaveListener) {
		this.currentTreeGridDatas = currentTreeGridDatas;

		this.kwicProviderCache = kwicProviderCache;
		initComponents();
		initActions(leaveListener);
		initData(queryResultPanelSettings);
		
	}

	private void initData(List<QueryResultPanelSetting> queryResultPanelSettings) {
		queryResultBox.setItems(queryResultPanelSettings.stream().map(settings -> new QuerySelection(settings)));
	}

	private void initComponents() {
		setSizeFull();
		
		mainContentSplitPanel = new HorizontalSplitPanel();
		mainContentSplitPanel.setSplitPosition(40, Sizeable.Unit.PERCENTAGE);
		
		addComponent(mainContentSplitPanel);
		
		// left column
		
		VerticalSplitPanel resultSelectionSplitPanel = new VerticalSplitPanel();
		mainContentSplitPanel.addComponent(resultSelectionSplitPanel);
		
		// top left 
		
		VerticalLayout topLeftPanel = new VerticalLayout();
		topLeftPanel.setSizeFull();
		resultSelectionSplitPanel.addComponent(topLeftPanel);
		
		HorizontalLayout buttonAndBoxPanel = new HorizontalLayout();
		buttonAndBoxPanel.setWidth("100%");
		buttonAndBoxPanel.setMargin(false);
		topLeftPanel.addComponent(buttonAndBoxPanel);
		
		arrowLeftBt = new IconButton(VaadinIcons.ARROW_LEFT);
		buttonAndBoxPanel.addComponent(arrowLeftBt);
		
		queryResultBox = new ComboBox<QuerySelection>();
		queryResultBox.setWidth("100%");
		queryResultBox.setEmptySelectionCaption("Select a resultset");
		
		buttonAndBoxPanel.addComponent(queryResultBox);
		
		resultsTreeGrid = new TreeGrid<>();
		topLeftPanel.addComponent(resultsTreeGrid);
		topLeftPanel.setExpandRatio(resultsTreeGrid, 1f);

		
		// bottom left

		
		selectedItemsTreeGrid = new TreeGrid<TreeRowItem>();
		selectedItemsTreeGrid.setSizeFull();

		selectedItemsTreeGridData = new TreeData<TreeRowItem>();
		selectedDataProvider = new TreeDataProvider<>(selectedItemsTreeGridData);
		selectedItemsTreeGrid.setDataProvider(selectedDataProvider);

		selectedItemsTreeGrid.addColumn(TreeRowItem::getShortenTreeKey).setCaption("tag/phrase").setId("treeKeyID");
		selectedItemsTreeGrid.getColumn("treeKeyID").setWidth(150);
		selectedItemsTreeGrid.getColumn("treeKeyID").setDescriptionGenerator(e -> e.getTreeKey(), ContentMode.HTML);

		selectedItemsTreeGrid.addColumn(TreeRowItem::getContext).setCaption("context").setId("contextID");
		selectedItemsTreeGrid.getColumn("contextID").setWidth(140);
	    selectedItemsTreeGrid.getColumn("contextID").setDescriptionGenerator(e -> e.getContextDiv(), ContentMode.HTML);
	    
	    selectedItemsTreeGrid.addColumn(TreeRowItem::getPropertyName).setCaption("Property").setId("propName").setHidable(true)
		.setHidden(true).setWidth(90);
	    selectedItemsTreeGrid.getColumn("propName").setDescriptionGenerator(e -> e.getPropertyName(), ContentMode.HTML);
	    
	    selectedItemsTreeGrid.addColumn(TreeRowItem::getPropertyValue).setCaption("Value").setId("propValue").setHidable(true)
		.setHidden(true).setWidth(90);
	    selectedItemsTreeGrid.getColumn("propValue").setDescriptionGenerator(e -> e.getPropertyValue(), ContentMode.HTML);

		ButtonRenderer<TreeRowItem> removeItemsRenderer = new ButtonRenderer<TreeRowItem>(
				removeClickEvent -> handleRemoveClickEvent(removeClickEvent));
		removeItemsRenderer.setHtmlContentAllowed(true);
		selectedItemsTreeGrid.addColumn(TreeRowItem::getRemoveIcon, removeItemsRenderer).setCaption("remove")
				.setId("removeID");
		selectedItemsTreeGrid.getColumn("removeID").setWidth(70);

		//TODO: make annotation-details-panel-annotation-details-grid generic 
		selectedItemsTreeGrid.addStyleNames("annotation-details-panel-annotation-details-grid",
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");

		ActionGridComponent<TreeGrid<TreeRowItem>> selectedGridComponent = new ActionGridComponent<>(
				new Label("Selected resultrows for the kwic visualization"), selectedItemsTreeGrid);

		resultSelectionSplitPanel.addComponent(selectedGridComponent);
		
		// right column
		
		//TODO: should be passed in as "Visualization"
		kwicNew = new KwicPanelNew(kwicProviderCache);
		kwicNew.setHeight("100%");
		kwicNew.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		
		mainContentSplitPanel.addComponent(kwicNew);
	}

	private void initActions(LeaveListener leaveListener) {
		arrowLeftBt.addClickListener(clickEvent -> leaveListener.onLeave(this));
		queryResultBox.addSelectionListener(new SingleSelectionListener<String>() {
			@Override
			public void selectionChange(SingleSelectionEvent<String> event) {
				String queryAsString = event.getSource().getValue();
				switchToResultTree(queryAsString);
			}
		});
	}

	//TODO: replace concrete class kwicNew with interface
	public void addQueryResultsToVisualisation(ArrayList<QueryResultRow> queryResultRows) {
		try {
			kwicNew.addQueryResultRows(queryResultRows);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	//TODO: replace concrete class kwicNew with interface
	public void removeQueryResultsFromVisualisation(ArrayList<QueryResultRow> queryResultRows) {
		try {
			kwicNew.removeQueryResultRows(queryResultRows);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private ArrayList<String> getQueriesForAvailableResults() {
		//TODO: refactor to work with TreeGridData directly
		ArrayList<String> allQueries = new ArrayList<>();
		Iterator<CurrentTreeGridData> queriesIterator = currentTreeGridDatas.iterator();
		while (queriesIterator.hasNext()) {
			allQueries.add(queriesIterator.next().getQuery());
		}
		return allQueries;
	}

	@SuppressWarnings("unchecked")
	private void switchToResultTree(String queryAsString) {
		Iterator<CurrentTreeGridData> allResultsIterator = currentTreeGridDatas.iterator();
		resultsTreeGridData = new TreeData<TreeRowItem>();
		selectedGridViewID = null;
		while (allResultsIterator.hasNext()) {
			CurrentTreeGridData currentData = allResultsIterator.next();
			if (currentData.getQuery().equalsIgnoreCase(queryAsString)) {
				resultsTreeGridData = currentData.getCurrentTreeData();
				selectedGridViewID = currentData.getViewID();
			}
		}
		resultsTreeGrid = createResultsTreeGridFromData(resultsTreeGridData, selectedGridViewID);
		resultsTreeGrid.addStyleNames("annotation-details-panel-annotation-details-grid",
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");
//		queryResultsPanel.setContent(resultsTreeGrid);			
	}

	private TreeGrid<TreeRowItem> createResultsTreeGridFromData(TreeData<TreeRowItem> resultsTreeGridData2,
			ViewID currentView) {
		TreeGrid<TreeRowItem> resultSetTreeGrid = new TreeGrid<>();
		switch (currentView) {
		case flatTableProperty:
			resultSetTreeGrid = addDataFlatTableStyle(resultsTreeGridData2);
			break;
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

	private TreeGrid<TreeRowItem> addDataPhraseStyle(TreeData<TreeRowItem> treeData) {
		phraseTreeGrid = new TreeGrid<>();
		phraseTreeGrid.setWidth(570, Unit.PIXELS);
		phraseDataProvider = new TreeDataProvider<>(treeData);

		phraseTreeGrid.addColumn(TreeRowItem::getShortenTreeKey).setCaption("Phrase").setId("phraseID");
		phraseTreeGrid.getColumn("phraseID").setWidth(200);
		phraseTreeGrid.getColumn("phraseID").setDescriptionGenerator(e -> e.getTreeKey(), ContentMode.HTML);

		phraseTreeGrid.addColumn(TreeRowItem::getPosition).setCaption("Position").setId("positionID").setHidable(true)
				.setHidden(true);
		phraseTreeGrid.getColumn("positionID").setWidth(80);

		phraseTreeGrid.addColumn(TreeRowItem::getContext).setCaption("Context").setId("contextID").setHidable(true)
				.setHidden(true).setWidth(100);
		phraseTreeGrid.getColumn("contextID").setDescriptionGenerator(e -> e.getContextDiv(), ContentMode.HTML);

		phraseTreeGrid.addColumn(TreeRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		phraseTreeGrid.getColumn("freqID").setWidth(85);

		ButtonRenderer<TreeRowItem> selectItemsRenderer = new ButtonRenderer<TreeRowItem>(
				rendererClickEvent -> handleSelectClickEvent(rendererClickEvent));
		selectItemsRenderer.setHtmlContentAllowed(true);
		phraseTreeGrid.addColumn(TreeRowItem::getSelectIcon, selectItemsRenderer).setCaption("select")
				.setId("selectID");
		phraseTreeGrid.getColumn("selectID").setWidth(70);

		phraseDataProvider.refreshAll();
		phraseTreeGrid.setDataProvider(phraseDataProvider);

		phraseTreeGrid.addExpandListener(new ExpandListener<TreeRowItem>() {

			public void itemExpand(ExpandEvent<TreeRowItem> event) {
				handleExpandClickEventPhrase(event);

			}
		});
		return phraseTreeGrid;
	}

	private TreeGrid<TreeRowItem> addDataTagStyle(TreeData<TreeRowItem> treeData) {
		tagTreeGrid = new TreeGrid<>();
		tagTreeGrid.setWidth(570, Unit.PIXELS);
		tagDataProvider = new TreeDataProvider<>(treeData);

		tagTreeGrid.addColumn(TreeRowItem::getShortenTreeKey).setCaption("Tag").setId("tagID");
		tagTreeGrid.getColumn("tagID").setWidth(160);
		tagTreeGrid.getColumn("tagID").setDescriptionGenerator(e -> e.getTreeKey(), ContentMode.HTML);

		tagTreeGrid.addColumn(TreeRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		tagTreeGrid.getColumn("freqID").setWidth(90);

		tagTreeGrid.addColumn(TreeRowItem::getPosition).setCaption("Position").setId("positionID").setHidable(true)
				.setHidden(true);
		tagTreeGrid.getColumn("positionID").setWidth(80);

		tagTreeGrid.addColumn(TreeRowItem::getContext).setCaption("Context").setId("contextID").setHidable(true)
				.setHidden(true).setWidth(150);
		tagTreeGrid.getColumn("contextID").setDescriptionGenerator(e -> e.getContextDiv(), ContentMode.HTML);

		ButtonRenderer<TreeRowItem> selectItemsRenderer = new ButtonRenderer<TreeRowItem>(
				rendererClickEvent -> handleSelectClickEvent(rendererClickEvent));
		selectItemsRenderer.setHtmlContentAllowed(true);
		tagTreeGrid.addColumn(TreeRowItem::getSelectIcon, selectItemsRenderer).setCaption("select")
				.setId("selectIconID");
		tagTreeGrid.getColumn("selectIconID").setWidth(60);

		tagDataProvider.refreshAll();
		tagTreeGrid.setDataProvider(tagDataProvider);
		tagTreeGrid.addExpandListener(new ExpandListener<TreeRowItem>() {
			public void itemExpand(ExpandEvent<TreeRowItem> event) {
				handleExpandClickEventTag(event);

			}
		});
		return tagTreeGrid;
	}

	private TreeGrid<TreeRowItem> addDataPropertyStyle(TreeData<TreeRowItem> treeData) {
		propertyTreeGrid = new TreeGrid<>();
		propertyTreeGrid.setWidth(570, Unit.PIXELS);
		propertyDataProvider = new TreeDataProvider<>(treeData);

		propertyTreeGrid.addColumn(TreeRowItem::getShortenTreeKey).setCaption("Tag").setId("tagID");
		propertyTreeGrid.getColumn("tagID").setWidth(140);
		propertyTreeGrid.getColumn("tagID").setDescriptionGenerator(e -> e.getTreeKey(), ContentMode.HTML);

		propertyTreeGrid.addColumn(TreeRowItem::getContext).setCaption("Context").setId("contextID").setHidable(true)
				.setHidden(true).setWidth(90);
		propertyTreeGrid.getColumn("contextID").setDescriptionGenerator(e -> e.getContextDiv(), ContentMode.HTML);

		ButtonRenderer<TreeRowItem> selectItemsRenderer = new ButtonRenderer<TreeRowItem>(
				rendererClickEvent -> handleSelectClickEvent(rendererClickEvent));
		selectItemsRenderer.setHtmlContentAllowed(true);

		propertyTreeGrid.addColumn(TreeRowItem::getPropertyName).setCaption("Property").setId("propNameID");
		propertyTreeGrid.getColumn("propNameID").setWidth(90);
		propertyTreeGrid.getColumn("propNameID").setDescriptionGenerator(e -> e.getPropertyName(), ContentMode.HTML);
		propertyTreeGrid.addColumn(TreeRowItem::getPropertyValue).setCaption("Value").setId("propValueID");
		propertyTreeGrid.getColumn("propValueID").setWidth(80);
		propertyTreeGrid.getColumn("propValueID").setDescriptionGenerator(e -> e.getPropertyValue(), ContentMode.HTML);

		propertyTreeGrid.addColumn(TreeRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		propertyTreeGrid.getColumn("freqID").setWidth(80);
		propertyTreeGrid.addColumn(TreeRowItem::getSelectIcon, selectItemsRenderer).setCaption("select")
				.setId("selectIconID");
		propertyTreeGrid.getColumn("selectIconID").setWidth(60);
		propertyDataProvider.refreshAll();
		propertyTreeGrid.setDataProvider(propertyDataProvider);
		propertyTreeGrid.addExpandListener(new ExpandListener<TreeRowItem>() {

			public void itemExpand(ExpandEvent<TreeRowItem> event) {
				handleExpandClickEventProperty(event);

			}
		});

		return propertyTreeGrid;
	}
	
	
	private TreeGrid<TreeRowItem> addDataFlatTableStyle(TreeData<TreeRowItem> treeData) {	
		TreeData <TreeRowItem> dataWithContext=setContextToDataObject(treeData);
		propertyFlatTreeGrid = new TreeGrid<TreeRowItem>();
		propertyFlatTreeGrid.setWidth(570, Unit.PIXELS);
		propertyFlatDataProvider = new TreeDataProvider<>(dataWithContext);

		propertyFlatTreeGrid.addColumn(TreeRowItem::getShortenTreeKey).setCaption("Tag").setId("tagID");
		propertyFlatTreeGrid.getColumn("tagID").setWidth(140);
		propertyFlatTreeGrid.getColumn("tagID").setDescriptionGenerator(e -> e.getTreeKey(), ContentMode.HTML);

		propertyFlatTreeGrid.addColumn(TreeRowItem::getContext).setCaption("Context").setId("contextID").setHidable(true)
				.setHidden(true).setWidth(90);
		propertyFlatTreeGrid.getColumn("contextID").setDescriptionGenerator(e -> e.getContextDiv(), ContentMode.HTML);

		ButtonRenderer<TreeRowItem> selectItemsRenderer = new ButtonRenderer<TreeRowItem>(
				rendererClickEvent -> handleSelectClickEvent(rendererClickEvent));
		selectItemsRenderer.setHtmlContentAllowed(true);

		propertyFlatTreeGrid.addColumn(TreeRowItem::getPropertyName).setCaption("Property").setId("propNameID");
		propertyFlatTreeGrid.getColumn("propNameID").setWidth(90);
		propertyFlatTreeGrid.getColumn("propNameID").setDescriptionGenerator(e -> e.getPropertyName(), ContentMode.HTML);
		propertyFlatTreeGrid.addColumn(TreeRowItem::getPropertyValue).setCaption("Value").setId("propValueID");
		propertyFlatTreeGrid.getColumn("propValueID").setWidth(80);
		propertyFlatTreeGrid.getColumn("propValueID").setDescriptionGenerator(e -> e.getPropertyValue(), ContentMode.HTML);


		propertyFlatTreeGrid.addColumn(TreeRowItem::getSelectIcon, selectItemsRenderer).setCaption("select")
				.setId("selectIconID");
		propertyFlatTreeGrid.getColumn("selectIconID").setWidth(60);
		propertyFlatDataProvider.refreshAll();
		propertyFlatTreeGrid.setDataProvider(propertyFlatDataProvider);

		return propertyFlatTreeGrid;
	}

	
	private void handleRemoveClickEvent(RendererClickEvent<TreeRowItem> removeClickEvent) {
		TreeRowItem toRemove = removeClickEvent.getItem();
		TreeRowItem parent = selectedItemsTreeGridData.getParent(toRemove);
		List <TreeRowItem> itemsToRemove =collectChildrenRecursively(toRemove);
		ArrayList<QueryResultRow> compoundQueryResult=createQueryResultFromItemList(itemsToRemove);
		removeQueryResultsFromVisualisation(compoundQueryResult);

		// check on every level: if toRemove has no siblings delete parent too
		List<TreeRowItem> siblingsOne = selectedItemsTreeGridData.getChildren(parent);
		TreeRowItem parentParent = selectedItemsTreeGridData.getParent(parent);

		List<TreeRowItem> siblingsTwo = selectedItemsTreeGridData.getChildren(parentParent);
		TreeRowItem parentParentParent = selectedItemsTreeGridData.getParent(parentParent);

		List<TreeRowItem> siblingsThree = selectedItemsTreeGridData.getChildren(parentParentParent);
		TreeRowItem parentParentParentParent = selectedItemsTreeGridData.getParent(parentParentParent);

		List<TreeRowItem> siblingsFour = selectedItemsTreeGridData.getChildren(parentParentParentParent);

		if (siblingsOne.size() == 1) {
			if (siblingsTwo.size() == 1) {
				if (siblingsThree.size() == 1) {
					if (siblingsFour.size() == 1) {
						selectedItemsTreeGridData.removeItem(parentParentParentParent);

					} else {
						selectedItemsTreeGridData.removeItem(parentParentParent);
					}
				} else {
					selectedItemsTreeGridData.removeItem(parentParent);
				}
			} else {
				selectedItemsTreeGridData.removeItem(parent);
			}
		} else {
			selectedItemsTreeGridData.removeItem(toRemove);
		}
		selectedDataProvider.refreshAll();
	}
	
	
	private List<TreeRowItem> collectChildrenRecursively(TreeRowItem toRemove) {
		List<TreeRowItem> itemsToRemove = new ArrayList<TreeRowItem>();

		List<TreeRowItem> singleItemsToRemove = selectedItemsTreeGridData.getChildren(toRemove);
		if (singleItemsToRemove.isEmpty()) {
			itemsToRemove.add(toRemove);
		} else {
			for (TreeRowItem treeRowItem : singleItemsToRemove) {
				List<TreeRowItem> singleItemsToRemove1 = selectedItemsTreeGridData.getChildren(treeRowItem);
				if (singleItemsToRemove1.isEmpty()) {
					itemsToRemove.add(treeRowItem);
				} else {
					for (TreeRowItem treeRowItem2 : singleItemsToRemove1) {
						List<TreeRowItem> singleItemsToRemove2 = selectedItemsTreeGridData.getChildren(treeRowItem2);
						if (singleItemsToRemove2.isEmpty()) {
							itemsToRemove.add(treeRowItem2);
						} else {
							for (TreeRowItem treeRowItem3 : singleItemsToRemove2) {
								List<TreeRowItem> singleItemsToRemove3 = selectedItemsTreeGridData
										.getChildren(treeRowItem3);
								if (singleItemsToRemove3.isEmpty()) {
									itemsToRemove.add(treeRowItem3);
								}
								itemsToRemove.addAll(singleItemsToRemove3);
							}

						}

					}
				}
			}

		}
		return itemsToRemove;
		
	}
	
	
	
	private void handleExpandClickEventPhrase(ExpandEvent<TreeRowItem> expandClickEvent) {

		TreeRowItem clickedItem = expandClickEvent.getExpandedItem();
		TreeRowItem dummyItem = phraseDataProvider.getTreeData().getChildren(clickedItem).get(0);

		if (clickedItem instanceof DocumentItem && (dummyItem.getForward() == null)) {

			DocumentItem selectedItem = (DocumentItem) clickedItem;
			phraseDataProvider.getTreeData()
					.removeItem(phraseDataProvider.getTreeData().getChildren(selectedItem).get(0));
			
			QueryResultRowArray groupedChildren = selectedItem.getRows();
			String docID=groupedChildren.get(0).getSourceDocumentId();
	
			KwicProvider kwicProvider= null;
	
				try {
					kwicProvider = kwicProviderCache.get(docID);
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	
					
			ArrayList<TreeRowItem> children = createSingleItemRowsArrayList(selectedItem, 
					kwicProvider);
			phraseDataProvider.getTreeData().addItems(selectedItem, children);

		} else {
			// do nothing , item already expanded and dummy is replaced with real items
		}
		phraseDataProvider.refreshAll();

	}

	private void handleExpandClickEventTag(ExpandEvent<TreeRowItem> expandClickEvent) {

		TreeRowItem clickedItem = expandClickEvent.getExpandedItem();
		TreeRowItem dummyItem = tagDataProvider.getTreeData().getChildren(clickedItem).get(0);

		if (clickedItem instanceof CollectionItem && (dummyItem.getRows() == null)) {

			CollectionItem selectedItem = (CollectionItem) clickedItem;
			tagDataProvider.getTreeData().removeItem(dummyItem);
			
			
			QueryResultRowArray groupedChildren = selectedItem.getRows();
			String docID=groupedChildren.get(0).getSourceDocumentId();
	
			KwicProvider kwicProvider= null;
			try {
				kwicProvider = kwicProviderCache.get(docID);
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			ArrayList<TreeRowItem> children = createSingleItemRowsArrayList(selectedItem, kwicProvider);
			tagDataProvider.getTreeData().addItems(selectedItem, children);
		} else {
		}
		tagDataProvider.refreshAll();
	}

	private void handleExpandClickEventProperty(ExpandEvent<TreeRowItem> expandClickEvent) {
		TreeRowItem clickedItem = expandClickEvent.getExpandedItem();
		TreeRowItem dummyItem = propertyDataProvider.getTreeData().getChildren(clickedItem).get(0);

		if (clickedItem.getClass().equals(CollectionItem.class) && (dummyItem.getRows() == null)) {
			CollectionItem selectedItem = (CollectionItem) clickedItem;
			propertyDataProvider.getTreeData().removeItem(dummyItem);
			
			
			QueryResultRowArray groupedChildren = selectedItem.getRows();
			String docID=groupedChildren.get(0).getSourceDocumentId();

			KwicProvider kwicProvider= null;
			try {
				kwicProvider = kwicProviderCache.get(docID);
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			ArrayList<TreeRowItem> children = createSingleItemRowsArrayList(selectedItem,
					kwicProvider);
			propertyDataProvider.getTreeData().addItems(selectedItem, children);
		} else {
		}
		propertyDataProvider.refreshAll();
	}

	/**
	 * @param selectedItem
	 * @return
	 */
	private ArrayList<TreeRowItem> createSingleItemRowsArrayList(TreeRowItem selectedItem,
			KwicProvider kwicProvider) {
						
		ArrayList<TreeRowItem> children = new ArrayList<>();
		QueryResultRowArray groupedChildren = selectedItem.getRows();
		Iterator<QueryResultRow> resultIterator = groupedChildren.iterator();
	
		while (resultIterator.hasNext()) {
			QueryResultRow queryResultRow = (QueryResultRow) resultIterator.next();
			QueryResultRowArray itemAsQRRA = new QueryResultRowArray();
			itemAsQRRA.add(queryResultRow);

			SingleItem item = new SingleItem();
			if (queryResultRow.getClass().equals(TagQueryResultRow.class)) {
				TagQueryResultRow tQRR = (TagQueryResultRow) queryResultRow;
				item.setTreeKey(tQRR.getTagDefinitionPath());
				if (selectedGridViewID == ViewID.property) {
					SingleItem singleItem = (SingleItem) item;
					singleItem.setPropertyName(tQRR.getPropertyName());
					singleItem.setPropertyValue(tQRR.getPropertyValue());
					item = singleItem;
				}
			} else {
				item.setTreeKey(queryResultRow.getPhrase());
			}
			item.setRows(itemAsQRRA);
			item.setQuery(queryResultBox.getValue());
			SingleItem itemWithContext;
		
			try {
				itemWithContext = setContext(item, kwicProvider);
				if (!children.contains(itemWithContext)) {
					children.add((TreeRowItem) itemWithContext);
				}
			} catch (Exception e) {
		
				e.printStackTrace();
			}
		}
		return children;
	}

	private SingleItem setContext(SingleItem item, KwicProvider kwicProvider) throws Exception {
		
		QueryResultRow row = item.getQueryResultRowArray().get(0);	
		KeywordInContext kwic = kwicProvider.getKwic(row.getRange(), kwicSize);
		item.setBackward(kwic.getBackwardContext());
		item.setForward(kwic.getForwardContext());
		item.setPhrase(kwic.getKeyword());
		Range keyWordRange = kwic.getKeywordRange();
		int startPoint = keyWordRange.getStartPoint();
		if (startPoint == 0) {
			startPoint = 1;
		}
		
		int position = (100 * startPoint) / kwicProvider.getDocumentLength();
		item.setPosition(position);
		return item;
	}
	
	private TreeData<TreeRowItem> setContextToDataObject(TreeData<TreeRowItem> treeData) {
		List<TreeRowItem> allItems = treeData.getRootItems();

		allItems.forEach(e -> {

			KwicProvider kwicProvider = null;

			String docID = e.getRows().get(0).getSourceDocumentId();
			try {
				kwicProvider = kwicProviderCache.get(docID);
				setContext((SingleItem) e, kwicProvider);
			} catch (Exception e1) {
			
				e1.printStackTrace();
			}

		});
		return treeData;
	}
	 

	private void handleSelectClickEvent(RendererClickEvent<TreeRowItem> rendererClickEvent) {
		TreeRowItem selectedItem = rendererClickEvent.getItem();
		TreeDataProvider<TreeRowItem> currentTreeGridDataProvider = (TreeDataProvider<TreeRowItem>) resultsTreeGrid
				.getDataProvider();

		if (selectedGridViewID== ViewID.phrase) {
			addPhraseItemsToSelectedPanel(selectedItem);
		} else {
			
			if(selectedGridViewID== ViewID.flatTableProperty) {
				addPropertyAsFlatTableToSelectedPanel(selectedItem, currentTreeGridDataProvider);
			}else {
				addTagOrPropertyItemsToSelectedPanel(selectedItem, currentTreeGridDataProvider);
			}
		}
	}
	
	private void addPropertyAsFlatTableToSelectedPanel(TreeRowItem selectedItem,
			TreeDataProvider<TreeRowItem> currentTreeDataProvider) {

		Optional<String> currentQuery = queryResultBox.getSelectedItem();
		int length = currentQuery.toString().length();
		String queryString = currentQuery.toString().substring(20, length - 1);
		QueryRootItem root = new QueryRootItem();
		root.setTreeKey(queryString);

		if (selectedItemsTreeGridData.contains(root)) {
			if (selectedItemsTreeGridData.contains(selectedItem)) {
				//item already inside- do nothing
			} else {
				selectedItemsTreeGridData.addItem(root, selectedItem);
				ArrayList<TreeRowItem> items= new ArrayList<TreeRowItem>();
				items.add(selectedItem);
				addQueryResultsToVisualisation(createQueryResultFromItemList(items));

			}
		} else {
			selectedItemsTreeGridData.addItem(null, root);
			selectedItemsTreeGridData.addItem(root, selectedItem);
			ArrayList<TreeRowItem> items= new ArrayList<TreeRowItem>();
			items.add(selectedItem);
			addQueryResultsToVisualisation(createQueryResultFromItemList(items));

		}
		selectedDataProvider.refreshAll();
	}

	private void addTagOrPropertyItemsToSelectedPanel(TreeRowItem selectedItem,
			TreeDataProvider<TreeRowItem> currentTreeDataProvider) {
		try {
			// check if dummy is already removed
			TreeRowItem dummy = currentTreeDataProvider.getTreeData().getChildren(selectedItem).get(0);
			List<TreeRowItem> childrenLevelOne = currentTreeDataProvider.getTreeData().getChildren(selectedItem);

			if (selectedItem instanceof CollectionItem && dummy.getRows() == null) {
				replaceDummyWithTagItems(selectedItem, currentTreeDataProvider);

			}
			if (selectedItem instanceof  DocumentItem) {
				for (TreeRowItem collection : childrenLevelOne) {
					TreeRowItem dummy2 = currentTreeDataProvider.getTreeData().getChildren(collection).get(0);
					if (dummy2.getRows() == null) {
						replaceDummyWithTagItems(collection, currentTreeDataProvider);
					}
				}
			}
			if (selectedItem instanceof RootItem) {
				for (TreeRowItem document : childrenLevelOne) {
					List<TreeRowItem> collectionsPerDoc = currentTreeDataProvider.getTreeData().getChildren(document);
					for (TreeRowItem collection : collectionsPerDoc) {
						TreeRowItem dummy2 = currentTreeDataProvider.getTreeData().getChildren(collection).get(0);
						if (dummy2.getRows() == null) {
							replaceDummyWithTagItems(collection, currentTreeDataProvider);
						}
					}
				}
			}
		} catch (Exception e) {
			e.getMessage();
		}

		Collection<TreeRowItem> allRootItems = selectedItemsTreeGridData.getRootItems();
		Optional<String> currentQuery = queryResultBox.getSelectedItem();

		int length = currentQuery.toString().length();
		String queryString = currentQuery.toString().substring(20, length - 1);
		QueryRootItem queryRoot = new QueryRootItem();
		queryRoot.setTreeKey(queryString);

		if ((allRootItems.isEmpty()) || (!allRootItems.contains(queryRoot))) {
			selectedItemsTreeGridData.addItem(null, queryRoot);

			if (selectedItem instanceof RootItem) {
				selectedItemsTreeGridData.addItem(queryRoot, selectedItem);
				List<TreeRowItem> documents = resultsTreeGridData.getChildren(selectedItem);
				for (TreeRowItem doc : documents) {
					selectedItemsTreeGridData.addItem(selectedItem, doc);
					List<TreeRowItem> collections = resultsTreeGridData.getChildren(doc);
					for (TreeRowItem coll : collections) {
						selectedItemsTreeGridData.addItem(doc, coll);
						List<TreeRowItem> items = resultsTreeGridData.getChildren(coll);
						selectedItemsTreeGridData.addItems(coll, items);
						
						addQueryResultsToVisualisation(createQueryResultFromItemList(items));
					}
				}
			}
			if (selectedItem instanceof DocumentItem) {
				TreeRowItem root = resultsTreeGridData.getParent(selectedItem);
				selectedItemsTreeGridData.addItem(queryRoot, root);
				selectedItemsTreeGridData.addItem(root, selectedItem);
				List<TreeRowItem> collections = resultsTreeGridData.getChildren(selectedItem);
				for (TreeRowItem coll : collections) {
					selectedItemsTreeGridData.addItem(selectedItem, coll);
					List<TreeRowItem> items = resultsTreeGridData.getChildren(coll);
					selectedItemsTreeGridData.addItems(coll, items);
					
					addQueryResultsToVisualisation(createQueryResultFromItemList(items));
				}
			}
			if (selectedItem instanceof CollectionItem) {
				TreeRowItem doc = resultsTreeGridData.getParent(selectedItem);
				TreeRowItem root = resultsTreeGridData.getParent(doc);
				selectedItemsTreeGridData.addItem(queryRoot, root);
				selectedItemsTreeGridData.addItem(root, doc);
				selectedItemsTreeGridData.addItem(doc, selectedItem);
				List<TreeRowItem> items = resultsTreeGridData.getChildren(selectedItem);
				selectedItemsTreeGridData.addItems(selectedItem, items);
				
				addQueryResultsToVisualisation(createQueryResultFromItemList(items));

			}
			if (selectedItem instanceof SingleItem) {
				TreeRowItem collection = resultsTreeGridData.getParent(selectedItem);
				TreeRowItem document = resultsTreeGridData.getParent(collection);
				TreeRowItem root = resultsTreeGridData.getParent(document);
				selectedItemsTreeGridData.addItem(queryRoot, root);
				selectedItemsTreeGridData.addItem(root, document);
				selectedItemsTreeGridData.addItem(document, collection);
				selectedItemsTreeGridData.addItem(collection, selectedItem);
				
				List<TreeRowItem> items= new ArrayList<TreeRowItem>();
				items.add(selectedItem);
				addQueryResultsToVisualisation(createQueryResultFromItemList(items));

			}

		}

		else {

			if (allRootItems.contains(queryRoot)) {

				if (selectedItem instanceof RootItem) {
					// single items of that branch maybe already inside->update whole branch
					if (selectedItemsTreeGridData.contains(selectedItem)) {
						selectedItemsTreeGridData.removeItem(selectedItem);
						selectedItemsTreeGridData.addItem(queryRoot, selectedItem);

						List<TreeRowItem> documents = resultsTreeGridData.getChildren(selectedItem);
						selectedItemsTreeGridData.addItems(selectedItem, documents);

						for (TreeRowItem doc : documents) {
							List<TreeRowItem> collections = resultsTreeGridData.getChildren(doc);
							for (TreeRowItem coll : collections) {
								selectedItemsTreeGridData.addItem(doc, coll);
								List<TreeRowItem> items = resultsTreeGridData.getChildren(coll);
								selectedItemsTreeGridData.addItems(coll, items);
								
								addQueryResultsToVisualisation(createQueryResultFromItemList(items));
							}
						}

					} else {
						// add whole new branch to tree
						selectedItemsTreeGridData.addItem(queryRoot, selectedItem);

						List<TreeRowItem> documents = resultsTreeGridData.getChildren(selectedItem);
						selectedItemsTreeGridData.addItems(selectedItem, documents);

						for (TreeRowItem doc : documents) {
							List<TreeRowItem> collections = resultsTreeGridData.getChildren(doc);
							for (TreeRowItem coll : collections) {
								selectedItemsTreeGridData.addItem(doc, coll);
								List<TreeRowItem> items = resultsTreeGridData.getChildren(coll);
								selectedItemsTreeGridData.addItems(coll, items);
								
								addQueryResultsToVisualisation(createQueryResultFromItemList(items));
							}

						}

					}
				}
				// update branch on doc level
				if (selectedItem instanceof DocumentItem) {
					// single items of that doc-branch maybe already inside->update whole doc_branch
					if (selectedItemsTreeGridData.contains(selectedItem)) {
						TreeRowItem rootTag = resultsTreeGridData.getParent(selectedItem);

						selectedItemsTreeGridData.removeItem(selectedItem);
						selectedItemsTreeGridData.addItem(rootTag, selectedItem);

						List<TreeRowItem> colls = resultsTreeGridData.getChildren(selectedItem);
						selectedItemsTreeGridData.addItems(selectedItem, colls);

						for (TreeRowItem coll : colls) {
							List<TreeRowItem> items = resultsTreeGridData.getChildren(coll);
							selectedItemsTreeGridData.addItems(coll, items);
							
							addQueryResultsToVisualisation(createQueryResultFromItemList(items));
						}
					} else {
						// add whole new doc_branch to tree
						TreeRowItem rootTag = resultsTreeGridData.getParent(selectedItem);
						if(!selectedItemsTreeGridData.contains(rootTag)) {
							selectedItemsTreeGridData.addItem(queryRoot,rootTag);
							
						}
							
						selectedItemsTreeGridData.addItem(rootTag, selectedItem);
						List<TreeRowItem> colls = resultsTreeGridData.getChildren(selectedItem);
						selectedItemsTreeGridData.addItems(selectedItem, colls);

						for (TreeRowItem coll : colls) {
							List<TreeRowItem> items = resultsTreeGridData.getChildren(coll);
							selectedItemsTreeGridData.addItems(coll, items);
							
							addQueryResultsToVisualisation(createQueryResultFromItemList(items));
						}
					}

				}
				// update tree-branch on collection level
				if (selectedItem  instanceof CollectionItem) {
				TreeRowItem parentDocument=	resultsTreeGridData.getParent(selectedItem);
					// with new document as parent for that collection
					if(!selectedItemsTreeGridData.contains(parentDocument)) {
						TreeRowItem tagItem=resultsTreeGridData.getParent(parentDocument);
						
						if(!selectedItemsTreeGridData.contains(tagItem)) {
							selectedItemsTreeGridData.addItem(queryRoot,tagItem);	
						}
						selectedItemsTreeGridData.addItem(tagItem, parentDocument);
						selectedItemsTreeGridData.addItem( parentDocument,selectedItem);	
						List<TreeRowItem> items = resultsTreeGridData.getChildren(selectedItem);
						selectedItemsTreeGridData.addItems(selectedItem, items);
						
						addQueryResultsToVisualisation(createQueryResultFromItemList(items));
					}
					// single items of that collection-branch maybe already inside->update whole
					// collection_branch
					else {
						if (selectedItemsTreeGridData.contains(selectedItem)) {
							List<TreeRowItem> items = resultsTreeGridData.getChildren(selectedItem);
							selectedItemsTreeGridData.removeItem(selectedItem);
							selectedItemsTreeGridData.addItems(selectedItem, items);
							
							addQueryResultsToVisualisation(createQueryResultFromItemList(items));

						} else {
							// add whole new collection branch to tree

							TreeRowItem doc = resultsTreeGridData.getParent(selectedItem);
							TreeRowItem rootTag = resultsTreeGridData.getParent(doc);
							List<TreeRowItem> items = resultsTreeGridData.getChildren(selectedItem);
							if (selectedItemsTreeGridData.contains(doc)) {
								selectedItemsTreeGridData.addItem(doc, selectedItem);
								selectedItemsTreeGridData.addItems(selectedItem, items);
								
								addQueryResultsToVisualisation(createQueryResultFromItemList(items));
							} else {
								selectedItemsTreeGridData.addItem(queryRoot, rootTag);
								selectedItemsTreeGridData.addItem(rootTag, doc);
								selectedItemsTreeGridData.addItem(doc, selectedItem);
								selectedItemsTreeGridData.addItems(selectedItem, items);
								
								addQueryResultsToVisualisation(createQueryResultFromItemList(items));
							}
						}
						
					}
			
				}
				// update branch on singleItem level
				if (selectedItem instanceof SingleItem) {
					// single item already inside, do nothing
					if (selectedItemsTreeGridData.contains(selectedItem)) {
						// do nothing, item already inside
					} else {
						// item not inside -> check which hierarchy level already inside

						TreeRowItem coll = resultsTreeGridData.getParent(selectedItem);
						TreeRowItem doc = resultsTreeGridData.getParent(coll);
						TreeRowItem rootTag = resultsTreeGridData.getParent(doc);

						if (selectedItemsTreeGridData.contains(coll)) {
							selectedItemsTreeGridData.addItem(coll, selectedItem);
							
							List<TreeRowItem> items= new ArrayList<TreeRowItem>();
							items.add(selectedItem);
							addQueryResultsToVisualisation(createQueryResultFromItemList(items));
						} else {
							if (selectedItemsTreeGridData.contains(doc)) {
								selectedItemsTreeGridData.addItem(doc, coll);
								selectedItemsTreeGridData.addItem(coll, selectedItem);
								
								List<TreeRowItem> items= new ArrayList<TreeRowItem>();
								items.add(selectedItem);
								addQueryResultsToVisualisation(createQueryResultFromItemList(items));
							} else {
								if (selectedItemsTreeGridData.contains(rootTag)) {
									selectedItemsTreeGridData.addItem(rootTag, doc);
									selectedItemsTreeGridData.addItem(doc, coll);
									selectedItemsTreeGridData.addItem(coll, selectedItem);
									
									List<TreeRowItem> items= new ArrayList<TreeRowItem>();
									items.add(selectedItem);
									addQueryResultsToVisualisation(createQueryResultFromItemList(items));
								} else { // tagRoot is not yet inside
									selectedItemsTreeGridData.addItem(queryRoot, rootTag);
									selectedItemsTreeGridData.addItem(rootTag, doc);
									selectedItemsTreeGridData.addItem(doc, coll);
									selectedItemsTreeGridData.addItem(coll, selectedItem);
									
									List<TreeRowItem> items= new ArrayList<TreeRowItem>();
									items.add(selectedItem);
									addQueryResultsToVisualisation(createQueryResultFromItemList(items));

								}
							}
						}
					}
				}

			}

		}
		selectedDataProvider.refreshAll();
	}

	private void addPhraseItemsToSelectedPanel(TreeRowItem selectedItem) {
		try {
	
			TreeRowItem dummy = phraseDataProvider.getTreeData().getChildren(selectedItem).get(0);
			List<TreeRowItem> childrenLevelOne = phraseDataProvider.getTreeData().getChildren(selectedItem);
							
			//item is on declevel then no deeper iteration and no cache needed
			
			if ((selectedItem instanceof DocumentItem) && dummy.getRows() == null) {
				QueryResultRowArray groupedChildren = selectedItem.getRows();
				String docID=groupedChildren.get(0).getSourceDocumentId();
				KwicProvider kwicProvider= null;
				kwicProvider = this.kwicProviderCache.get(docID);		
				replaceDummyWithPhraseItems(selectedItem,phraseDataProvider,kwicProvider);

			}
			
			// items on phrase level -> iteration on child(=document) level needed
			
			if (selectedItem instanceof RootItem) { // instance of
				for (TreeRowItem documentItem : childrenLevelOne) {
					TreeRowItem dummy2 = phraseDataProvider.getTreeData().getChildren(documentItem).get(0);
					if (dummy2.getRows() == null) { 
						String sdID=documentItem.getRows().get(0).getSourceDocumentId();
						KwicProvider kwicProvider =null;					
						kwicProvider=	(KwicProvider) this.kwicProviderCache.get(sdID);					
						replaceDummyWithPhraseItems(documentItem, phraseDataProvider,kwicProvider);

					}else {
						// do nothing, dummy already replaced by real items
					}

				}
			}

		} catch (Exception e) {
			e.getMessage(); //TODO
		}
		
		
		Collection<TreeRowItem> allRootItems = selectedItemsTreeGridData.getRootItems();
		Optional<String> currentQuery = queryResultBox.getSelectedItem();

		int length = currentQuery.toString().length();
		String queryString = currentQuery.toString().substring(20, length - 1);
		QueryRootItem queryRoot = new QueryRootItem();
		queryRoot.setTreeKey(queryString);

		if (allRootItems.isEmpty()) {
			selectedItemsTreeGridData.addItem(null, queryRoot);

			if (selectedItem.getClass().equals(DocumentItem.class)) {
				TreeRowItem rootPhrase = resultsTreeGridData.getParent(selectedItem);
				selectedItemsTreeGridData.addItem(queryRoot, rootPhrase);
				selectedItemsTreeGridData.addItem(rootPhrase, selectedItem);
				List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(selectedItem);
				selectedItemsTreeGridData.addItems(selectedItem, singleItems);
				
				addQueryResultsToVisualisation(createQueryResultFromItemList(singleItems));

			}
			if (selectedItem.getClass().equals(RootItem.class)) {
				List<TreeRowItem> documents = resultsTreeGridData.getChildren(selectedItem);
				selectedItemsTreeGridData.addItem(queryRoot, selectedItem);
				selectedItemsTreeGridData.addItems(selectedItem, documents);
				for (TreeRowItem doc : documents) {
					List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(doc);
					selectedItemsTreeGridData.addItems(doc, singleItems);
					
					addQueryResultsToVisualisation(createQueryResultFromItemList(singleItems));

				}
			}
			if (selectedItem.getClass().equals(SingleItem.class)) {
				TreeRowItem document = resultsTreeGridData.getParent(selectedItem);
				TreeRowItem phrase = resultsTreeGridData.getParent(document);
				selectedItemsTreeGridData.addItem(queryRoot, phrase);
				selectedItemsTreeGridData.addItems(phrase, document);
				selectedItemsTreeGridData.addItems(document, selectedItem);
				
				ArrayList<TreeRowItem> itemList = new ArrayList<TreeRowItem>();
						
				itemList.add(selectedItem);
					
				addQueryResultsToVisualisation(createQueryResultFromItemList(itemList));
			}

		} else {
			if (!allRootItems.contains(queryRoot)) {
				selectedItemsTreeGridData.addItem(null, queryRoot);

				if (selectedItem.getClass().equals(DocumentItem.class)) {
					TreeRowItem root = resultsTreeGridData.getParent(selectedItem);
					selectedItemsTreeGridData.addItem(queryRoot, root);
					selectedItemsTreeGridData.addItem(root, selectedItem);
					
					List<TreeRowItem> singleItems=resultsTreeGridData.getChildren(selectedItem);
					addQueryResultsToVisualisation(createQueryResultFromItemList(singleItems));
					
					
				}
				if (selectedItem.getClass().equals(RootItem.class)) {
					List<TreeRowItem> childrenDocs = resultsTreeGridData.getChildren(selectedItem);
					selectedItemsTreeGridData.addItem(queryRoot, selectedItem);
					selectedItemsTreeGridData.addItems(selectedItem, childrenDocs);

					for (TreeRowItem doc : childrenDocs) {
						List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(doc);
						selectedItemsTreeGridData.addItems(doc, singleItems);
						
						addQueryResultsToVisualisation(createQueryResultFromItemList(singleItems));
									

					}

				}
				if (selectedItem.getClass().equals(SingleItem.class)) {
					TreeRowItem document = resultsTreeGridData.getParent(selectedItem);
					TreeRowItem phrase = resultsTreeGridData.getParent(document);
					selectedItemsTreeGridData.addItem(queryRoot, phrase);
					selectedItemsTreeGridData.addItems(phrase, document);
					selectedItemsTreeGridData.addItems(document, selectedItem);
					
					
					ArrayList<TreeRowItem> itemList = new ArrayList<TreeRowItem>();
					
					itemList.add(selectedItem);
						
					addQueryResultsToVisualisation(createQueryResultFromItemList(itemList));
				}
			} else {
				if (allRootItems.contains(queryRoot)) {

					if (selectedItem.getClass().equals(DocumentItem.class)) {

						if (selectedItemsTreeGridData.contains(selectedItem)) {
							TreeRowItem phraseRoot = resultsTreeGridData.getParent(selectedItem);
							selectedItemsTreeGridData.removeItem(selectedItem);
							selectedItemsTreeGridData.addItem(phraseRoot, selectedItem);

							List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(selectedItem);
							selectedItemsTreeGridData.addItems(selectedItem, singleItems);
							
							addQueryResultsToVisualisation(createQueryResultFromItemList(singleItems));

						} else {
							TreeRowItem phraseRoot = resultsTreeGridData.getParent(selectedItem);
							if (selectedItemsTreeGridData.contains(phraseRoot)) {
								// phrase already inside
								selectedItemsTreeGridData.addItem(phraseRoot, selectedItem);
								List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(selectedItem);
								selectedItemsTreeGridData.addItems(selectedItem, singleItems);
								
								addQueryResultsToVisualisation(createQueryResultFromItemList(singleItems));
							} else {
								// phrase not inside
								selectedItemsTreeGridData.addItem(queryRoot, phraseRoot);
								selectedItemsTreeGridData.addItem(phraseRoot, selectedItem);
								List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(selectedItem);
								selectedItemsTreeGridData.addItems(selectedItem, singleItems);
								
								addQueryResultsToVisualisation(createQueryResultFromItemList(singleItems));

							}

						}

					}
					if (selectedItem.getClass().equals(RootItem.class)) {

						if (selectedItemsTreeGridData.contains(selectedItem)) {
							List<TreeRowItem> documents = resultsTreeGridData.getChildren(selectedItem);
							selectedItemsTreeGridData.removeItem(selectedItem);
							selectedItemsTreeGridData.addItem(queryRoot, selectedItem);
							selectedItemsTreeGridData.addItems(selectedItem, documents);

							for (TreeRowItem oneDoc : documents) {
								List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(oneDoc);
								selectedItemsTreeGridData.addItems(oneDoc, singleItems);
								
								addQueryResultsToVisualisation(createQueryResultFromItemList(singleItems));
							}
						} else {
							List<TreeRowItem> documents = resultsTreeGridData.getChildren(selectedItem);
							selectedItemsTreeGridData.addItem(queryRoot, selectedItem);
							selectedItemsTreeGridData.addItems(selectedItem, documents);

							for (TreeRowItem oneDoc : documents) {
								List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(oneDoc);
								selectedItemsTreeGridData.addItems(oneDoc, singleItems);
								
								addQueryResultsToVisualisation(createQueryResultFromItemList(singleItems));
							}

						}
					}

					if (selectedItem.getClass().equals(SingleItem.class)) {

						TreeRowItem document = resultsTreeGridData.getParent(selectedItem);
						TreeRowItem phrase = resultsTreeGridData.getParent(document);

						if (selectedItemsTreeGridData.contains(phrase)) {
							// doc already inside -> check if item inside and if not insert
							if (selectedItemsTreeGridData.contains(document)) {

								if (!selectedItemsTreeGridData.getChildren(document).contains(selectedItem)) {
									selectedItemsTreeGridData.addItem(document, selectedItem);
									
									
									ArrayList<TreeRowItem> itemList = new ArrayList<TreeRowItem>();
									
									itemList.add(selectedItem);
										
									addQueryResultsToVisualisation(createQueryResultFromItemList(itemList));
								} else {
									// do nothing because item already inside
								}
							} else {
								selectedItemsTreeGridData.addItem(phrase, document);
								selectedItemsTreeGridData.addItem(document, selectedItem);
								
								ArrayList<TreeRowItem> itemList = new ArrayList<TreeRowItem>();
								
								itemList.add(selectedItem);
									
								addQueryResultsToVisualisation(createQueryResultFromItemList(itemList));
							}
						} else {
							// insert new phrase and new document before inserting the single item
							selectedItemsTreeGridData.addItem(queryRoot, phrase);
							selectedItemsTreeGridData.addItem(phrase, document);
							selectedItemsTreeGridData.addItem(document, selectedItem);
							
							ArrayList<TreeRowItem> itemList = new ArrayList<TreeRowItem>();
							
							itemList.add(selectedItem);
								
							addQueryResultsToVisualisation(createQueryResultFromItemList(itemList));
						}

					}
				}
			}
		}
		selectedDataProvider.refreshAll();

	}

	private void replaceDummyWithTagItems(TreeRowItem selectedItem,
			TreeDataProvider<TreeRowItem> tagDataProvider) {
		
		QueryResultRowArray groupedChildren = selectedItem.getRows();
		String docID=groupedChildren.get(0).getSourceDocumentId();
		KwicProvider kwicProvider= null;
		try {
			kwicProvider = kwicProviderCache.get(docID);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<TreeRowItem> children = createSingleItemRowsArrayList(selectedItem, kwicProvider);
		TreeRowItem dummy = tagDataProvider.getTreeData().getChildren(selectedItem).get(0);
		tagDataProvider.getTreeData().removeItem(dummy);
		tagDataProvider.getTreeData().addItems(selectedItem, children);
	}

	private void replaceDummyWithPhraseItems(TreeRowItem selectedItem,
			TreeDataProvider<TreeRowItem> phraseDataProvider2, KwicProvider kwicProvider) {
		QueryResultRowArray groupedChildren = selectedItem.getRows();
		
		List<TreeRowItem> children = createSingleItemRowsArrayList(selectedItem, kwicProvider);

		TreeRowItem dummy = phraseDataProvider2.getTreeData().getChildren(selectedItem).get(0);
		if (selectedItem.getClass() == DocumentItem.class) {
			phraseDataProvider2.getTreeData().removeItem(dummy);
			phraseDataProvider2.getTreeData().addItems(selectedItem, children);

		} else {
			
			List<TreeRowItem> docList = phraseDataProvider2.getTreeData().getChildren(selectedItem);
			
	
			
			for (TreeRowItem doc : docList) {
				TreeRowItem dummy2 = phraseDataProvider2.getTreeData().getChildren(doc).get(0);
				String sdID=groupedChildren.get(0).getSourceDocumentId();
				KwicProvider kwicProvider1 =null;
										
				try {
					kwicProvider1 = kwicProviderCache.get(sdID);
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}														
				
				List<TreeRowItem> children2 = createSingleItemRowsArrayList(doc, kwicProvider1);
				phraseDataProvider2.getTreeData().removeItem(dummy2);
				phraseDataProvider2.getTreeData().addItems(doc, children2);
			}
		}
	}
	
	
	 private ArrayList<QueryResultRow> createQueryResultFromItemList(List<TreeRowItem> selectedItems){
			ArrayList<QueryResultRow> queryResult = new ArrayList<QueryResultRow>();
			
			for (TreeRowItem item : selectedItems) {
				QueryResultRow qrr=item.getRows().get(0);
				queryResult.add(qrr);
				
			}
			return queryResult;
	 }
	
	/*
	 * private ArrayList<QueryResultRow> createQueryResultFromTreeGridData() {
	 * ArrayList<QueryResultRow> queryResult = new ArrayList<QueryResultRow>();
	 * List<TreeRowItem> rootElements =
	 * selectedDataProvider.getTreeData().getRootItems(); if
	 * (!rootElements.isEmpty()) { for (TreeRowItem root : rootElements) { //
	 * List<TreeRowItem> children = new ArrayList<TreeRowItem>(); children =
	 * selectedItemsTreeGridData.getChildren(root); if (!children.isEmpty()) { for
	 * (TreeRowItem child : children) {
	 * 
	 * List<TreeRowItem> childrenTwo = selectedItemsTreeGridData.getChildren(child);
	 * if(childrenTwo.isEmpty()) { // flat table items QueryResultRowArray qrrArray=
	 * child.getRows(); QueryResultRow qrr = qrrArray.get(0); queryResult.add(qrr);
	 * 
	 * }else { for (TreeRowItem treeRowItem : childrenTwo) { List<TreeRowItem>
	 * childrenThree = selectedItemsTreeGridData.getChildren(treeRowItem);
	 * 
	 * for (TreeRowItem treeRowItem2 : childrenThree) { QueryResultRowArray
	 * queryResultRowArray = new QueryResultRowArray();
	 * 
	 * List<TreeRowItem> childrenFour =
	 * selectedItemsTreeGridData.getChildren(treeRowItem2);
	 * 
	 * if (childrenFour.isEmpty()) { QueryResultRow result =
	 * treeRowItem2.getRows().get(0); queryResultRowArray.add(result);
	 * 
	 * } else { for (TreeRowItem treeRowItem4 : childrenFour) { QueryResultRow
	 * result = treeRowItem4.getRows().get(0); queryResultRowArray.add(result);
	 * 
	 * }
	 * 
	 * } queryResult.addAll(queryResultRowArray); } }
	 * 
	 * }
	 * 
	 * 
	 * }
	 * 
	 * } } } else { } return queryResult; }
	 */

}
