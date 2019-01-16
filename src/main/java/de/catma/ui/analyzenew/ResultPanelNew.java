package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.ExpandEvent;
import com.vaadin.event.ExpandEvent.ExpandListener;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.event.selection.SingleSelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.components.grid.ItemClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.themes.ValoTheme;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.ui.analyzer.AnalyzerView.CloseListener;

public class ResultPanelNew extends Panel   {

	private static enum TreePropertyName {
		caption, frequency, visibleInKwic,;
	}
	private static enum ViewID {
		phrase, tag, property,phraseTag,phraseProperty;
	}
	public static interface ResultPanelCloseListener{
		public void closeRequest(ResultPanelNew resultPanelNew);
	}

	private VerticalLayout contentVerticalLayout;
	
	private TreeData<TagRowItem> tagData;
	private TreeGrid<TagRowItem> treeGridTag;
	
	private TreeGrid<TagRowItem> treeGridPhrase;
	private TreeGrid<TagRowItem> treeGridProperty;
	
	
    private TreeGrid<TagRowItem> treeGridPhraseLazy;
    private	TreeDataProvider<TagRowItem> dataProviderLazy;
    private TreeData<TagRowItem> lazyData;
    
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


	public ResultPanelNew(Repository repository, QueryResult result, String queryAsString, ResultPanelCloseListener resultPanelCloseListener) throws Exception {

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
			setDataPhraseStyleLazy();
			//setDataPhraseStyle();
			setCurrentView(ViewID.phrase);
			//treeGridPanel.setContent(treeGridPhrase);
			treeGridPanel.setContent(treeGridPhraseLazy);
		}

	}

	public Component getCurrentTreeGrid() {
	return treeGridPanel.getContent();
	}

	private void setCurrentView(ViewID currentView) {
		this.currentView = currentView;
	}

	private void initComponents() {
		contentVerticalLayout = new VerticalLayout();
	
		
		setContent(contentVerticalLayout);

		treeGridTag = new TreeGrid<TagRowItem>();
		treeGridTag.setSelectionMode(SelectionMode.MULTI);
		
		
		
		treeGridPhrase = new TreeGrid<TagRowItem>();
		//treeGridPhrase.setSelectionMode(SelectionMode.MULTI);


		treeGridProperty = new TreeGrid<TagRowItem>();
		//treeGridProperty.setSelectionMode(SelectionMode.MULTI);
		
		
		treeGridPhraseLazy = new TreeGrid<TagRowItem>();
		//treeGridPhraseLazy.setSelectionMode(SelectionMode.MULTI);
		lazyData= new TreeData<TagRowItem>();	
		
		
		

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
		
		
		
		// we dont select items here anymore, just as test for fullview now
				treeGridTag.addSelectionListener(new SelectionListener<TagRowItem>() {
					
					@Override
					public void selectionChange(SelectionEvent<TagRowItem> event) {
						
					
					Iterable<TagRowItem> selectedItems=	event.getAllSelectedItems();
			
					
					selectedItems.forEach(item -> { System.out.println(" TgaPath :"+item.getTreePath()+ 
							" Collection :"+item.getCollectionName()+" Tag ID:"+item.getTagInstanceID());});	
					
					
				for (TagRowItem item: selectedItems) {
					TagRowItem      parent  = tagData.getParent(item);
					//can have siblings 
					if(parent!=null) {
						checkIfAllSiblingsAreSelectedAndSelectParent(item);
						//setChildrenSelected(item);
					
					//is root= no siblings	
					}else {
					//setChildrenSelected(item);
						
					}
					
					
				}
				
						
					}
				});
				


		treeGridPhraseLazy.addExpandListener(new ExpandListener<TagRowItem>() {

			@Override
			public void itemExpand(ExpandEvent<TagRowItem> event) {
				
				TagRowItem rootPhraseItem = event.getExpandedItem();
				TagRowItem placeHolder = lazyData.getChildren(rootPhraseItem).get(0);
				
				// if the child is only a placeholder the value for Treepath field is not set.
				//means the user clicks this root item for the first time
				if (placeHolder.getTreePath()== null) {
				lazyData.removeItem(placeHolder);
				Set<GroupedQueryResult> groupedQueryResult = queryResult.asGroupedSet();

				Iterator<GroupedQueryResult> groupIterator = groupedQueryResult.iterator();

				while (groupIterator.hasNext()) {
					GroupedQueryResult onePhraseGroup = (GroupedQueryResult) groupIterator.next();
					if (onePhraseGroup.getGroup().equals(rootPhraseItem.getTreePath())) {
						try {
							lazyData.addItems(rootPhraseItem,
									getChilderenForSpecificPhrase(rootPhraseItem, onePhraseGroup));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				dataProviderLazy.refreshAll();
				treeGridPhraseLazy.expand(rootPhraseItem);
			} else {
				
			}
			}
		});
		
     treeGridPhraseLazy.addSelectionListener(new SelectionListener<TagRowItem>() {
			
			@Override
			public void selectionChange(SelectionEvent<TagRowItem> event) {
				
			Iterable<TagRowItem> selectedItems=	event.getAllSelectedItems();
			
			selectedItems.forEach(item -> { System.out.println(" TgaPath :"+item.getTreePath()+ 
					" Collection :"+item.getCollectionName()+" Tag ID:"+item.getTagInstanceID());});	
			}
		});
	}

	
	private void setDataTagStyle() throws Exception {

		tagData = new TreeData<>();
		tagData = populateTreeDataWithTags(repository, tagData, queryResult);
		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(tagData);

		treeGridTag.addColumn(TagRowItem::getTreePath).setCaption("Tag").setId("tagID");
		treeGridTag.getColumn("tagID").setExpandRatio(7);

		treeGridTag.addColumn(TagRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		treeGridTag.getColumn("freqID").setExpandRatio(1);

		dataProvider.refreshAll();
		treeGridTag.setDataProvider(dataProvider);
		treeGridTag.recalculateColumnWidths();
		treeGridTag.setWidth("100%");
		treeGridPanel.setContent(treeGridTag);
		//setDataPhraseStyle();
		setDataPhraseStyleLazy();
	}

/*	private void setDataPhraseStyle() throws Exception {

		Set<GroupedQueryResult> groupedQueryResults = queryResult.asGroupedSet();
		TreeData<TagRowItem> phraseData = new TreeData<>();
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
			phraseData.addItems(rootPhrase, retrieveChilderen(groupedQueryResult));

		}

		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(phraseData);

		treeGridPhrase.addColumn(TagRowItem::getTreePath).setCaption("Phrase").setId("phraseID");
		treeGridPhrase.getColumn("phraseID").setExpandRatio(7);

		treeGridPhrase.addColumn(TagRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		treeGridPhrase.getColumn("freqID").setExpandRatio(1);

		dataProvider.refreshAll();

		treeGridPhrase.setDataProvider(dataProvider);
		treeGridPhrase.setWidth("100%");
	}*/
	
	
	private void setDataPhraseStyleLazy() throws Exception {
		
		Set<GroupedQueryResult> groupedQueryResults = queryResult.asGroupedSet();
	
		for (GroupedQueryResult groupedQueryResult : groupedQueryResults) {

			String phrase = (String) groupedQueryResult.getGroup();
			TagRowItem rootPhrase = new TagRowItem();
			rootPhrase.setTreePath(phrase);
			rootPhrase.setFrequency(groupedQueryResult.getTotalFrequency());
			lazyData.addItems(null, rootPhrase);	
			TagRowItem placeHolder= new TagRowItem();	   
			lazyData.addItems(rootPhrase, placeHolder);
		
		}
	
		dataProviderLazy = new TreeDataProvider<>(lazyData);
		treeGridPhraseLazy.addColumn(TagRowItem::getTreePath).setCaption("Phrase").setId("phraseID");
		treeGridPhraseLazy.getColumn("phraseID").setExpandRatio(7);
		treeGridPhraseLazy.addColumn(TagRowItem::getFrequency).setCaption("Frequency").setId("freqID");
		treeGridPhraseLazy.getColumn("freqID").setExpandRatio(1);	
		dataProviderLazy.refreshAll();
		treeGridPhraseLazy.setDataProvider(dataProviderLazy);
		treeGridPhraseLazy.setWidth("100%");		
	}
	
	
	private void setDataPropertyStyle() throws Exception {
		TreeData<TagRowItem> propData = new TreeData<>();

		propData = populateTreeDataWithProperties(repository, propData, queryResult); // TODO !!!!!!

		TreeDataProvider<TagRowItem> dataProvider = new TreeDataProvider<>(propData);

		treeGridProperty.addColumn(TagRowItem::getTreePath).setCaption("Tag").setId("tagID");
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

		treeGridPanel.setContent(treeGridProperty);
		
		//setDataPhraseStyle();
		setDataPhraseStyleLazy();
	}

	private TreeData<TagRowItem> populateTreeDataWithTags(Repository repository, TreeData<TagRowItem> treeData,
			QueryResult queryResult) throws Exception {

		// adding tags as root items
		ArrayList<TagRowItem> tagsAsRoot = new ArrayList<TagRowItem>();

		for (QueryResultRow queryResultRow : queryResult) {

			TagQueryResultRow tagQueryResultRow = (TagQueryResultRow) queryResultRow;
			TagRowItem tagRowItem = new TagRowItem();

			tagRowItem.setTagDefinitionPath(tagQueryResultRow.getTagDefinitionPath());

			if (!tagsAsRoot.stream()
					.anyMatch(var -> var.getTagDefinitionPath().equalsIgnoreCase(tagRowItem.getTagDefinitionPath()))) {

				tagRowItem.setTreePath(tagRowItem.getTagDefinitionPath());
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
							collectionsForADocument.add(tagRowItem);
						} else {
							collectionsForADocument.stream()
									.filter(x -> x.getTagDefinitionPath().equals(tagRowItem.getTagDefinitionPath()))
									.findFirst().get().setFrequencyOneUp();
						}
					}

				}
				treeData.addItems(oneDoc, collectionsForADocument);
			}
		}

		return treeData;
	}
	

	private TreeData<TagRowItem> populateTreeDataWithProperties(Repository repository, TreeData<TagRowItem> treeData,
			QueryResult queryResult) throws Exception {

		// adding tags as root items, for now. in future properties will be the root elements 
		ArrayList<TagRowItem> tagsAsRoot = new ArrayList<TagRowItem>();

		for (QueryResultRow queryResultRow : queryResult) {

			TagQueryResultRow tagQueryResultRow = (TagQueryResultRow) queryResultRow;
			TagRowItem tagRowItem = new TagRowItem();

			tagRowItem.setTagDefinitionPath(tagQueryResultRow.getTagDefinitionPath());

			if (!tagsAsRoot.stream()
					.anyMatch(var -> var.getTagDefinitionPath().equalsIgnoreCase(tagRowItem.getTagDefinitionPath()))) {

				tagRowItem.setTreePath(tagRowItem.getTagDefinitionPath());
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

							treeData.addItem(oneCollection, propItem);
						}

					}

				}

			}

		}

		return treeData;

	}

/*	// get docs as children for a phrase- normal style
	private ArrayList<TagRowItem> retrieveChilderen(GroupedQueryResult groupedQueryResult) throws Exception {

		Set<String> docsForAPhrase = groupedQueryResult.getSourceDocumentIDs();
		ArrayList<TagRowItem> docItems = new ArrayList<>();
		for (String doc : docsForAPhrase) {
			TagRowItem rowItem = new TagRowItem();
			// get SourceDoc name from sorceDocID
			String docName = retrieveDocumentName(this.repository, doc);
			rowItem.setTreePath(docName);
			rowItem.setFrequency(groupedQueryResult.getFrequency(doc));
			docItems.add(rowItem);

		}
		return docItems;
	}*/
	
	private void setChildrenSelected( TagRowItem item) {
	Iterable<TagRowItem> childIterartor =  tagData.getChildren(item);
	childIterartor.forEach(x->treeGridTag.select(x));	
	}
	
	private void setParentUnSelected( TagRowItem item) {	
	TagRowItem parent =  tagData.getParent(item);
    treeGridTag.deselect(parent);	
	}
	
	
	
	private void checkIfAllSiblingsAreSelectedAndSelectParent(TagRowItem item) {
		
		boolean allChildrenSelected = true;
		TagRowItem      parent  = tagData.getParent(item);
		List<TagRowItem> children =tagData.getChildren(item);
		List<TagRowItem> siblings = tagData.getChildren(parent);
		
/*	
			if (siblings.size() == 1) {			
				treeGridTag.asMultiSelect().select(parent);		
			} else {*/
				
				
				for (TagRowItem sibl : siblings) {
					if (treeGridTag.asMultiSelect().isSelected(sibl)) {
				//allChildrenSelected remains true
					} else {
						allChildrenSelected = false;
					}
				}

				if (allChildrenSelected) {
					treeGridTag.asMultiSelect().select(parent);

				} else {
					treeGridTag.asMultiSelect().deselect(parent);

				}
			
			//}

	}
	
	private void adaptSelectStatus() {
		
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

	private void swichView() throws Exception {
		
		switch(currentView){
		
		case tag: 	setCurrentView(ViewID.phraseTag);
		treeGridPanel.setContent(treeGridPhraseLazy);
		break;
		
		case property: 	setCurrentView(ViewID.phraseProperty);
		treeGridPanel.setContent(treeGridPhraseLazy);
		break;
		
		case phrase: Notification.show("no tag view available for that query", Notification.Type.HUMANIZED_MESSAGE);
		break;
		
		case phraseProperty: setCurrentView(ViewID.property);
		treeGridPanel.setContent(treeGridProperty);
		break;
		
		case phraseTag: setCurrentView(ViewID.tag);
		treeGridPanel.setContent(treeGridTag);
		break;
		
		
		default:
			Notification.show("no view available ", Notification.Type.HUMANIZED_MESSAGE);
			break;
							
		
       }
	}
	        


}


