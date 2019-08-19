package de.catma.ui.analyzenew.queryresultpanel;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.cache.LoadingCache;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.ExpandEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.DescriptionGenerator;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.repository.Repository;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.TagDefinition;
import de.catma.ui.analyzenew.treegridhelper.TreeRowItem;
import de.catma.ui.component.IconButton;
import de.catma.ui.tagger.annotationpanel.AnnotatedTextProvider;

public class QueryResultPanel extends VerticalLayout {
	
	public static interface CloseListener {
		public void closeRequest(QueryResultPanel resultPanel);
	}
	
	private ContextMenu optionsMenu;

	private Label queryInfo;

	private LoadingCache<String , KwicProvider> kwicProviderCache;

	private Button caretRightBt;
	private Button caretDownBt;
	private Button removeBt;
	private Button optionsBt;

	private QueryResult queryResult;
	private String query;
	private String creationTime;
	private Repository project;

	private VerticalLayout treeGridPanel;

	private TreeData<QueryResultRowItem> phraseBasedTreeData;
	private TreeData<QueryResultRowItem> tagBasedTreeData;
	private TreeData<QueryResultRowItem> flatTagBasedTreeData;
	private TreeData<QueryResultRowItem> propertiesAsColumnsTagBasedTreeData;

	private boolean resultContainsProperties = false;
	private TreeSet<String> propertyNames;
	
	private TreeGrid<QueryResultRowItem> queryResultGrid;

	private MenuItem miGroupByTagPath;
	private MenuItem miGroupByPhrase;
	private MenuItem miFlatTable;
	private MenuItem miPropertiesAsColumns;
	
	private DisplaySetting displaySetting;
	
	public QueryResultPanel(Repository project, QueryResult result, String query, 
			LoadingCache<String, KwicProvider> kwicProviderCache,
			CloseListener resultPanelCloseListener) throws Exception {

		this.project = project;
		this.queryResult = result;
		this.query = query;
		this.kwicProviderCache= kwicProviderCache;

		initComponents();
		initActions(resultPanelCloseListener);
		initPhraseBasedData();
	}

	private void initPhraseBasedData() {
		displaySetting = DisplaySetting.GROUPED_BY_PHRASE;
		
		miGroupByTagPath.setEnabled(true);
		miGroupByPhrase.setEnabled(false);
		miFlatTable.setEnabled(true);
		miPropertiesAsColumns.setEnabled(true);
		
		treeGridPanel.removeAllComponents();
		
		initQueryResultGrid();
		
		queryResultGrid
			.addColumn(QueryResultRowItem::getKey)
			.setCaption("Phrase")
			.setRenderer(new HtmlRenderer())
			.setWidth(400);

		queryResultGrid
			.addColumn(QueryResultRowItem::getFrequency)
			.setCaption("Frequency")
			.setExpandRatio(1);
		
		TreeDataProvider<QueryResultRowItem> queryResultDataProvider = 
				new TreeDataProvider<QueryResultRowItem>(getPhraseBasedTreeData());
		
		queryResultGrid.setDataProvider(queryResultDataProvider);

		treeGridPanel.addComponent(queryResultGrid);
	}
	
	private TreeData<QueryResultRowItem> getPhraseBasedTreeData() {
		if (phraseBasedTreeData == null) {
			phraseBasedTreeData = new TreeData<QueryResultRowItem>();
			
			Set<GroupedQueryResult> groupedQueryResults = queryResult.asGroupedSet();
			
			for (GroupedQueryResult groupedQueryResult : groupedQueryResults) {
				PhraseQueryResultRowItem phraseQueryResultRowItem = 
						new PhraseQueryResultRowItem(groupedQueryResult);
				phraseBasedTreeData.addItem(null, phraseQueryResultRowItem);
				phraseBasedTreeData.addItem(phraseQueryResultRowItem, new DummyQueryResultRowItem());
			}
		}
		return phraseBasedTreeData;
	}
	

	private void initPropertiesAsColumnsTagBasedData() {
		displaySetting = DisplaySetting.PROPERTIES_AS_COLUMNS;
		
		miGroupByTagPath.setEnabled(true);
		miGroupByPhrase.setEnabled(true);
		miFlatTable.setEnabled(true);
		miPropertiesAsColumns.setEnabled(false);
		
		treeGridPanel.removeAllComponents();
		
		initQueryResultGrid();
		
		TreeDataProvider<QueryResultRowItem> queryResultDataProvider = 
				new TreeDataProvider<QueryResultRowItem>(getPropertiesAsColumnsTagBasedTreeData());
		
		queryResultGrid
			.addColumn(QueryResultRowItem::getTagPath)
			.setCaption("Tag Path")
			.setWidth(200);

		queryResultGrid
			.addColumn(QueryResultRowItem::getKey)
			.setCaption("Annotation")
			.setRenderer(new HtmlRenderer())
			.setWidth(200);
		
		for (String propertyName : propertyNames) {
			queryResultGrid.addColumn(item -> item.getPropertyValue(propertyName))
				.setCaption(propertyName)
				.setWidth(200);
		}
		
		queryResultGrid
			.addColumn(QueryResultRowItem::getDocumentName)
			.setCaption("Document")
			.setWidth(200);		
		
		queryResultGrid
			.addColumn(QueryResultRowItem::getCollectionName)
			.setCaption("Collection")
			.setWidth(200);
		

		queryResultGrid.setDataProvider(queryResultDataProvider);
		
		treeGridPanel.addComponent(queryResultGrid);
		
		if (queryResultDataProvider.getTreeData().getRootItems().size() == 0) {
			Notification.show(
				"Info", "Your query result does not contain annotated occurrences!", Type.HUMANIZED_MESSAGE);
		}
		
	}


	private void initFlatTagBasedData() {
		displaySetting = DisplaySetting.ANNOTATIONS_AS_FLAT_TABLE;
		
		miGroupByTagPath.setEnabled(true);
		miGroupByPhrase.setEnabled(true);
		miFlatTable.setEnabled(false);
		miPropertiesAsColumns.setEnabled(true);
		
		treeGridPanel.removeAllComponents();
		
		initQueryResultGrid();
		
		TreeDataProvider<QueryResultRowItem> queryResultDataProvider = 
				new TreeDataProvider<QueryResultRowItem>(getFlatTagBasedTreeData());
		
		queryResultGrid
			.addColumn(QueryResultRowItem::getTagPath)
			.setCaption("Tag Path")
			.setWidth(200);

		queryResultGrid
			.addColumn(QueryResultRowItem::getDocumentName)
			.setCaption("Document")
			.setWidth(200);		
		
		queryResultGrid
			.addColumn(QueryResultRowItem::getCollectionName)
			.setCaption("Collection")
			.setWidth(200);
		
		queryResultGrid
			.addColumn(QueryResultRowItem::getKey)
			.setCaption("Annotation")
			.setRenderer(new HtmlRenderer())
			.setWidth(200);
		
		if (resultContainsProperties) {
			queryResultGrid.addColumn(QueryResultRowItem::getPropertyName)
			.setCaption("Property")
			.setWidth(100);
			queryResultGrid.addColumn(QueryResultRowItem::getPropertyValue)
			.setCaption("Property Value")
			.setWidth(300);
		}

		queryResultGrid.setDataProvider(queryResultDataProvider);
		
		treeGridPanel.addComponent(queryResultGrid);
		
		if (queryResultDataProvider.getTreeData().getRootItems().size() == 0) {
			Notification.show(
				"Info", "Your query result does not contain annotated occurrences!", Type.HUMANIZED_MESSAGE);
		}

	}

	private void initTagBasedData() {
		displaySetting = DisplaySetting.GROUPED_BY_TAG;
		
		miGroupByTagPath.setEnabled(false);
		miGroupByPhrase.setEnabled(true);
		miFlatTable.setEnabled(true);
		miPropertiesAsColumns.setEnabled(true);
		
		treeGridPanel.removeAllComponents();
		
		initQueryResultGrid();
		
		TreeDataProvider<QueryResultRowItem> queryResultDataProvider = 
				new TreeDataProvider<QueryResultRowItem>(getTagBasedTreeData());
		
		Column<QueryResultRowItem, ?> tagPathColumn = queryResultGrid
			.addColumn(QueryResultRowItem::getKey)
			.setCaption("Tag Path")
			.setRenderer(new HtmlRenderer())
			.setWidth(400);
		
		if (resultContainsProperties) {
			queryResultGrid.addColumn(QueryResultRowItem::getPropertyName)
			.setCaption("Property")
			.setWidth(100);
			queryResultGrid.addColumn(QueryResultRowItem::getPropertyValue)
			.setCaption("Property Value")
			.setWidth(300);
			
			tagPathColumn.setWidth(200);
		}

		queryResultGrid
			.addColumn(QueryResultRowItem::getFrequency)
			.setCaption("Frequency")
			.setExpandRatio(1);
		
		queryResultGrid.setDataProvider(queryResultDataProvider);
		
		treeGridPanel.addComponent(queryResultGrid);
		
		if (queryResultDataProvider.getTreeData().getRootItems().size() == 0) {
			Notification.show(
				"Info", "Your query result does not contain annotated occurrences!", Type.HUMANIZED_MESSAGE);
		}
	}
	
	
	private TreeData<QueryResultRowItem> getPropertiesAsColumnsTagBasedTreeData() {
		
		if (propertiesAsColumnsTagBasedTreeData == null) {
			propertiesAsColumnsTagBasedTreeData = new TreeData<QueryResultRowItem>();
			try {
				
				HashMap<String, QueryResultRowArray> rowsGroupedByTagInstance = 
						new HashMap<String, QueryResultRowArray>();
				
				propertyNames = new TreeSet<String>();
				for (QueryResultRow row : queryResult) {
					
					if (row instanceof TagQueryResultRow) {
						TagQueryResultRow tRow = (TagQueryResultRow) row;
						QueryResultRowArray rows = 
								rowsGroupedByTagInstance.get(tRow.getTagInstanceId());
						
						if (rows == null) {
							rows = new QueryResultRowArray();
							rowsGroupedByTagInstance.put(tRow.getTagInstanceId(), rows);
						}
						rows.add(tRow);
						propertyNames.add(tRow.getPropertyName());
					}
				}
				
				
				for (Map.Entry<String, QueryResultRowArray> entry : rowsGroupedByTagInstance.entrySet()) {
					
					QueryResultRowArray rows = entry.getValue();
					TagQueryResultRow masterRow = (TagQueryResultRow) rows.get(0);
					
					KwicProvider kwicProvider = 
							kwicProviderCache.get(masterRow.getSourceDocumentId());
					TagDefinition tagDefinition = 
							project.getTagManager().getTagLibrary().getTagDefinition(
									masterRow.getTagDefinitionId());
					
					propertiesAsColumnsTagBasedTreeData.addItem(
							null, 
							new KwicPropertiesAsColumnsQueryResultRowItem(
								rows, 
								AnnotatedTextProvider.buildAnnotatedText(
										new ArrayList<>(masterRow.getRanges()), 
										kwicProvider, 
										tagDefinition),
								AnnotatedTextProvider.buildAnnotatedKeywordInContext(
										new ArrayList<>(masterRow.getRanges()), 
										kwicProvider, 
										tagDefinition, 
										masterRow.getTagDefinitionPath()),
								kwicProvider.getSourceDocumentName(),
								kwicProvider
									.getSourceDocument()
									.getUserMarkupCollectionReference(masterRow.getMarkupCollectionId())
									.getName()
							)
						);
				}				
			}
			catch (Exception e) {
				e.printStackTrace(); //TODO:
			}			
		}
		
		return propertiesAsColumnsTagBasedTreeData;
	}
	
	
	private TreeData<QueryResultRowItem> getFlatTagBasedTreeData() {
		if (flatTagBasedTreeData == null) {
			flatTagBasedTreeData = new TreeData<QueryResultRowItem>();
			try {
				for (QueryResultRow row : queryResult) {
					if (row instanceof TagQueryResultRow) {
						TagQueryResultRow tRow = (TagQueryResultRow)row;
						
						KwicProvider kwicProvider = kwicProviderCache.get(row.getSourceDocumentId());
						TagDefinition tagDefinition = 
							project.getTagManager().getTagLibrary().getTagDefinition(tRow.getTagDefinitionId());
							
						flatTagBasedTreeData.addItem(
							null, new KwicQueryResultRowItem(
								tRow, 
								AnnotatedTextProvider.buildAnnotatedText(
										new ArrayList<>(tRow.getRanges()), 
										kwicProvider, 
										tagDefinition),
								AnnotatedTextProvider.buildAnnotatedKeywordInContext(
										new ArrayList<>(tRow.getRanges()), 
										kwicProvider, 
										tagDefinition, 
										tRow.getTagDefinitionPath()),
								kwicProvider.getSourceDocumentName(),
								kwicProvider
									.getSourceDocument()
									.getUserMarkupCollectionReference(tRow.getMarkupCollectionId())
									.getName()
							)
						);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace(); //TODO:
			}
		}
		return flatTagBasedTreeData;
	}

	private TreeData<QueryResultRowItem> getTagBasedTreeData() {
		if (tagBasedTreeData == null) {
			resultContainsProperties = false;
			tagBasedTreeData = new TreeData<QueryResultRowItem>();
			Set<GroupedQueryResult> groupedQueryResults = queryResult.asGroupedSet(row -> {
				if (row instanceof TagQueryResultRow) {
					if (((TagQueryResultRow) row).getPropertyDefinitionId() != null) {
						resultContainsProperties = true;
					}
					return ((TagQueryResultRow) row).getTagDefinitionPath();
				}
				return "no Tag available / not annotated";
			});
			
			for (GroupedQueryResult groupedQueryResult : groupedQueryResults) {
				TagQueryResultRowItem tagQueryResultRowItem = 
						new TagQueryResultRowItem(groupedQueryResult, project);
				tagBasedTreeData.addItem(null, tagQueryResultRowItem);
				tagBasedTreeData.addItem(tagQueryResultRowItem, new DummyQueryResultRowItem());
			}
		}
		
		return tagBasedTreeData;
	}
	
	private void initQueryResultGrid() {
		// grid needs to be reinitialized when a new set of root nodes is set
		// otherwise the children triangle is not shown even if children are present
		queryResultGrid = new TreeGrid<QueryResultRowItem>();
		queryResultGrid.setSizeFull();
		
		queryResultGrid.addStyleNames("annotation-details-panel-annotation-details-grid",
				"flat-undecorated-icon-buttonrenderer", "no-focused-before-border");
		
		
		queryResultGrid.addExpandListener(event -> handleExpandRequest(event));
		queryResultGrid.setDescriptionGenerator(new DescriptionGenerator<QueryResultRowItem>() {
			@Override
			public String apply(QueryResultRowItem item) {
				return item.getDetailedKeyInContext();
			}
		}, ContentMode.HTML);
	}

	private void initComponents() {
		addStyleName("analyze-card");
		setMargin(false);
		setSpacing(false);

		initQueryResultGrid();
		
		createResultInfoBar();
		createButtonBar();
		treeGridPanel = new VerticalLayout();
		treeGridPanel.setSizeFull();
		treeGridPanel.setMargin(false);
		treeGridPanel.addStyleName("analyze-queryresult-panel");
		
		treeGridPanel.addComponent(queryResultGrid);
	}

	private void createResultInfoBar() {
		QueryResultRowArray resultRowArrayArrayList = queryResult.asQueryResultRowArray();
		int resultSize = resultRowArrayArrayList.size(); //TODO: analyze during loop
		//TODO: use java.util.time
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		creationTime = timestamp.toString().substring(0, 19);
		queryInfo = new Label(query + "(" + resultSize + ")"+" created: "+creationTime);
		
		queryInfo.addStyleName("analyze-card-infobar");
		
		addComponent(queryInfo);
	}

	private void createButtonBar() {
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setWidth("100%");
		
		caretRightBt = new IconButton(VaadinIcons.CARET_RIGHT);

		caretDownBt = new IconButton(VaadinIcons.CARET_DOWN);

		optionsBt = new IconButton(VaadinIcons.ELLIPSIS_V);
		optionsMenu = new ContextMenu(optionsBt,true);

		removeBt = new IconButton(VaadinIcons.ERASER);

		buttonPanel.addComponents(removeBt, optionsBt, caretRightBt);
		buttonPanel.setComponentAlignment(caretRightBt, Alignment.MIDDLE_RIGHT);
		buttonPanel.setComponentAlignment(optionsBt, Alignment.MIDDLE_RIGHT);
		buttonPanel.setComponentAlignment(removeBt, Alignment.MIDDLE_RIGHT);
		buttonPanel.setExpandRatio(removeBt, 1f);
		
		buttonPanel.addStyleName("analyze-card-buttonbar");
		addComponent(buttonPanel);
	}

	private void initActions(CloseListener resultPanelCloseListener) {
		caretRightBt.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				addComponent(treeGridPanel);
				((HorizontalLayout)caretRightBt.getParent()).replaceComponent(caretRightBt, caretDownBt);
			}
		});

		caretDownBt.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				removeComponent(treeGridPanel);
				((HorizontalLayout)caretDownBt.getParent()).replaceComponent(caretDownBt, caretRightBt);
			}
		});
		
		optionsBt.addClickListener((evt) ->  optionsMenu.open(evt.getClientX(), evt.getClientY()));
		
		miGroupByPhrase = optionsMenu.addItem("Group by Phrase", mi-> initPhraseBasedData());
		miGroupByPhrase.setEnabled(false);
		miGroupByTagPath = optionsMenu.addItem("Group by Tag Path", mi -> initTagBasedData());
		miFlatTable = optionsMenu.addItem("Display Annotations as flat table", mi -> initFlatTagBasedData());
		miPropertiesAsColumns = optionsMenu.addItem("Display Properties as columns", mi -> initPropertiesAsColumnsTagBasedData());

		removeBt.addClickListener(clickEvent -> resultPanelCloseListener.closeRequest(QueryResultPanel.this));
	}


	private void handleExpandRequest(ExpandEvent<QueryResultRowItem> event) {
		QueryResultRowItem expandedItem = event.getExpandedItem();
		@SuppressWarnings("unchecked")
		TreeData<QueryResultRowItem> currentData = 
				((TreeDataProvider<QueryResultRowItem>)queryResultGrid.getDataProvider()).getTreeData();
		QueryResultRowItem firstChild = currentData.getChildren(expandedItem).get(0);
		if (firstChild.isExpansionDummy()) {
			currentData.removeItem(firstChild);
			
			expandedItem.addChildRowItems(currentData, kwicProviderCache);
		}

		queryResultGrid.getDataProvider().refreshAll();
	}

	@SuppressWarnings("unchecked")
	public TreeData<TreeRowItem> getCurrentTreeGridData() {

//		TreeGrid<TreeRowItem> currentTreeGrid = (TreeGrid<TreeRowItem>) treeGridPanel.getComponent(0);
//		TreeDataProvider<TreeRowItem> dataProvider = (TreeDataProvider<TreeRowItem>) currentTreeGrid.getDataProvider();
//		TreeData<TreeRowItem> treeData = (TreeData<TreeRowItem>) dataProvider.getTreeData();
//		return copyTreeData(treeData);
		return null;
	}
	
	public QueryResultPanelSetting getQueryResultPanelSetting() {
		return new QueryResultPanelSetting(query, queryResult, displaySetting);
	}
	
	public String getQueryAsString() {
		return this.query+ "("+ creationTime+")";
	}

}
