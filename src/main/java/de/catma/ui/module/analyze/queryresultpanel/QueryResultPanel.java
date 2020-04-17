package de.catma.ui.module.analyze.queryresultpanel;

import java.lang.ref.WeakReference;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import com.google.common.cache.LoadingCache;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.GridSortOrderBuilder;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.InMemoryDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.event.ExpandEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.FooterCell;
import com.vaadin.ui.components.grid.FooterRow;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.queryengine.QueryId;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.TagDefinition;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.module.analyze.CSVExportFlatStreamSource;
import de.catma.ui.module.analyze.CSVExportGroupedStreamSource;
import de.catma.ui.module.annotate.annotationpanel.AnnotatedTextProvider;
import de.catma.ui.module.main.ErrorHandler;

public class QueryResultPanel extends VerticalLayout {
	
	public static interface CloseListener {
		public void closeRequest(QueryResultPanel resultPanel);
	}
	
	public static interface ItemSelectionListener {
		public void itemSelected(QueryResultRowItem item);
	}
	
	public static interface ItemRemovedListener {
		public void itemRemoved(QueryResultRowItem item);
	}
	
	private ContextMenu optionsMenu;

	private TextField queryInfo;

	private LoadingCache<String , KwicProvider> kwicProviderCache;

	private Button caretRightBt;
	private Button caretDownBt;
	private Button removeBt;
	private Button optionsBt;

	private QueryResult queryResult;

	private Project project;

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

	private boolean cardStyle;

	private ItemSelectionListener itemSelectionListener;

	private ItemRemovedListener itemRemovedListener;

	private boolean includeQueryId;

	private TextField searchField;

	private QueryId queryId;

	private int tokenCount;

	private HorizontalLayout buttonPanel;
	
	private DisplaySettingChangeListener displaySettingChangeListener;
	
	private ArrayList<Registration> itemSelectionListenerRegistrations;
	private ArrayList<WeakReference<SelectionListener<QueryResultRowItem>>> itemSelectionListeners;
	
	private PunctuationFilter punctuationFilter;

	public QueryResultPanel(Project project, QueryResult result, QueryId queryId, 
			LoadingCache<String, KwicProvider> kwicProviderCache, DisplaySetting displaySetting, 
			ItemSelectionListener itemSelectionListener) {
		this(project, result, queryId, kwicProviderCache, null, displaySetting, 
				itemSelectionListener, null, false, false);
	}

	public QueryResultPanel(
			Project project,
			LoadingCache<String, KwicProvider> kwicProviderCache, DisplaySetting displaySetting, 
			ItemRemovedListener itemRemovedListener) {
		this(project, new QueryResultRowArray(), new QueryId("")/*no query*/, kwicProviderCache, 
				null, displaySetting, null, itemRemovedListener, false, true);
	}
	
	public QueryResultPanel(Project project, QueryResult result, QueryId queryId, 
			LoadingCache<String, KwicProvider> kwicProviderCache,
			CloseListener resultPanelCloseListener) {
		this(project, result, queryId, kwicProviderCache, resultPanelCloseListener, 
				DisplaySetting.GROUPED_BY_PHRASE, null, null, true, false);
	}
	
	protected QueryResultPanel(Project project, QueryResult result, QueryId queryId, 
			LoadingCache<String, KwicProvider> kwicProviderCache,
			CloseListener resultPanelCloseListener, DisplaySetting displaySetting,
			ItemSelectionListener itemSelectionListener, ItemRemovedListener itemRemovedListener,
			boolean cardStyle, boolean includeQueryId) {

		this.project = project;
		this.queryResult = result;
		this.kwicProviderCache= kwicProviderCache;
		this.itemSelectionListener = itemSelectionListener;
		this.itemRemovedListener = itemRemovedListener;
		this.cardStyle = cardStyle;
		this.includeQueryId = includeQueryId;
		this.queryId = queryId; 
		this.itemSelectionListenerRegistrations = new ArrayList<>();
		this.itemSelectionListeners = new ArrayList<>();
		
		initComponents();
		initActions(resultPanelCloseListener);
		displaySetting.init(this);
		if (cardStyle) {
			caretRightBt.click();
		}
	}

	void initPhraseBasedData() {
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
			.setDescriptionGenerator(item -> item.getDetailedKeyInContext(), ContentMode.HTML)			
			.setRenderer(new HtmlRenderer())
			.setId("phrase")
			.setWidth(310);

		
		Column<QueryResultRowItem, Integer> freqColumn = queryResultGrid
			.addColumn(QueryResultRowItem::getFrequency)
			.setCaption("Frequency")
			.setId("frequency");
		
		if ((itemSelectionListener == null) && (itemRemovedListener==null)) {
			freqColumn.setExpandRatio(1);
		}
		else {
			freqColumn.setWidth(120);
		}
		
		if (itemSelectionListener != null) {
			ButtonRenderer<QueryResultRowItem> selectItemsRenderer = new ButtonRenderer<QueryResultRowItem>(
					rendererClickEvent -> itemSelectionListener.itemSelected(rendererClickEvent.getItem()));
			selectItemsRenderer.setHtmlContentAllowed(true);

			queryResultGrid
			.addColumn((item) -> VaadinIcons.ARROW_CIRCLE_DOWN_O.getHtml())
			.setCaption("Select")
			.setRenderer(selectItemsRenderer)
			.setExpandRatio(1);		
		}

		if (itemRemovedListener != null) {
			ButtonRenderer<QueryResultRowItem> removeItemsRenderer = new ButtonRenderer<QueryResultRowItem>(
					rendererClickEvent -> itemRemovedListener.itemRemoved(rendererClickEvent.getItem()));
			removeItemsRenderer.setHtmlContentAllowed(true);

			queryResultGrid
			.addColumn((item) -> VaadinIcons.ERASER.getHtml())
			.setCaption("Remove")
			.setRenderer(removeItemsRenderer)
			.setExpandRatio(1);		
		}		
		
		FooterRow footer = queryResultGrid.prependFooterRow();
		queryResultGrid.setFooterVisible(true);
		TreeDataProvider<QueryResultRowItem> queryResultDataProvider = 
				new TreeDataProvider<QueryResultRowItem>(getPhraseBasedTreeData());
		
		queryResultDataProvider.addFilter(punctuationFilter);
		
		queryResultGrid.setDataProvider(queryResultDataProvider);

		queryResultDataProvider.addDataProviderListener(
			event -> handleTotalTypesChange(
				"Types", 
				footer.getCell("phrase"), 
				"Tokens",
				footer.getCell("frequency"), 
				queryResultDataProvider));
		
		handleTotalTypesChange(
				"Types", 
				footer.getCell("phrase"), 
				"Tokens",
				footer.getCell("frequency"), 
				queryResultDataProvider);
		
		treeGridPanel.addComponent(queryResultGrid);
		
		queryResultGrid.setSortOrder(new GridSortOrderBuilder<QueryResultRowItem>().thenDesc(freqColumn).build());
		
		initInfoLabel();
		fireDisplaySettingChanged();
	}
	
	private void handleTotalTypesChange(
		String types,
		FooterCell typeFooterCell, 
		String tokens,
		FooterCell tokenFooterCell, 
		TreeDataProvider<QueryResultRowItem> queryResultDataProvider) {
		List<QueryResultRowItem> filteredItems = getFilteredQueryResultRowItems();
		if (typeFooterCell != null) {
			typeFooterCell.setText(types + ": " + filteredItems.size());
		}
		tokenCount = filteredItems.stream().map(item -> item.getFrequency()).reduce(0, Integer::sum).intValue();
		tokenFooterCell.setText(tokens + ": " + tokenCount);
	}

	private void initInfoLabel() {
		if (cardStyle) {
			this.queryInfo.setValue(queryId + " (" + tokenCount + ")");
		}
	}

	private TreeData<QueryResultRowItem> getPhraseBasedTreeData() {
		if (phraseBasedTreeData == null) {
			phraseBasedTreeData = new TreeData<QueryResultRowItem>();
			
			addPhraseBasedRootItems(queryResult);
		}
		return phraseBasedTreeData;
	}
	

	void initPropertiesAsColumnsTagBasedData() {
		displaySetting = DisplaySetting.PROPERTIES_AS_COLUMNS;
		
		miGroupByTagPath.setEnabled(true);
		miGroupByPhrase.setEnabled(true);
		miFlatTable.setEnabled(true);
		miPropertiesAsColumns.setEnabled(false);
		
		treeGridPanel.removeAllComponents();
		
		initQueryResultGrid();
		
		TreeDataProvider<QueryResultRowItem> queryResultDataProvider = 
				new TreeDataProvider<QueryResultRowItem>(getPropertiesAsColumnsTagBasedTreeData());
		queryResultDataProvider.addFilter(punctuationFilter);
		
		Column<QueryResultRowItem, ?> tagPathColumn = queryResultGrid
			.addColumn(QueryResultRowItem::getTagPath)
			.setCaption("Tag Path")
			.setWidth(200);

		if (includeQueryId) {
			//TODO: add a queryId column
		}

		queryResultGrid
			.addColumn(QueryResultRowItem::getKey)
			.setCaption("Annotation")
			.setDescriptionGenerator(item -> item.getDetailedKeyInContext(), ContentMode.HTML)
			.setRenderer(new HtmlRenderer())
			.setId("annotation")
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
		
		if (itemSelectionListener != null) {
			ButtonRenderer<QueryResultRowItem> selectItemsRenderer = new ButtonRenderer<QueryResultRowItem>(
					rendererClickEvent -> itemSelectionListener.itemSelected(rendererClickEvent.getItem()));
			selectItemsRenderer.setHtmlContentAllowed(true);
			queryResultGrid
			.addColumn((item) -> VaadinIcons.ARROW_CIRCLE_DOWN_O.getHtml())
			.setCaption("Select")
			.setRenderer(selectItemsRenderer)
			.setExpandRatio(1);		
		}
		
		if (itemRemovedListener != null) {
			ButtonRenderer<QueryResultRowItem> removeItemsRenderer = new ButtonRenderer<QueryResultRowItem>(
					rendererClickEvent -> itemRemovedListener.itemRemoved(rendererClickEvent.getItem()));
			removeItemsRenderer.setHtmlContentAllowed(true);

			queryResultGrid
			.addColumn((item) -> VaadinIcons.ERASER.getHtml())
			.setCaption("Remove")
			.setRenderer(removeItemsRenderer)
			.setExpandRatio(1);		
		}		
		
		FooterRow footer = queryResultGrid.prependFooterRow();
		queryResultGrid.setFooterVisible(true);
		
		queryResultDataProvider.addDataProviderListener(
			event -> handleTotalTypesChange(
						null, 
						null,
						"Annotations",
						footer.getCell("annotation"), 
						queryResultDataProvider));
		
		queryResultGrid.setDataProvider(queryResultDataProvider);
		handleTotalTypesChange(
				null, 
				null, 
				"Annotations",
				footer.getCell("annotation"), 
				queryResultDataProvider);

		
		treeGridPanel.addComponent(queryResultGrid);
		
		if (queryResultDataProvider.getTreeData().getRootItems().size() == 0) {
			Notification.show(
				"Info", "Your query result does not contain annotated occurrences!", Type.HUMANIZED_MESSAGE);
		}

		queryResultGrid.setSortOrder(new GridSortOrderBuilder<QueryResultRowItem>().thenAsc(tagPathColumn).build());
		
		initInfoLabel();
		
		fireDisplaySettingChanged();
	}


	void initFlatTagBasedData() {
		displaySetting = DisplaySetting.ANNOTATIONS_AS_FLAT_TABLE;
		
		miGroupByTagPath.setEnabled(true);
		miGroupByPhrase.setEnabled(true);
		miFlatTable.setEnabled(false);
		miPropertiesAsColumns.setEnabled(true);
		
		treeGridPanel.removeAllComponents();
		
		initQueryResultGrid();
		
		TreeDataProvider<QueryResultRowItem> queryResultDataProvider = 
				new TreeDataProvider<QueryResultRowItem>(getFlatTagBasedTreeData());
		queryResultDataProvider.addFilter(punctuationFilter);
		
		Column<QueryResultRowItem, ?> tagPathColumn = queryResultGrid
			.addColumn(QueryResultRowItem::getTagPath)
			.setCaption("Tag Path")
			.setDescriptionGenerator(item -> item.getDetailedKeyInContext(), ContentMode.HTML)			
			.setWidth(200);
		
		if (includeQueryId) {
			//TODO: add a queryId column
		}

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
			.setId("annotation")
			.setRenderer(new HtmlRenderer())
			.setWidth(200);
		
		if (resultContainsProperties) {
			queryResultGrid.addColumn(QueryResultRowItem::getPropertyName)
			.setCaption("Property")
			.setWidth(100);
			queryResultGrid.addColumn(QueryResultRowItem::getPropertyValue)
			.setId("propertyvalue")
			.setCaption("Property Value")
			.setWidth(300);
		}

		if (itemSelectionListener != null) {
			ButtonRenderer<QueryResultRowItem> selectItemsRenderer = new ButtonRenderer<QueryResultRowItem>(
					rendererClickEvent -> itemSelectionListener.itemSelected(rendererClickEvent.getItem()));
			selectItemsRenderer.setHtmlContentAllowed(true);
			queryResultGrid
			.addColumn((item) -> VaadinIcons.ARROW_CIRCLE_DOWN_O.getHtml())
			.setCaption("Select")
			.setRenderer(selectItemsRenderer)
			.setExpandRatio(1);
		}

		if (itemRemovedListener != null) {
			ButtonRenderer<QueryResultRowItem> removeItemsRenderer = new ButtonRenderer<QueryResultRowItem>(
					rendererClickEvent -> itemRemovedListener.itemRemoved(rendererClickEvent.getItem()));
			removeItemsRenderer.setHtmlContentAllowed(true);

			queryResultGrid
			.addColumn((item) -> VaadinIcons.ERASER.getHtml())
			.setCaption("Remove")
			.setRenderer(removeItemsRenderer)
			.setExpandRatio(1);		
		}			

		queryResultGrid.setDataProvider(queryResultDataProvider);
		FooterRow footer = queryResultGrid.prependFooterRow();
		queryResultGrid.setFooterVisible(true);
		if (resultContainsProperties) {
			queryResultDataProvider.addDataProviderListener(
					event -> handleTotalTypesChange(
							null, 
							null,
							"Property Values",
							footer.getCell("propertyvalue"), 
							queryResultDataProvider));
			handleTotalTypesChange(
					null, 
					null,
					"Property Values",
					footer.getCell("propertyvalue"), 
					queryResultDataProvider);
		}
		else {
			queryResultDataProvider.addDataProviderListener(
					event -> handleTotalTypesChange(
							null, 
							null,
							"Annotations",
							footer.getCell("annotation"), 
							queryResultDataProvider));
			handleTotalTypesChange(
					null, 
					null, 
					"Annotations",
					footer.getCell("annotation"), 
					queryResultDataProvider);
			
		}
		treeGridPanel.addComponent(queryResultGrid);
		
		if (queryResultDataProvider.getTreeData().getRootItems().size() == 0) {
			Notification.show(
				"Info", "Your query result does not contain annotated occurrences!", Type.HUMANIZED_MESSAGE);
		}
		
		queryResultGrid.setSortOrder(new GridSortOrderBuilder<QueryResultRowItem>().thenAsc(tagPathColumn).build());
		
		initInfoLabel();
		
		fireDisplaySettingChanged();
	}

	void initTagBasedData() {
		displaySetting = DisplaySetting.GROUPED_BY_TAG;
		
		miGroupByTagPath.setEnabled(false);
		miGroupByPhrase.setEnabled(true);
		miFlatTable.setEnabled(true);
		miPropertiesAsColumns.setEnabled(true);
		
		treeGridPanel.removeAllComponents();
		
		initQueryResultGrid();

		TreeDataProvider<QueryResultRowItem> queryResultDataProvider = 
				new TreeDataProvider<QueryResultRowItem>(getTagBasedTreeData());
		queryResultDataProvider.addFilter(punctuationFilter);
		
		Column<QueryResultRowItem, ?> tagPathColumn = queryResultGrid
			.addColumn(QueryResultRowItem::getKey)
			.setCaption("Tag Path")
			.setRenderer(new HtmlRenderer())
			.setDescriptionGenerator(item -> item.getDetailedKeyInContext(), ContentMode.HTML)
			.setId("tagPath")
			.setWidth(300);
		
		if (resultContainsProperties) {
			queryResultGrid.addColumn(QueryResultRowItem::getPropertyName)
			.setCaption("Property")
			.setWidth(100);
			queryResultGrid.addColumn(QueryResultRowItem::getPropertyValue)
			.setCaption("Property Value")
			.setWidth(300);
			
			tagPathColumn.setWidth(130);
		}

		Column<QueryResultRowItem, ?> freqColumn = queryResultGrid
			.addColumn(QueryResultRowItem::getFrequency)
			.setId("frequency")
			.setCaption("Frequency");
		if ((itemSelectionListener == null) && (itemRemovedListener==null)) {
			freqColumn.setExpandRatio(1);
		}
		else {
			freqColumn.setWidth(130);
		}
		
		if (itemSelectionListener != null) {
			ButtonRenderer<QueryResultRowItem> selectItemsRenderer = new ButtonRenderer<QueryResultRowItem>(
					rendererClickEvent -> itemSelectionListener.itemSelected(rendererClickEvent.getItem()));
			selectItemsRenderer.setHtmlContentAllowed(true);
			queryResultGrid
			.addColumn((item) -> VaadinIcons.ARROW_CIRCLE_DOWN_O.getHtml())
			.setCaption("Select")
			.setRenderer(selectItemsRenderer)
			.setWidth(70);			
		}
		
		if (itemRemovedListener != null) {
			ButtonRenderer<QueryResultRowItem> removeItemsRenderer = new ButtonRenderer<QueryResultRowItem>(
					rendererClickEvent -> itemRemovedListener.itemRemoved(rendererClickEvent.getItem()));
			removeItemsRenderer.setHtmlContentAllowed(true);

			queryResultGrid
			.addColumn((item) -> VaadinIcons.ERASER.getHtml())
			.setCaption("Remove")
			.setRenderer(removeItemsRenderer)
			.setExpandRatio(1);		
		}	


		
		queryResultGrid.setDataProvider(queryResultDataProvider);
		
		FooterRow footer = queryResultGrid.prependFooterRow();
		queryResultGrid.setFooterVisible(true);
		queryResultDataProvider.addDataProviderListener(
				event -> handleTotalTypesChange(
						"Tags", 
						footer.getCell("tagPath"),
						resultContainsProperties?"Property Values":"Annotations",
						footer.getCell("frequency"), 
						queryResultDataProvider));
		handleTotalTypesChange(
				"Tags", 
				footer.getCell("tagPath"), 
				resultContainsProperties?"Property Values":"Annotations",
				footer.getCell("frequency"), 
				queryResultDataProvider);
		
		treeGridPanel.addComponent(queryResultGrid);
		
		if (queryResultDataProvider.getTreeData().getRootItems().size() == 0) {
			Notification.show(
				"Info", "Your query result does not contain annotated occurrences!", Type.HUMANIZED_MESSAGE);
		}
		
		initInfoLabel();
		
		queryResultGrid.setSortOrder(new GridSortOrderBuilder<QueryResultRowItem>().thenDesc(freqColumn).build());
		
		fireDisplaySettingChanged();
	}
	
	
	private TreeData<QueryResultRowItem> getPropertiesAsColumnsTagBasedTreeData() {
		
		if (propertiesAsColumnsTagBasedTreeData == null) {
			propertiesAsColumnsTagBasedTreeData = new TreeData<QueryResultRowItem>();
			propertyNames = new TreeSet<String>();
			addPropertiesAsColumnsTagBasedRootItems(queryResult);
		}
		
		return propertiesAsColumnsTagBasedTreeData;
	}
	
	
	private TreeData<QueryResultRowItem> getFlatTagBasedTreeData() {
		if (flatTagBasedTreeData == null) {
			resultContainsProperties = false;
			flatTagBasedTreeData = new TreeData<QueryResultRowItem>();
			addFlatTagBasedRootItems(queryResult);
		}
		return flatTagBasedTreeData;
	}

	private TreeData<QueryResultRowItem> getTagBasedTreeData() {
		if (tagBasedTreeData == null) {
			resultContainsProperties = false;
			tagBasedTreeData = new TreeData<QueryResultRowItem>();
			
			addTagBasedRootItems(queryResult);
		}
		
		return tagBasedTreeData;
	}
	
	private void initQueryResultGrid() {
		// grid needs to be reinitialized when a new set of root nodes is set
		// otherwise the children triangle is not shown even if children are present
		
		ArrayList<SelectionListener<QueryResultRowItem>> existingListeners = new ArrayList<>();
		
		if (queryResultGrid != null) {

			Iterator<WeakReference<SelectionListener<QueryResultRowItem>>> itemSelectionListenerIterator =
					itemSelectionListeners.iterator();
			while (itemSelectionListenerIterator.hasNext()) {
				SelectionListener<QueryResultRowItem> selectionListener =
						itemSelectionListenerIterator.next().get();
				
				if (selectionListener != null) {
					existingListeners.add(selectionListener);
				}
				
				itemSelectionListenerIterator.remove();
			}
			
			Iterator<Registration> registrationIterator = 
					itemSelectionListenerRegistrations.iterator();
			while (registrationIterator.hasNext()) {
				registrationIterator.next().remove();
				registrationIterator.remove();
			}
			
			if (queryResultGrid.getDataProvider() instanceof InMemoryDataProvider) {
				((InMemoryDataProvider<?>)queryResultGrid.getDataProvider()).clearFilters();
			}
		}
		
		queryResultGrid = TreeGridFactory.createDefaultTreeGrid();
		queryResultGrid.setSizeFull();
		
		existingListeners.forEach(
			listener -> addItemSelectionListener(listener));
		
		queryResultGrid.addStyleNames("annotation-details-panel-annotation-details-grid",
				"flat-undecorated-icon-buttonrenderer");
		
		
		queryResultGrid.addExpandListener(event -> handleExpandRequest(event));
	}

	public void addItemSelectionListener(
			SelectionListener<QueryResultRowItem> listener) {
		itemSelectionListeners.add(new WeakReference<>(listener));
		itemSelectionListenerRegistrations.add(
				queryResultGrid.addSelectionListener(listener));
	}

	private void initComponents() {
		if (cardStyle) {
			addStyleName("analyze-card");
		}
		setMargin(false);
		setSpacing(false);

		initQueryResultGrid();
		
		if (cardStyle) {
			createResultInfoBar();
		}
		createButtonBar(cardStyle);
		treeGridPanel = new VerticalLayout();
		treeGridPanel.setSizeFull();
		treeGridPanel.setMargin(false);
		treeGridPanel.addStyleName("analyze-queryresult-panel");
		
		if (!cardStyle) {
			addComponent(treeGridPanel);	
			setExpandRatio(treeGridPanel, 1f);
		}
	}

	private void createResultInfoBar() {
		queryInfo = new TextField();
		queryInfo.setWidth("90%");
		queryInfo.addStyleName("analyze-card-infobar");
		
		addComponent(queryInfo);
	}

	private void createButtonBar(boolean cardStyle) {
		buttonPanel = new HorizontalLayout();
		buttonPanel.setWidth("100%");
		
		searchField = new TextField();
        searchField.setPlaceholder("\u2315");
        buttonPanel.addComponent(searchField);
        buttonPanel.setComponentAlignment(searchField, Alignment.MIDDLE_RIGHT);
        buttonPanel.setExpandRatio(searchField, 1f);
		caretRightBt = new IconButton(VaadinIcons.CARET_RIGHT);
		caretRightBt.setVisible(cardStyle);
		caretDownBt = new IconButton(VaadinIcons.CARET_DOWN);
		caretDownBt.setVisible(cardStyle);

		optionsBt = new IconButton(VaadinIcons.ELLIPSIS_DOTS_V);
		optionsMenu = new ContextMenu(optionsBt,true);

		removeBt = new IconButton(VaadinIcons.ERASER);

		buttonPanel.addComponents(removeBt, optionsBt, caretRightBt);
		buttonPanel.setComponentAlignment(caretRightBt, Alignment.MIDDLE_RIGHT);
		buttonPanel.setComponentAlignment(optionsBt, Alignment.MIDDLE_RIGHT);
		buttonPanel.setComponentAlignment(removeBt, Alignment.MIDDLE_RIGHT);
		
		if (cardStyle) {
			buttonPanel.addStyleName("analyze-card-buttonbar");
			searchField.setEnabled(false);
		}
		else {
			buttonPanel.addStyleName("analyze-query-result-panel-buttonbar");
		}
		addComponent(buttonPanel);
	}

	private void initActions(CloseListener resultPanelCloseListener) {
		if (cardStyle) {
			caretRightBt.addClickListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					addComponent(treeGridPanel);
					((HorizontalLayout)caretRightBt.getParent()).replaceComponent(caretRightBt, caretDownBt);
					searchField.setEnabled(true);
				}
			});
	
			caretDownBt.addClickListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					removeComponent(treeGridPanel);
					((HorizontalLayout)caretDownBt.getParent()).replaceComponent(caretDownBt, caretRightBt);
					searchField.setEnabled(false);
				}
			});
		}
		
		optionsBt.addClickListener((evt) ->  optionsMenu.open(evt.getClientX(), evt.getClientY()));
		
		miGroupByPhrase = optionsMenu.addItem("Group by Phrase", mi-> initPhraseBasedData());
		miGroupByPhrase.setEnabled(false);
		miGroupByTagPath = optionsMenu.addItem("Group by Tag Path", mi -> initTagBasedData());
		miFlatTable = optionsMenu.addItem("Display Annotations as flat table", mi -> initFlatTagBasedData());
		miPropertiesAsColumns = optionsMenu.addItem("Display Properties as columns", mi -> initPropertiesAsColumnsTagBasedData());
		MenuItem miExport = optionsMenu.addItem("Export");
		MenuItem miCSVFlatExport = miExport.addItem("Export flat as CSV");
		
		StreamResource csvFlatExportResource = new StreamResource(
					new CSVExportFlatStreamSource(
						() -> getFilteredQueryResult(), 
						project, 
						kwicProviderCache, 
						((BackgroundServiceProvider)UI.getCurrent())),
					"CATMA-Query-Result_Export-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ".csv");
		csvFlatExportResource.setCacheTime(0);
		csvFlatExportResource.setMIMEType("text/comma-separated-values");
		
		FileDownloader csvFlatExportFileDownloader = 
			new FileDownloader(csvFlatExportResource);
		
		csvFlatExportFileDownloader.extend(miCSVFlatExport);
		
		MenuItem miCSVGroupedByPhraseExport = miExport.addItem("Export grouped as CSV");
		
		StreamResource csvGroupedByPhraseExportResource = new StreamResource(
					new CSVExportGroupedStreamSource(
						() -> getFilteredQueryResult(), 
						project, 
						() -> getDisplaySetting().equals(DisplaySetting.GROUPED_BY_TAG),
						kwicProviderCache, 
						((BackgroundServiceProvider)UI.getCurrent())),
					"CATMA-Query-Result_Export-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ".csv");
		csvGroupedByPhraseExportResource.setCacheTime(0);
		csvGroupedByPhraseExportResource.setMIMEType("text/comma-separated-values");
		
		FileDownloader csvGroupedByPhraseExportFileDownloader = 
			new FileDownloader(csvGroupedByPhraseExportResource);
		
		csvGroupedByPhraseExportFileDownloader.extend(miCSVGroupedByPhraseExport);
		
		
		MenuItem miFilterPunctuation = optionsMenu.addItem(
				"Filter punctuation", 
				mi -> queryResultGrid.getDataProvider().refreshAll());
		miFilterPunctuation.setCheckable(true);
		miFilterPunctuation.setChecked(true);
		punctuationFilter = new PunctuationFilter(() -> miFilterPunctuation.isChecked());
		
		if (resultPanelCloseListener != null) {
			removeBt.addClickListener(clickEvent -> resultPanelCloseListener.closeRequest(QueryResultPanel.this));
		}
		else {
			removeBt.setVisible(false);
		}
		
		searchField.addValueChangeListener(event -> handleSearchValueInput(event.getValue()));
	}

	private void handleSearchValueInput(String searchValue) {
		@SuppressWarnings("unchecked")
		TreeDataProvider<QueryResultRowItem> dataProvider = 
				(TreeDataProvider<QueryResultRowItem>) this.queryResultGrid.getDataProvider();
		if ((searchValue == null) || searchValue.isEmpty()) {
			dataProvider.setFilter(null);
		}
		else {
			dataProvider.setFilter(row -> row.startsWith(searchValue));
		}
		
		dataProvider.addFilter(punctuationFilter);
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

	public QueryResultPanelSetting getQueryResultPanelSetting() {
		return new QueryResultPanelSetting(queryId, queryResult, displaySetting);
	}
	
	public QueryResultRowArray getFilteredQueryResult() {
		QueryResultRowArray result = new QueryResultRowArray();
		@SuppressWarnings("unchecked")
		final TreeDataProvider<QueryResultRowItem> dataProvider = 
				((TreeDataProvider<QueryResultRowItem>) queryResultGrid.getDataProvider());
		dataProvider.fetch(
				new HierarchicalQuery<QueryResultRowItem, SerializablePredicate<QueryResultRowItem>>(null , null))
		.forEach(item -> result.addAll(item.getRows()));

		return result;
	}
	
	private List<QueryResultRowItem> getFilteredQueryResultRowItems() {
		@SuppressWarnings("unchecked")
		final TreeDataProvider<QueryResultRowItem> dataProvider = 
				((TreeDataProvider<QueryResultRowItem>) queryResultGrid.getDataProvider());
		return dataProvider.fetch(
				new HierarchicalQuery<QueryResultRowItem, SerializablePredicate<QueryResultRowItem>>(null , null))
		.collect(Collectors.toList());
	}

	public QueryId getQueryId() {
		return queryId;
	}

	public void addQueryResultRows(QueryResultRowArray rows) {
		@SuppressWarnings("unchecked")
		final TreeDataProvider<QueryResultRowItem> dataProvider = 
				((TreeDataProvider<QueryResultRowItem>) queryResultGrid.getDataProvider());
		boolean rowsAdded = false;
		for (QueryResultRow row : rows) {
			if (!((QueryResultRowArray)queryResult).contains(row)) {
				((QueryResultRowArray)queryResult).add(row);
				rowsAdded = true;
				// update existing items
				dataProvider.getTreeData().getRootItems().forEach(
					item -> item.addQueryResultRow(row, dataProvider.getTreeData(), kwicProviderCache));
			}
		};
		if (rowsAdded) {
			if (!dataProvider.getTreeData().equals(phraseBasedTreeData)) {
				phraseBasedTreeData = null;
			}
			if (!dataProvider.getTreeData().equals(tagBasedTreeData)) {
				tagBasedTreeData = null;
			}
			if (!dataProvider.getTreeData().equals(flatTagBasedTreeData)) {
				flatTagBasedTreeData = null;
			}
			if (!dataProvider.getTreeData().equals(propertiesAsColumnsTagBasedTreeData)) {
				propertiesAsColumnsTagBasedTreeData = null;
			}
		}
		
		// add new root items
		displaySetting.addQueryResultRootItems(this, rows);
		tokenCount = ((QueryResultRowArray)queryResult).size();
		dataProvider.refreshAll();
		
		if (!dataProvider.getTreeData().getRootItems().isEmpty() 
				&& queryResultGrid.getSelectedItems().isEmpty()) {
			queryResultGrid.getDataCommunicator().fetchItemsWithRange(0,1)
				.stream()
				.findFirst()
				.ifPresent(item -> 
					queryResultGrid.select(item));
		}
	}

	void addPhraseBasedRootItems(QueryResult result) {
		Set<GroupedQueryResult> groupedQueryResults = result.asGroupedSet();
		tokenCount = 0;
		for (GroupedQueryResult groupedQueryResult : groupedQueryResults) {
			PhraseQueryResultRowItem phraseQueryResultRowItem = 
					new PhraseQueryResultRowItem(includeQueryId, groupedQueryResult);
			if (!phraseBasedTreeData.contains(phraseQueryResultRowItem)) {
				tokenCount += groupedQueryResult.getTotalFrequency();
				phraseBasedTreeData.addItem(null, phraseQueryResultRowItem);
				phraseBasedTreeData.addItem(phraseQueryResultRowItem, new DummyQueryResultRowItem());
			}
		}
	}

	void addTagBasedRootItems(QueryResult result) {
		tokenCount = 0;
		Set<GroupedQueryResult> groupedQueryResults = result.asGroupedSet(row -> {
			if (row instanceof TagQueryResultRow) {
				if (((TagQueryResultRow) row).getPropertyDefinitionId() != null) {
					resultContainsProperties = true;
				}
				return ((TagQueryResultRow) row).getTagDefinitionPath();
			}
			return TagQueryResultRowItem.getNoTagAvailableKey();
		});
		for (GroupedQueryResult groupedQueryResult : groupedQueryResults) {
			TagQueryResultRowItem tagQueryResultRowItem = 
					new TagQueryResultRowItem(includeQueryId, groupedQueryResult, project);
			if (!tagBasedTreeData.contains(tagQueryResultRowItem)) {
				tokenCount += groupedQueryResult.getTotalFrequency();
				tagBasedTreeData.addItem(null, tagQueryResultRowItem);
				tagBasedTreeData.addItem(tagQueryResultRowItem, new DummyQueryResultRowItem());
			}
		}
	}
	
	void addFlatTagBasedRootItems(QueryResult result) {
		try {
			for (QueryResultRow row : result) {
				if (row instanceof TagQueryResultRow) {
					if (((TagQueryResultRow) row).getPropertyDefinitionId() != null) {
						resultContainsProperties = true;
					}

					TagQueryResultRow tRow = (TagQueryResultRow)row;
					
					KwicProvider kwicProvider = kwicProviderCache.get(row.getSourceDocumentId());
					TagDefinition tagDefinition = 
						project.getTagManager().getTagLibrary().getTagDefinition(tRow.getTagDefinitionId());
					KwicQueryResultRowItem item =
						new KwicQueryResultRowItem(
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
								.getName(),
							true
						);
					if (!flatTagBasedTreeData.contains(item)) {
						flatTagBasedTreeData.addItem(null, item);
					}
				}
			}
			
			tokenCount = flatTagBasedTreeData.getRootItems().size();
		}
		catch (Exception e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("error adding query result", e);
        }
	}

	void addPropertiesAsColumnsTagBasedRootItems(QueryResult result) {
		try {
			
			HashMap<String, QueryResultRowArray> rowsGroupedByTagInstance = 
					new HashMap<String, QueryResultRowArray>();
			for (QueryResultRow row : result) {
				
				if (row instanceof TagQueryResultRow) {
					TagQueryResultRow tRow = (TagQueryResultRow) row;
					QueryResultRowArray rows = 
							rowsGroupedByTagInstance.get(tRow.getTagInstanceId());
					
					if (rows == null) {
						rows = new QueryResultRowArray();
						rowsGroupedByTagInstance.put(tRow.getTagInstanceId(), rows);
					}
					rows.add(tRow);
					if (tRow.getPropertyName() != null) {
						propertyNames.add(tRow.getPropertyName());
					}
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
				KwicPropertiesAsColumnsQueryResultRowItem item = 
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
					);
				if (!propertiesAsColumnsTagBasedTreeData.contains(item)) {
					propertiesAsColumnsTagBasedTreeData.addItem(null, item);
				}
			}				
			
			tokenCount = propertiesAsColumnsTagBasedTreeData.getRootItems().size();
		}
		catch (Exception e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("error adding query result", e);
		}			
	}

	public void removeQueryResultRows(QueryResultRowArray rows) {

		@SuppressWarnings("unchecked")
		final TreeDataProvider<QueryResultRowItem> dataProvider = 
				((TreeDataProvider<QueryResultRowItem>) queryResultGrid.getDataProvider());
		
		if (((QueryResultRowArray)queryResult).removeAll(rows)) {
			tokenCount = ((QueryResultRowArray)queryResult).size();
			if (!dataProvider.getTreeData().equals(phraseBasedTreeData)) {
				phraseBasedTreeData = null;
			}
			if (!dataProvider.getTreeData().equals(tagBasedTreeData)) {
				tagBasedTreeData = null;
			}
			if (!dataProvider.getTreeData().equals(flatTagBasedTreeData)) {
				flatTagBasedTreeData = null;
			}
			if (!dataProvider.getTreeData().equals(propertiesAsColumnsTagBasedTreeData)) {
				propertiesAsColumnsTagBasedTreeData = null;
			}
		}
		
		new ArrayList<>(rows).forEach(row -> {
			// update existing items
			dataProvider.getTreeData().getRootItems().forEach(
				item -> item.removeQueryResultRow(row, dataProvider.getTreeData()));
		});
		
		new ArrayList<>(dataProvider.getTreeData().getRootItems()).forEach(item -> {
			if (item.getRows().isEmpty()) {
				dataProvider.getTreeData().removeItem(item);
			}
		});

		dataProvider.refreshAll();
	}

	public void addToButtonBarLeft(Component component) {
		buttonPanel.addComponent(component, 0);
	}
	
	public void setDisplaySettingChangeListener(DisplaySettingChangeListener displaySettingChangeListener) {
		this.displaySettingChangeListener = displaySettingChangeListener;
	}
	
	private void fireDisplaySettingChanged() {
		if (this.displaySettingChangeListener != null) {
			this.displaySettingChangeListener.displaySettingChanged(displaySetting);
		}
	}

	public DisplaySetting getDisplaySetting() {
		return displaySetting;
	}
	
	public void clear() {
		if (phraseBasedTreeData != null) {
			phraseBasedTreeData.clear();
		}
		if (tagBasedTreeData != null) {
			tagBasedTreeData.clear();
		}
		if (flatTagBasedTreeData != null) {
			flatTagBasedTreeData.clear();
		}
		if (propertiesAsColumnsTagBasedTreeData != null) {
			propertiesAsColumnsTagBasedTreeData.clear();
		}
		optionsBt.setEnabled(false);
		queryResultGrid.getDataProvider().refreshAll();
	}

	public MenuItem addOptionsMenuItem(String caption, Command command) {
		return optionsMenu.addItem(caption, command);
	}
	
	public boolean isEmpty() {
		return ((TreeDataProvider<QueryResultRowItem>) queryResultGrid.getDataProvider()).getTreeData().getRootItems()
				.isEmpty();
	}

	
}
