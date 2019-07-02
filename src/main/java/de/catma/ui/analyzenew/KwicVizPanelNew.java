package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.DataChangeEvent;
import com.vaadin.data.provider.DataProviderListener;
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
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.themes.ValoTheme;

import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeGrid;
//import com.vaadin.ui.VerticalLayout;
//import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;

import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.source.KeywordInContext;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.analyzenew.treehelper.CollectionItem;
import de.catma.ui.analyzenew.treehelper.DocumentItem;
import de.catma.ui.analyzenew.treehelper.QueryRootItem;
import de.catma.ui.analyzenew.treehelper.RootItem;
import de.catma.ui.analyzenew.treehelper.SingleItem;
import de.catma.ui.analyzenew.treehelper.TreeRowItem;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

public class KwicVizPanelNew extends VerticalLayout implements VizPanel {

	private Repository repository;
	private HorizontalLayout headerButtonBar;
	private HorizontalSplitPanel mainContentSplitPanel;
	private Panel selectedItemsPanel;
	private CloseVizViewListener leaveViewListener;
	private Label visualisationName;
	private Button optionsBt;
	private Button arrowLeftBt;
	private ComboBox<String> comboBox;
	private List<String> availableResultSets;
	private VerticalLayout frameLayout;
	private VerticalLayout leftSide;
	private Panel queryResultsPanel;
	private Panel rightSide;
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

	public KwicVizPanelNew(ArrayList<CurrentTreeGridData> currentTreeGridDatas, Repository repository) {
		this.currentTreeGridDatas = currentTreeGridDatas;

		this.repository = repository;
		initComponents();
		initActions();
		initListeners();

	}

	public KwicVizPanelNew(CloseVizViewListener leaveVizListener, ArrayList<CurrentTreeGridData> currentTreeGridDatas,
			Repository repository) {
		this.currentTreeGridDatas = currentTreeGridDatas;
		this.leaveViewListener = leaveVizListener;
		this.repository = repository;
		initComponents();
		initActions();
		initListeners();
	}

	public CloseVizViewListener getLeaveViewListener() {
		return leaveViewListener;
	}

	public void setLeaveViewListener(CloseVizViewListener leaveViewListener) {
		this.leaveViewListener = leaveViewListener;
	}

	private void initComponents() {
		frameLayout = new VerticalLayout();
		leftSide = new VerticalLayout();
		leftSide.addStyleName("analyzer_kwic_leftside_vertical");
		rightSide = new Panel();
		rightSide.addStyleName("analyze_kwic_right");
		rightSide.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		kwicNew = new KwicPanelNew(repository);
		kwicNew.setHeight("100%");
		kwicNew.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		rightSide.setContent(kwicNew);
		rightSide.setHeight("100%");
		headerButtonBar = new HorizontalLayout();

		arrowLeftBt = new Button(VaadinIcons.ARROW_LEFT);
		arrowLeftBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		visualisationName = new Label("KWIC Visualisation");
		visualisationName.addStyleName("analyze_kwic_header_label");

		optionsBt = new Button(VaadinIcons.ELLIPSIS_V);
		optionsBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		optionsBt.addStyleName("analyze_kwic_header_button");

		headerButtonBar.addComponents(arrowLeftBt, visualisationName, optionsBt);
		headerButtonBar.addStyleName("analyze_kwic_buttonBar");
		frameLayout.addComponent(headerButtonBar);
		mainContentSplitPanel = new HorizontalSplitPanel();
		mainContentSplitPanel.setSplitPosition(40, Sizeable.UNITS_PERCENTAGE);
		mainContentSplitPanel.addStyleName("analyzer_kwic_splitpanel");

		frameLayout.addComponent(mainContentSplitPanel);

		addComponent(frameLayout);
		addStyleName("analyze_tab");
		frameLayout.setSizeFull();

		comboBox = new ComboBox<String>();
		comboBox.setWidth("100%");
		comboBox.setPlaceholder("Select one resultset");
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
		resultsTreeGrid = new TreeGrid<>();

		selectedItemsTreeGrid = new TreeGrid<TreeRowItem>();
		selectedItemsTreeGrid.setWidth(570, UNITS_PIXELS);

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

		selectedItemsPanel = new Panel();
		selectedItemsPanel.setCaption("selected items for the kwic visualization");

		selectedItemsPanel.setWidth("100%");
		selectedItemsPanel.setHeight("200px");

		selectedItemsPanel.setContent(selectedItemsTreeGrid);
		leftSide.addComponent(comboBox);
		leftSide.addComponent(queryResultsPanel);
		setActionGridComponenet();

		mainContentSplitPanel.addComponents(leftSide, rightSide);
	}

	private void initActions() {
		arrowLeftBt.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				leaveViewListener.onClose();
				new VizSnapshot("KWIC Snapshot");
			}
		});
	}

	private void initListeners() {
		selectedDataProvider.addDataProviderListener(new DataProviderListener<TreeRowItem>() {
			@Override
			public void onDataChange(DataChangeEvent<TreeRowItem> event) {
				updateKwicView();
			}
		});
	}

	private void updateKwicView() {
		ArrayList<QueryResultRow> queryResult = createQueryResultFromTreeGridData();
		try {
			kwicNew.addQueryResultRows(queryResult);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		queryResultsPanel.setContent(resultsTreeGrid);			
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
		phraseTreeGrid.setWidth(570, UNITS_PIXELS);
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
		tagTreeGrid.setWidth(570, UNITS_PIXELS);
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
		propertyTreeGrid.setWidth(570, UNITS_PIXELS);
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
		propertyFlatTreeGrid.setWidth(570, UNITS_PIXELS);
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

	private void handleExpandClickEventPhrase(ExpandEvent<TreeRowItem> expandClickEvent) {

		TreeRowItem clickedItem = expandClickEvent.getExpandedItem();
		TreeRowItem dummyItem = phraseDataProvider.getTreeData().getChildren(clickedItem).get(0);

		if (clickedItem.getClass().equals(DocumentItem.class) && (dummyItem.getForward() == null)) {

			DocumentItem selectedItem = (DocumentItem) clickedItem;
			phraseDataProvider.getTreeData()
					.removeItem(phraseDataProvider.getTreeData().getChildren(selectedItem).get(0));
			ArrayList<TreeRowItem> children = createSingleItemRowsArrayList(selectedItem);
			phraseDataProvider.getTreeData().addItems(selectedItem, children);

		} else {

		}
		phraseDataProvider.refreshAll();

	}

	private void handleExpandClickEventTag(ExpandEvent<TreeRowItem> expandClickEvent) {

		TreeRowItem clickedItem = expandClickEvent.getExpandedItem();
		TreeRowItem dummyItem = tagDataProvider.getTreeData().getChildren(clickedItem).get(0);

		if (clickedItem.getClass().equals(CollectionItem.class) && (dummyItem.getRows() == null)) {

			CollectionItem selectedItem = (CollectionItem) clickedItem;
			tagDataProvider.getTreeData().removeItem(dummyItem);
			ArrayList<TreeRowItem> children = createSingleItemRowsArrayList(selectedItem);
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
			ArrayList<TreeRowItem> children = createSingleItemRowsArrayList(selectedItem);
			propertyDataProvider.getTreeData().addItems(selectedItem, children);
		} else {
		}
		propertyDataProvider.refreshAll();
	}

	private ArrayList<TreeRowItem> createSingleItemRowsArrayList(TreeRowItem selectedItem) {
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
			item.setQuery(comboBox.getValue());
			SingleItem itemWithContext;
			try {
				itemWithContext = setContext(item);
				if (!children.contains(itemWithContext)) {
					children.add((TreeRowItem) itemWithContext);
				}
			} catch (Exception e) {
		
				e.printStackTrace();
			}
		}
		return children;
	}

	private SingleItem setContext(SingleItem item) throws Exception {
		QueryResultRow row = item.getQueryResultRowArray().get(0);
		SourceDocument sourceDocument = repository.getSourceDocument(row.getSourceDocumentId());

		KwicProvider kwicProvider = new KwicProvider(sourceDocument);
		KeywordInContext kwic = kwicProvider.getKwic(row.getRange(), kwicSize);
		item.setBackward(kwic.getBackwardContext());
		item.setForward(kwic.getForwardContext());
		item.setPhrase(kwic.getKeyword());
		Range keyWordRange = kwic.getKeywordRange();
		int startPoint = keyWordRange.getStartPoint();
		if (startPoint == 0) {
			startPoint = 1;
		}
		int doclength = sourceDocument.getLength();
		int position = (100 * startPoint) / doclength;
		item.setPosition(position);
		return item;
	}
	 private TreeData<TreeRowItem> setContextToDataObject(TreeData<TreeRowItem> treeData){
		List<TreeRowItem> allItems=  treeData.getRootItems();

		allItems.forEach(e->{
			try {
				setContext((SingleItem) e);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
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
				addPropertyAsFlatTable(selectedItem, currentTreeGridDataProvider);
			}else {
				addTagOrPropertyItemsToSelectedPanel(selectedItem, currentTreeGridDataProvider);
			}
		}
	}
	
	private void addPropertyAsFlatTable(TreeRowItem selectedItem,
			TreeDataProvider<TreeRowItem> currentTreeDataProvider) {

		Optional<String> currentQuery = comboBox.getSelectedItem();
		int length = currentQuery.toString().length();
		String queryString = currentQuery.toString().substring(20, length - 1);
		QueryRootItem root = new QueryRootItem();
		root.setTreeKey(queryString);

		if (selectedItemsTreeGridData.contains(root)) {
			if (selectedItemsTreeGridData.contains(selectedItem)) {
				//item already inside- do nothing
			} else {
				selectedItemsTreeGridData.addItem(root, selectedItem);
			}
		} else {
			selectedItemsTreeGridData.addItem(null, root);
			selectedItemsTreeGridData.addItem(root, selectedItem);
		}
		selectedDataProvider.refreshAll();
	}

	private void addTagOrPropertyItemsToSelectedPanel(TreeRowItem selectedItem,
			TreeDataProvider<TreeRowItem> currentTreeDataProvider) {
		try {
			// check if dummy is already removed
			TreeRowItem dummy = currentTreeDataProvider.getTreeData().getChildren(selectedItem).get(0);
			List<TreeRowItem> childrenLevelOne = currentTreeDataProvider.getTreeData().getChildren(selectedItem);

			if (selectedItem.getClass() == CollectionItem.class && dummy.getRows() == null) {
				replaceDummyWithTagItems(selectedItem, currentTreeDataProvider);

			}
			if (selectedItem.getClass() == DocumentItem.class) {
				for (TreeRowItem collection : childrenLevelOne) {
					TreeRowItem dummy2 = currentTreeDataProvider.getTreeData().getChildren(collection).get(0);
					if (dummy2.getRows() == null) {
						replaceDummyWithTagItems(collection, currentTreeDataProvider);
					}
				}
			}
			if (selectedItem.getClass() == RootItem.class) {
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
		Optional<String> currentQuery = comboBox.getSelectedItem();

		int length = currentQuery.toString().length();
		String queryString = currentQuery.toString().substring(20, length - 1);
		QueryRootItem queryRoot = new QueryRootItem();
		queryRoot.setTreeKey(queryString);

		if ((allRootItems.isEmpty()) || (!allRootItems.contains(queryRoot))) {
			selectedItemsTreeGridData.addItem(null, queryRoot);

			if (selectedItem.getClass().equals(RootItem.class)) {
				selectedItemsTreeGridData.addItem(queryRoot, selectedItem);
				List<TreeRowItem> documents = resultsTreeGridData.getChildren(selectedItem);
				for (TreeRowItem doc : documents) {
					selectedItemsTreeGridData.addItem(selectedItem, doc);
					List<TreeRowItem> collections = resultsTreeGridData.getChildren(doc);
					for (TreeRowItem coll : collections) {
						selectedItemsTreeGridData.addItem(doc, coll);
						List<TreeRowItem> items = resultsTreeGridData.getChildren(coll);
						selectedItemsTreeGridData.addItems(coll, items);
					}
				}
			}
			if (selectedItem.getClass().equals(DocumentItem.class)) {
				TreeRowItem root = resultsTreeGridData.getParent(selectedItem);
				selectedItemsTreeGridData.addItem(queryRoot, root);
				selectedItemsTreeGridData.addItem(root, selectedItem);
				List<TreeRowItem> collections = resultsTreeGridData.getChildren(selectedItem);
				for (TreeRowItem coll : collections) {
					selectedItemsTreeGridData.addItem(selectedItem, coll);
					List<TreeRowItem> items = resultsTreeGridData.getChildren(coll);
					selectedItemsTreeGridData.addItems(coll, items);
				}
			}
			if (selectedItem.getClass().equals(CollectionItem.class)) {
				TreeRowItem doc = resultsTreeGridData.getParent(selectedItem);
				TreeRowItem root = resultsTreeGridData.getParent(doc);
				selectedItemsTreeGridData.addItem(queryRoot, root);
				selectedItemsTreeGridData.addItem(root, doc);
				selectedItemsTreeGridData.addItem(doc, selectedItem);
				List<TreeRowItem> items = resultsTreeGridData.getChildren(selectedItem);
				selectedItemsTreeGridData.addItems(selectedItem, items);

			}
			if (selectedItem.getClass().equals(SingleItem.class)) {
				TreeRowItem collection = resultsTreeGridData.getParent(selectedItem);
				TreeRowItem document = resultsTreeGridData.getParent(collection);
				TreeRowItem root = resultsTreeGridData.getParent(document);
				selectedItemsTreeGridData.addItem(queryRoot, root);
				selectedItemsTreeGridData.addItem(root, document);
				selectedItemsTreeGridData.addItem(document, collection);
				selectedItemsTreeGridData.addItem(collection, selectedItem);

			}

		}

		else {

			if (allRootItems.contains(queryRoot)) {

				if (selectedItem.getClass().equals(RootItem.class)) {
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
							}

						}

					}
				}
				// update branch on doc level
				if (selectedItem.getClass().equals(DocumentItem.class)) {
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
						}
					} else {
						// add whole new doc_branch to tree
						TreeRowItem rootTag = resultsTreeGridData.getParent(selectedItem);
						selectedItemsTreeGridData.addItem(queryRoot, rootTag);
						selectedItemsTreeGridData.addItem(rootTag, selectedItem);

						List<TreeRowItem> colls = resultsTreeGridData.getChildren(selectedItem);
						selectedItemsTreeGridData.addItems(selectedItem, colls);

						for (TreeRowItem coll : colls) {
							List<TreeRowItem> items = resultsTreeGridData.getChildren(coll);
							selectedItemsTreeGridData.addItems(coll, items);
						}
					}

				}
				// updatetree-branch on collection level
				if (selectedItem.getClass().equals(CollectionItem.class)) {
					// single items of that collection-branch maybe already inside->update whole
					// collection_branch

					if (selectedItemsTreeGridData.contains(selectedItem)) {
						List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(selectedItem);
						TreeRowItem parent = selectedItemsTreeGridData.getParent(selectedItem);
						selectedItemsTreeGridData.removeItem(selectedItem);
						selectedItemsTreeGridData.addItem(parent, selectedItem);
						// selectedItemsTreeGridData.getChildren(selectedItem).clear();
						selectedItemsTreeGridData.addItems(selectedItem, singleItems);

					} else {
						// add whole new collection branch to tree

						TreeRowItem doc = resultsTreeGridData.getParent(selectedItem);
						TreeRowItem rootTag = resultsTreeGridData.getParent(doc);
						List<TreeRowItem> items = resultsTreeGridData.getChildren(selectedItem);
						if (selectedItemsTreeGridData.contains(doc)) {
							selectedItemsTreeGridData.addItem(doc, selectedItem);
							selectedItemsTreeGridData.addItems(selectedItem, items);
						} else {
							selectedItemsTreeGridData.addItem(queryRoot, rootTag);
							selectedItemsTreeGridData.addItem(rootTag, doc);
							selectedItemsTreeGridData.addItem(doc, selectedItem);
							selectedItemsTreeGridData.addItems(selectedItem, items);
						}
					}
				}
				// update branch on singleItem level
				if (selectedItem.getClass().equals(SingleItem.class)) {
					// single item already inside, do nothing
					if (selectedItemsTreeGridData.contains(selectedItem)) {

					} else {
						// item not inside-> check which hierarchy level already inside

						TreeRowItem coll = resultsTreeGridData.getParent(selectedItem);
						TreeRowItem doc = resultsTreeGridData.getParent(coll);
						TreeRowItem rootTag = resultsTreeGridData.getParent(doc);

						if (selectedItemsTreeGridData.contains(coll)) {
							selectedItemsTreeGridData.addItem(coll, selectedItem);
						} else {
							if (selectedItemsTreeGridData.contains(doc)) {
								selectedItemsTreeGridData.addItem(doc, coll);
								selectedItemsTreeGridData.addItem(coll, selectedItem);
							} else {
								if (selectedItemsTreeGridData.contains(rootTag)) {
									selectedItemsTreeGridData.addItem(rootTag, doc);
									selectedItemsTreeGridData.addItem(doc, coll);
									selectedItemsTreeGridData.addItem(coll, selectedItem);
								} else { // tagRoot is not yet inside
									selectedItemsTreeGridData.addItem(queryRoot, rootTag);
									selectedItemsTreeGridData.addItem(rootTag, doc);
									selectedItemsTreeGridData.addItem(doc, coll);
									selectedItemsTreeGridData.addItem(coll, selectedItem);

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
			// check if dummy is already removed
			TreeRowItem dummy = phraseDataProvider.getTreeData().getChildren(selectedItem).get(0);
			List<TreeRowItem> childrenLevelOne = phraseDataProvider.getTreeData().getChildren(selectedItem);

			if (selectedItem.getClass() == DocumentItem.class && dummy.getRows() == null) {
				replaceDummyWithPhraseItems(selectedItem, phraseDataProvider);

			}
			if (selectedItem.getClass() == RootItem.class) {
				for (TreeRowItem treeRowItem : childrenLevelOne) {
					TreeRowItem dummy2 = phraseDataProvider.getTreeData().getChildren(treeRowItem).get(0);
					if (dummy2.getRows() == null) {
						replaceDummyWithPhraseItems(selectedItem, phraseDataProvider);

					}

				}

			}

		} catch (Exception e) {
			e.getMessage();
		}

		Collection<TreeRowItem> allRootItems = selectedItemsTreeGridData.getRootItems();
		Optional<String> currentQuery = comboBox.getSelectedItem();

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

			}
			if (selectedItem.getClass().equals(RootItem.class)) {
				List<TreeRowItem> documents = resultsTreeGridData.getChildren(selectedItem);
				selectedItemsTreeGridData.addItem(queryRoot, selectedItem);
				selectedItemsTreeGridData.addItems(selectedItem, documents);
				for (TreeRowItem doc : documents) {
					List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(doc);
					selectedItemsTreeGridData.addItems(doc, singleItems);

				}
			}
			if (selectedItem.getClass().equals(SingleItem.class)) {
				TreeRowItem document = resultsTreeGridData.getParent(selectedItem);
				TreeRowItem phrase = resultsTreeGridData.getParent(document);
				selectedItemsTreeGridData.addItem(queryRoot, phrase);
				selectedItemsTreeGridData.addItems(phrase, document);
				selectedItemsTreeGridData.addItems(document, selectedItem);
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
					List<TreeRowItem> childrenDocs = resultsTreeGridData.getChildren(selectedItem);
					selectedItemsTreeGridData.addItem(queryRoot, selectedItem);
					selectedItemsTreeGridData.addItems(selectedItem, childrenDocs);

					for (TreeRowItem doc : childrenDocs) {
						List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(doc);
						selectedItemsTreeGridData.addItems(doc, singleItems);

					}

				}
				if (selectedItem.getClass().equals(SingleItem.class)) {
					TreeRowItem document = resultsTreeGridData.getParent(selectedItem);
					TreeRowItem phrase = resultsTreeGridData.getParent(document);
					selectedItemsTreeGridData.addItem(queryRoot, phrase);
					selectedItemsTreeGridData.addItems(phrase, document);
					selectedItemsTreeGridData.addItems(document, selectedItem);
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

						} else {
							TreeRowItem phraseRoot = resultsTreeGridData.getParent(selectedItem);
							if (selectedItemsTreeGridData.contains(phraseRoot)) {
								// phrase already inside
								selectedItemsTreeGridData.addItem(phraseRoot, selectedItem);
								List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(selectedItem);
								selectedItemsTreeGridData.addItems(selectedItem, singleItems);
							} else {
								// phrase not inside
								selectedItemsTreeGridData.addItem(queryRoot, phraseRoot);
								selectedItemsTreeGridData.addItem(phraseRoot, selectedItem);
								List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(selectedItem);
								selectedItemsTreeGridData.addItems(selectedItem, singleItems);

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
							}
						} else {
							List<TreeRowItem> documents = resultsTreeGridData.getChildren(selectedItem);
							selectedItemsTreeGridData.addItem(queryRoot, selectedItem);
							selectedItemsTreeGridData.addItems(selectedItem, documents);

							for (TreeRowItem oneDoc : documents) {
								List<TreeRowItem> singleItems = resultsTreeGridData.getChildren(oneDoc);
								selectedItemsTreeGridData.addItems(oneDoc, singleItems);
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
								} else {
									// do nothing because item already inside
								}
							} else {
								selectedItemsTreeGridData.addItem(phrase, document);
								selectedItemsTreeGridData.addItem(document, selectedItem);
							}
						} else {
							// insert new phrase and new document before inserting the singleitem
							selectedItemsTreeGridData.addItem(queryRoot, phrase);
							selectedItemsTreeGridData.addItem(phrase, document);
							selectedItemsTreeGridData.addItem(document, selectedItem);
						}

					}
				}
			}
		}
		selectedDataProvider.refreshAll();

	}

	private void replaceDummyWithTagItems(TreeRowItem selectedItem,
			TreeDataProvider<TreeRowItem> tagDataProvider) {

		List<TreeRowItem> children = createSingleItemRowsArrayList(selectedItem);
		TreeRowItem dummy = tagDataProvider.getTreeData().getChildren(selectedItem).get(0);
		tagDataProvider.getTreeData().removeItem(dummy);
		tagDataProvider.getTreeData().addItems(selectedItem, children);
	}

	private void replaceDummyWithPhraseItems(TreeRowItem selectedItem,
			TreeDataProvider<TreeRowItem> phraseDataProvider2) {
		List<TreeRowItem> children = createSingleItemRowsArrayList(selectedItem);

		TreeRowItem dummy = phraseDataProvider2.getTreeData().getChildren(selectedItem).get(0);
		if (selectedItem.getClass() == DocumentItem.class) {
			phraseDataProvider2.getTreeData().removeItem(dummy);
			phraseDataProvider2.getTreeData().addItems(selectedItem, children);

		} else {

			List<TreeRowItem> docList = phraseDataProvider2.getTreeData().getChildren(selectedItem);
			for (TreeRowItem doc : docList) {

				TreeRowItem dummy2 = phraseDataProvider2.getTreeData().getChildren(doc).get(0);
				// TreeRowItem parent= phraseDataProvider2.getTreeData().getParent(treeRowItem);
				List<TreeRowItem> children2 = createSingleItemRowsArrayList(doc);
				phraseDataProvider2.getTreeData().removeItem(dummy2);
				phraseDataProvider2.getTreeData().addItems(doc, children2);
			}
		}
	}

	private ArrayList<QueryResultRow> createQueryResultFromTreeGridData() {
		ArrayList<QueryResultRow> queryResult = new ArrayList<QueryResultRow>();
		List<TreeRowItem> rootElements = selectedDataProvider.getTreeData().getRootItems();
		if (!rootElements.isEmpty()) {
			for (TreeRowItem root : rootElements) {
				//
				List<TreeRowItem> children = new ArrayList<TreeRowItem>();
				children = selectedItemsTreeGridData.getChildren(root);
				if (!children.isEmpty()) { 
					for (TreeRowItem child : children) {
						
						List<TreeRowItem> childrenTwo = selectedItemsTreeGridData.getChildren(child);
						if(childrenTwo.isEmpty()) { // flat table items
							QueryResultRowArray qrrArray= child.getRows();
							QueryResultRow qrr = qrrArray.get(0);
							queryResult.add(qrr);
							
						}else {
							for (TreeRowItem treeRowItem : childrenTwo) {
								List<TreeRowItem> childrenThree = selectedItemsTreeGridData.getChildren(treeRowItem);

								for (TreeRowItem treeRowItem2 : childrenThree) {
									QueryResultRowArray queryResultRowArray = new QueryResultRowArray();

									List<TreeRowItem> childrenFour = selectedItemsTreeGridData.getChildren(treeRowItem2);

									if (childrenFour.isEmpty()) {
										QueryResultRow result = treeRowItem2.getRows().get(0);
										queryResultRowArray.add(result);

									} else {
										for (TreeRowItem treeRowItem4 : childrenFour) {
											QueryResultRow result = treeRowItem4.getRows().get(0);
											queryResultRowArray.add(result);

										}

									}
									queryResult.addAll(queryResultRowArray);
								}
							}
							
						}
				

					}

				}
			}
		} else {
		}
		return queryResult;
	}

	private void setActionGridComponenet() {

		selectedItemsTreeGrid.addStyleNames("annotation-details-panel-annotation-details-grid",
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");

		ActionGridComponent<TreeGrid<TreeRowItem>> selectedGridComponent = new ActionGridComponent<>(
				new Label("Selected resultrows for the kwic visualization"), selectedItemsTreeGrid);
		leftSide.addComponent(selectedGridComponent);
		
	}

	@Override
	public void setQueryResults() {
		// TODO Auto-generated method stub

	}

}
