package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.event.selection.SingleSelectionListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

public class KwicVizPanelNew extends HorizontalLayout implements VizPanel {

	private VerticalLayout leftSide;

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
	private HorizontalLayout mainContentSplitPanel;
	private Panel rightSide;
	private ArrayList<CurrentTreeGridData> currentTreeGridDatas;
	private TreeData<TagRowItem> selectedTreeGridData;
	private TreeGrid<TagRowItem> selectedTreeGrid;

	public KwicVizPanelNew(CloseVizViewListener leaveVizListener, ArrayList<CurrentTreeGridData> currentTreeGridDatas) {
		this.currentTreeGridDatas = currentTreeGridDatas;
		this.leaveViewListener = leaveVizListener;
		initComponents();
		initActions();
		initListeners();
	}

	private void initComponents() {
		leftSide = new VerticalLayout();
		rightSide = new Panel("Visualisation");
		header = new Panel();
		arrowLeft = new Button("<");
		header.setContent(arrowLeft);
		leftSide.addComponent(header);
		mainContentSplitPanel = new HorizontalLayout();
		mainContentSplitPanel.addComponents(leftSide, rightSide);
		addComponent(mainContentSplitPanel);
		mainContentSplitPanel.setHeight("100%");
		mainContentSplitPanel.setWidth("100%");
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
		leftSide.addComponent(comboBox);
		leftSide.addComponent(treeGridPanelKwic);
	}

	
	private void initActions() {
		arrowLeft.addClickListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				leaveViewListener.onClose();

			}
		});

	}

	private void initListeners() {
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
		selectedTreeGridData = new TreeData<TagRowItem>();
		ViewID selectedGridView = null;
		while (allResultsIterator.hasNext()) {
			CurrentTreeGridData currentData = allResultsIterator.next();
			if (currentData.getQuery().equalsIgnoreCase(queryAsString)) {
				selectedTreeGridData = currentData.getCurrentTreeData();
				selectedGridView = currentData.getViewID();
			}
		}
		selectedTreeGrid = createTreeGridFromData(selectedTreeGridData, selectedGridView);
		selectedTreeGrid.setSelectionMode(SelectionMode.MULTI);
		selectedTreeGrid.setWidth("100%");
		selectedTreeGrid.setHeight("100%");
		selectedTreeGrid.addSelectionListener(new SelectionListener<TagRowItem>() {
			@Override
			public void selectionChange(SelectionEvent<TagRowItem> event) {
				Iterable<TagRowItem> selectedItems = event.getAllSelectedItems();
				selectedItems.forEach(item -> {
					System.out.println(" TgaPath :" + item.getTreePath() + " Collection :" + item.getCollectionName()
							+ " Tag ID:" + item.getTagInstanceID());
				});
				for (TagRowItem item : selectedItems) {
					TagRowItem parent = selectedTreeGridData.getParent(item);
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
		});
		treeGridPanelKwic.setContent(selectedTreeGrid);
	}

	
	private void checkIfAllSiblingsAreSelectedAndSelectParent(TagRowItem item) {
		boolean allChildrenSelected = true;
		TagRowItem parent = selectedTreeGridData.getParent(item);
		List<TagRowItem> children = selectedTreeGridData.getChildren(item);
		List<TagRowItem> siblings = selectedTreeGridData.getChildren(parent);

		/*
		 * if (siblings.size() == 1) { treeGridTag.asMultiSelect().select(parent); }
		 * else {
		 */

		for (TagRowItem sibl : siblings) {
			if (selectedTreeGrid.asMultiSelect().isSelected(sibl)) {
				// allChildrenSelected remains true
			} else {
				allChildrenSelected = false;
			}
		}
		if (allChildrenSelected) {
			selectedTreeGrid.asMultiSelect().select(parent);

		} else {
			selectedTreeGrid.asMultiSelect().deselect(parent);
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

	
	@Override
	public void setQueryResults() {
		// TODO Auto-generated method stub

	}

}
