package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;

import de.catma.document.repository.Repository;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.TagQueryResult;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.tagger.annotationpanel.AnnotationTreeItem;

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
	private TreeData<TagRowItem> resultsTreeGridData;
	private TreeGrid<TagRowItem> resultsTreeGrid;
	//private Grid<TagRowItem>selectedItemsGrid;
	private TreeData<TagRowItem> selectedItemsTreeGridData;
	private TreeGrid<TagRowItem>selectedItemsTreeGrid;
	private TreeDataProvider<TagRowItem> selectedDataProvider;
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
		selectedItemsTreeGrid = new TreeGrid<TagRowItem>();
		selectedItemsTreeGrid.addColumn(TagRowItem::getShortenTreePath).setCaption("tag");
		selectedItemsTreeGrid.addColumn(TagRowItem::getPhrase).setCaption("phrase");
		selectedItemsTreeGrid.addColumn(TagRowItem::getPropertyName).setCaption("property");
	
		selectedItemsTreeGridData= new 	TreeData<TagRowItem>();
		selectedDataProvider = new TreeDataProvider<>(selectedItemsTreeGridData);
		selectedItemsTreeGrid.setDataProvider(selectedDataProvider);
		selectedItemsTreeGrid.setWidth("100%");
		//selectedItemsGrid.addColumn(TagRowItem::getPhrase).setCaption("Phrase");
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
		
		selectedDataProvider.addDataProviderListener(new DataProviderListener<TagRowItem>() {
			
			@Override
			public void onDataChange(DataChangeEvent<TagRowItem> event) {
			//	if(comboBox.getValue().contains("tag")) {
				
					updateKwicView();
					
					System.out.println("TreeGrid hat sich geändert");	
				//}
			}
		});
		
		
		

	/*
	
	setDescription(
		    "<h2>"+
		    	    "A richtext tooltip</h2>"+
		    	    "<ul>"+
		    	    "  <li>Use rich formatting with HTML</li>"+
		    	    "  <li>Include images from themes</li>"+
		    	    "  <li>etc.</li>"+
		    	    "</ul>",ContentMode.HTML);
	*/
		
		
		
		
	}
	
	private void updateKwicView() {
		//QueryResult queryResult = createQueryResultFromTreeGridDataTags();
		ArrayList<QueryResultRow> queryResult = createQueryResultFromTreeGridData();
		
		try {
			
		//	kwicNew.addQueryResultRows(queryResult);
			
		} catch (Exception e) {		
			e.printStackTrace();
		}
	}
	
	
	
	
	private QueryResult createQueryResultFromTreeGridDataTags() {
		
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
	
	private ArrayList<QueryResultRow> createQueryResultFromTreeGridData() {
		
		ArrayList<QueryResultRow> queryResult = new ArrayList<QueryResultRow>();

		List<TagRowItem> rootElements = selectedDataProvider.getTreeData().getRootItems();

		if (!rootElements.isEmpty()) {
			for (TagRowItem root : rootElements) {
				List<TagRowItem> children= new ArrayList<TagRowItem>();
			   children	= selectedItemsTreeGridData.getChildren(root);
				if(!children.isEmpty()) {
					for (TagRowItem child : children) 		{
						QueryResultRow queryResultRow = child.getQueryResultRow();
						queryResult.add(queryResultRow);
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
		resultsTreeGridData = new TreeData<TagRowItem>();
		selectedGridView = null;
		while (allResultsIterator.hasNext()) {
			CurrentTreeGridData currentData = allResultsIterator.next();
			if (currentData.getQuery().equalsIgnoreCase(queryAsString)) {
				resultsTreeGridData = currentData.getCurrentTreeData();
				selectedGridView = currentData.getViewID();
			}
		}
		resultsTreeGrid = createResultsTreeGridFromData(resultsTreeGridData, selectedGridView);
		resultsTreeGrid.setSelectionMode(SelectionMode.MULTI);
		resultsTreeGrid.addStyleNames(
				"annotation-details-panel-annotation-details-grid", 
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");

		resultsTreeGrid.setWidth("100%");
		resultsTreeGrid.setHeight("230px");
		

		resultsTreeGrid.addSelectionListener(new SelectionListener<TagRowItem>() {
			@Override
			public void selectionChange(SelectionEvent<TagRowItem> event) {
				
				Iterable<TagRowItem> selectedItems = event.getAllSelectedItems();
				
		/*		selectedItems.forEach(item -> {
					System.out.println(" TgaPath :" + item.getTreePath() + " Collection :" + item.getCollectionName()
							+ " Tag ID:" + item.getTagInstanceID());
				});*/
				
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
		});
		
		// add selected items to the slected-panel
	     resultsTreeGrid.addSelectionListener(new SelectionListener<TagRowItem>() {
				
				@Override
				public void selectionChange(SelectionEvent<TagRowItem> event) {
					
				Optional<String> currentQuery = comboBox.getSelectedItem();
								
				Collection<TagRowItem> allRootItems = selectedItemsTreeGridData.getRootItems();	
				Iterable<TagRowItem> selectedItems=	event.getAllSelectedItems();
				
				addItemsToSelectedPanel(currentQuery.toString(), (Collection<TagRowItem>) selectedItems, allRootItems);
				
				}
			});
		queryResultsPanel.setContent(resultsTreeGrid);
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

	
	private TreeGrid<TagRowItem> createResultsTreeGridFromData(TreeData<TagRowItem> treeData, ViewID currentView) {
		TreeGrid<TagRowItem> resultSetTreeGrid = new TreeGrid<>();
		switch (currentView) {
		case tag:
			resultSetTreeGrid = addDataTagStyle(treeData);
			break;
		case property:
			resultSetTreeGrid = addDataPropertyStyle(treeData);
			break;
		case phrase:
			resultSetTreeGrid = addDataPhraseStyle(treeData);
			break;
		case phraseProperty:
			resultSetTreeGrid = addDataPhraseStyle(treeData);
			break;
		case phraseTag:
			resultSetTreeGrid = addDataPhraseStyle(treeData);
			break;
		}
		return resultSetTreeGrid;
	}

	
	private TreeGrid<TagRowItem> addDataTagStyle(TreeData<TagRowItem> treeData) {
		TreeGrid<TagRowItem> tagTreeGrid = new TreeGrid<>();
		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(treeData);
		tagTreeGrid.addColumn(TagRowItem::getShortenTreePath).setCaption("Tag").setId("tagID");
		tagTreeGrid.getColumn("tagID").setExpandRatio(7);
		tagTreeGrid.getColumn("tagID").setDescriptionGenerator(e-> e.getTreePath() , ContentMode.HTML);
		tagTreeGrid.addColumn(TagRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		tagTreeGrid.getColumn("freqID").setExpandRatio(1);
		dataProvider.refreshAll();
		tagTreeGrid.setDataProvider(dataProvider);
		tagTreeGrid.setWidth("100%");
	
		return tagTreeGrid;
	}

	
	private TreeGrid<TagRowItem> addDataPhraseStyle(TreeData<TagRowItem> treeData) {
		TreeGrid<TagRowItem> phraseTreeGrid = new TreeGrid<>();
		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(treeData);
		phraseTreeGrid.addColumn(TagRowItem::getShortenTreePath).setCaption("Phrase").setId("phraseID");
		phraseTreeGrid.getColumn("phraseID").setExpandRatio(7);
		phraseTreeGrid.addColumn(TagRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		phraseTreeGrid.getColumn("freqID").setExpandRatio(1);
		dataProvider.refreshAll();
		phraseTreeGrid.setDataProvider(dataProvider);
		return phraseTreeGrid;
	}

	
	private TreeGrid<TagRowItem> addDataPropertyStyle(TreeData<TagRowItem> treeData) {
		TreeGrid<TagRowItem> propertyTreeGrid = new TreeGrid<>();
		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(treeData);
		propertyTreeGrid.addColumn(TagRowItem::getShortenTreePath).setCaption("Tag").setId("tagID");
		propertyTreeGrid.getColumn("tagID").setExpandRatio(3);
		propertyTreeGrid.addColumn(TagRowItem::getPropertyName).setCaption("Property name").setId("propNameID");
		propertyTreeGrid.getColumn("propNameID").setExpandRatio(3);
		propertyTreeGrid.addColumn(TagRowItem::getPropertyValue).setCaption("Property value").setId("propValueID");
		propertyTreeGrid.getColumn("propValueID").setExpandRatio(3);
		propertyTreeGrid.addColumn(TagRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		propertyTreeGrid.getColumn("freqID").setExpandRatio(1);
		dataProvider.refreshAll();
		propertyTreeGrid.setDataProvider(dataProvider);
		propertyTreeGrid.setWidth("100%");
		return propertyTreeGrid;
	}

	private void addItemsToSelectedPanel(String query, Collection<TagRowItem> items,Collection<TagRowItem> allRootItemsIncoming) {

		
		TagRowItem newRoot = new TagRowItem();
		
		int length=query.length();
		String queryString = query.substring(20, length-1);
		
		newRoot.setTreePath(queryString);
		
		Collection<TagRowItem> allRootItems= allRootItemsIncoming;
		Iterator<TagRowItem> rootsIterator =allRootItems.iterator();
		
		
		if (items.isEmpty()) {
			
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
			selectedItemsTreeGridData.addItems(null, newRoot);
			
			if(selectedGridView==ViewID.phrase) {
				selectedItemsTreeGridData=	sortIncomingRowItemsToPhraseTreeData(newRoot,selectedItemsTreeGridData, items);
			}
			
			if(selectedGridView==ViewID.tag) {
				selectedItemsTreeGridData=	sortIncomingRowItemsToTagTreeData(newRoot,selectedItemsTreeGridData, items);
			}
			//selectedItemsTreeGridData.addItems(newRoot, items);
			selectedDataProvider.refreshAll();
		} else {
					
			for (TagRowItem rootItem : allRootItems) {
				// rootItem exists, therefore renew root+branch
				if (rootItem.getTreePath().equalsIgnoreCase(newRoot.getTreePath())) {
					contains=true;		
					selectedItemsTreeGridData.removeItem(rootItem);
					selectedDataProvider.refreshAll();
					selectedItemsTreeGridData.addItem(null, newRoot);
			
					
					if(selectedGridView==ViewID.phrase) {
						selectedItemsTreeGridData=	sortIncomingRowItemsToPhraseTreeData(newRoot,selectedItemsTreeGridData, items);
					}					
					if(selectedGridView==ViewID.tag) {
						selectedItemsTreeGridData=	sortIncomingRowItemsToTagTreeData(newRoot,selectedItemsTreeGridData, items);
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
						selectedItemsTreeGridData=	sortIncomingRowItemsToPhraseTreeData(newRoot,selectedItemsTreeGridData, items);
					}
					
					if(selectedGridView==ViewID.tag) {
						selectedItemsTreeGridData=	sortIncomingRowItemsToTagTreeData(newRoot,selectedItemsTreeGridData, items);
					}
					//selectedItemsTreeGridData.addItems(newRoot, items);
					selectedItemsTreeGrid.setWidth("100%");
					selectedDataProvider.refreshAll();
				}
			}
		}
		
	
	
		}
	
	private TreeData<TagRowItem> sortIncomingRowItemsToPhraseTreeData(TagRowItem root, TreeData<TagRowItem> treeData,
			Collection<TagRowItem> items) {

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
		treeData.addItems(root, allDocs);
		

		for (TagRowItem doc : allDocs) {
			String sourceDoc = doc.getSourceDocName();
			for (TagRowItem item : allItems) {
				if (item.getSourceDocName().equalsIgnoreCase(sourceDoc)) {
					treeData.addItem(doc, item);
				}
			}
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

		ActionGridComponent<TreeGrid<TagRowItem>> selectedGridComponent = 
				new ActionGridComponent<>(new Label("Selected resultrows for the kwic visualization"), selectedItemsTreeGrid);
		leftSide.addComponent(selectedGridComponent);
	}
	

	@Override
	public void setQueryResults() {
		// TODO Auto-generated method stub

	}

}
