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
import com.vaadin.ui.ComponentContainer;
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
import de.catma.ui.analyzenew.queryresultpanel.QueryResultRowItem;
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
		
		public QueryResultPanelSetting getSetting() {
			return setting;
		}
	}
	
	interface LeaveListener {
		public void onLeave(VizMaxPanel vizMaxPanel);
	}

	private LoadingCache<String, KwicProvider> kwicProviderCache;

	private HorizontalSplitPanel mainContentSplitPanel;
	private Button arrowLeftBt;
	private ComboBox<QuerySelection> queryResultBox;
	
	private TreeData<TreeRowItem> selectedItemsTreeGridData;
	private TreeGrid<TreeRowItem> selectedItemsTreeGrid;
	private TreeDataProvider<TreeRowItem> selectedDataProvider;
	
	private KwicPanelNew kwicNew;
	private ViewID selectedGridViewID;
	private int kwicSize = 5;
	
	private QueryResultPanel currentQueryResultPanel;

	private VerticalLayout topLeftPanel;

	private Repository project;

	public VizMaxPanel( 
			List<QueryResultPanelSetting> queryResultPanelSettings, Repository project,
			LoadingCache<String, KwicProvider> kwicProviderCache, LeaveListener leaveListener) {
		this.project = project;
		this.kwicProviderCache = kwicProviderCache;
		initComponents();
		initActions(leaveListener);
		initData(queryResultPanelSettings);
		
	}
	
	private void setQueryResultPanel(QuerySelection querySelection) {
		if (currentQueryResultPanel != null) {
			((ComponentContainer)currentQueryResultPanel.getParent()).removeComponent(currentQueryResultPanel);
		}
		
		if (querySelection.getPanel() == null) {
			QueryResultPanel queryResultPanel = 
					new QueryResultPanel(
						project, 
						querySelection.getSetting().getQueryResult(), 
						querySelection.getSetting().getQuery(), 
						kwicProviderCache,
						querySelection.getSetting().getDisplaySetting(),
						item -> handleItemSelection(item));
			queryResultPanel.setSizeFull();
			
			querySelection.setPanel(queryResultPanel);
		}

		topLeftPanel.addComponent(querySelection.getPanel());
		topLeftPanel.setExpandRatio(querySelection.getPanel(), 1f);
	}

	private void handleItemSelection(QueryResultRowItem item) {
		// TODO Auto-generated method stub
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
		
		topLeftPanel = new VerticalLayout();
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
		queryResultBox.setEmptySelectionAllowed(false);
		
		queryResultBox.setItemCaptionGenerator(querySelection -> querySelection.getSetting().getQuery());
		
		buttonAndBoxPanel.addComponent(queryResultBox);
		buttonAndBoxPanel.setExpandRatio(queryResultBox, 1f);
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
				new Label("Selected resultrows"), selectedItemsTreeGrid);

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
		queryResultBox.addSelectionListener(new SingleSelectionListener<QuerySelection>() {
			@Override
			public void selectionChange(SingleSelectionEvent<QuerySelection> event) {
				setQueryResultPanel(event.getValue());
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
//			item.setQuery(queryResultBox.getValue());
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
	 

	private void handleSelectClickEvent(RendererClickEvent<QueryResultRowItem> rendererClickEvent) {
//		TreeRowItem selectedItem = rendererClickEvent.getItem();
//		TreeDataProvider<TreeRowItem> currentTreeGridDataProvider = (TreeDataProvider<TreeRowItem>) resultsTreeGrid
//				.getDataProvider();
//
//		if (selectedGridViewID== ViewID.phrase) {
//			addPhraseItemsToSelectedPanel(selectedItem);
//		} else {
//			
//			if(selectedGridViewID== ViewID.flatTableProperty) {
//				addPropertyAsFlatTableToSelectedPanel(selectedItem, currentTreeGridDataProvider);
//			}else {
//				addTagOrPropertyItemsToSelectedPanel(selectedItem, currentTreeGridDataProvider);
//			}
//		}
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
	
}
