package de.catma.ui.analyzenew;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.antlr.runtime.RecognitionException;
import org.vaadin.sliderpanel.SliderPanel;
import org.vaadin.sliderpanel.SliderPanelBuilder;
import org.vaadin.sliderpanel.client.SliderMode;

import com.github.appreciated.material.MaterialTheme;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.ShortcutListener;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComboBox.NewItemHandler;
//import com.vaadin.ui.ComboBox.NewItemHandler;
import com.vaadin.ui.ComboBox.NewItemProvider;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Button.ClickShortcut;
import com.vaadin.ui.Notification.Type;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.Corpus;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.queryengine.QueryJob;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.QueryJob.QueryException;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;
import de.catma.ui.CatmaApplication;
import de.catma.ui.analyzenew.ResultPanelNew.ResultPanelCloseListener;
import de.catma.ui.analyzenew.resourcepanelanalyze.AnalyzeResourceSelectionListener;
import de.catma.ui.analyzenew.resourcepanelanalyze.CollectionDataItem;
import de.catma.ui.analyzenew.resourcepanelanalyze.DocumentTreeItem;
import de.catma.ui.analyzenew.resourcepanelanalyze.DocumentDataItem;
import de.catma.ui.analyzenew.resourcepanelanalyze.ResourcePanelAnalyze;
import de.catma.ui.analyzenew.treegridhelper.TreeRowItem;
import de.catma.ui.analyzer.GroupedQueryResultSelectionListener;
import de.catma.ui.analyzer.Messages;
import de.catma.ui.analyzer.RelevantUserMarkupCollectionProvider;
import de.catma.ui.analyzer.TagKwicResultsProvider;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.repository.MarkupCollectionItem;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.ui.tabbedview.TabComponent;

public class AnalyzeNewView extends HorizontalLayout
		implements ClosableTab, TabComponent, HasComponents {

	public static interface CloseListenerNew {
		public void closeRequest(AnalyzeNewView analyzeNewView);
	}
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private String userMarkupItemDisplayString = "Markup Collections";
	private IndexedRepository repository;
	private Corpus corpus;
	private List<String> relevantSourceDocumentIDs;
	private List<String> relevantUserMarkupCollIDs;
	private List<String> relevantStaticMarkupCollIDs;
	private List<String> predefQueries;
	private IndexInfoSet indexInfoSet;
	private Button btExecuteSearch;
	private Button btQueryBuilder;
	private Button kwicBt;
	private Button distBt;
	private Button wordCloudBt;
	private Button networkBt;
	private Button optionsBt;
	private String searchInput;
	private ComboBox<String> queryComboBox;
	private ResultPanelNew queryResultPanel;
	private HorizontalLayout resultAndMinMaxVizHorizontal;
	private VerticalLayout resultsPanel;
	private VerticalLayout minMaxPanel;
	private VerticalLayout searchPanel;
	private HorizontalLayout labelLayout;
	private VerticalLayout contentPanel;
	private HorizontalLayout searchRow;
	private HorizontalLayout searchAndVisIconsHorizontal;

	private Component visIconsPanel;
	private ResourcePanelAnalyze resourcePanelAnalyze;
	private SliderPanel drawer;
	private QueryOptions queryOptions;
	private boolean newItem= false;

	public AnalyzeNewView(Corpus corpus, IndexedRepository repository, CloseListenerNew closeListener)
			throws Exception {

		this.corpus = corpus;
		this.repository = repository;
		this.relevantSourceDocumentIDs = new ArrayList<String>();
		this.relevantUserMarkupCollIDs = new ArrayList<String>();
		this.relevantStaticMarkupCollIDs = new ArrayList<String>();
		this.indexInfoSet = new IndexInfoSet(Collections.<String>emptyList(), Collections.<Character>emptyList(),
				Locale.ENGLISH);
		
		 this.queryOptions = new QueryOptions(relevantSourceDocumentIDs, relevantUserMarkupCollIDs,
					relevantStaticMarkupCollIDs, indexInfoSet.getUnseparableCharacterSequences(),
					indexInfoSet.getUserDefinedSeparatingCharacters(), indexInfoSet.getLocale(), repository);

		initComponents();
		initListeners();
		initActions();
		addClickshortCuts();
	}

	private void initComponents() throws Exception {
		addRelevantResources();
		searchInput = "";
		searchPanel = (VerticalLayout) createSearchPanel();
		searchPanel.addStyleName("analyze_search_icon_bar");

		visIconsPanel = createVisIconsPanel();
		visIconsPanel.addStyleName("analyze_search_icon_bar");

		searchAndVisIconsHorizontal = new HorizontalLayout();
		searchAndVisIconsHorizontal.addStyleName("analyze_bar");
		searchAndVisIconsHorizontal.addComponents(searchPanel, visIconsPanel);

		resultAndMinMaxVizHorizontal = new HorizontalLayout();
		resultAndMinMaxVizHorizontal.addStyleName("analyze_results");

		resultsPanel = new VerticalLayout();
		resultsPanel.addStyleName("analyze_results_list");
		minMaxPanel = new VerticalLayout();
		minMaxPanel.addStyleName("analyze_results_minmax");

		resultAndMinMaxVizHorizontal.addComponents(resultsPanel, minMaxPanel);

		contentPanel = new VerticalLayout();
		contentPanel.addComponent(searchAndVisIconsHorizontal);
		contentPanel.addComponent(resultAndMinMaxVizHorizontal);
		contentPanel.addStyleName("analyze_content");

		this.addStyleName("analyze_content");
		
		resourcePanelAnalyze = new ResourcePanelAnalyze(this.repository, this.corpus); 
		
		drawer = new SliderPanelBuilder(resourcePanelAnalyze)
				.mode(SliderMode.LEFT).expanded(false).build();
		drawer.addStyleName("analyze_resourcePanel");
		
		addComponent(drawer);
		addComponent(contentPanel);
	}
	
	private Component createSearchPanel() {	
		searchPanel = new VerticalLayout();
		searchPanel.setAlignContent(AlignContent.CENTER);

		Label searchPanelLabel = new Label("Queries");
		searchPanelLabel.addStyleName("analyze_label");

		optionsBt = new Button("", VaadinIcons.ELLIPSIS_DOTS_V);
		optionsBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		optionsBt.addStyleName("analyze_options_bt");

	    labelLayout = new HorizontalLayout(searchPanelLabel, optionsBt);
		labelLayout.addStyleName("analyze_label_layout");

		btQueryBuilder = new Button(" + BUILD QUERY");
		btQueryBuilder.addStyleName("analyze_querybuilder_bt");

		predefQueries = new ArrayList<>();
		predefQueries.add("property= \"%\"");
		predefQueries.add("tag=\"%\"");
		predefQueries.add("wild= \"Blumen%\",tag=\"%\"");
		predefQueries.add("wild= \"%\"");
		predefQueries.add("freq>0");

		queryComboBox = new ComboBox<>();
		queryComboBox.setDataProvider(new ListDataProvider<>(predefQueries));
		queryComboBox.addStyleName("analyze_query_comobobox");
		queryComboBox.setEmptySelectionCaption("Select or enter a free query");

		btExecuteSearch = new Button("SEARCH", VaadinIcons.SEARCH);	
		btExecuteSearch.setStyleName("analyze_search_bt");
	
		searchRow = new HorizontalLayout();
		searchRow.addComponents(btQueryBuilder, queryComboBox);
		searchRow.addStyleName("analyze_search_row");
		
		VerticalLayout searchVerticalLayout = new VerticalLayout(searchRow, btExecuteSearch);
		searchVerticalLayout.addStyleName("analyze_search");

		searchPanel.addComponents(labelLayout, searchVerticalLayout);
		return searchPanel;
	}

	private Component createVisIconsPanel() {		
		VerticalLayout visIconsPanel = new VerticalLayout();
		visIconsPanel.setAlignContent(AlignContent.CENTER);
		Label visIconsLabel = new Label("Visualisations");
		visIconsLabel.addStyleName("analyze_label");

		optionsBt = new Button("", VaadinIcons.ELLIPSIS_DOTS_V);
		optionsBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		optionsBt.addStyleName("analyze_options_bt");

		HorizontalLayout labelLayout = new HorizontalLayout(visIconsLabel, optionsBt);
		labelLayout.addStyleName("analyze_label_layout");
		HorizontalLayout visIconBar = new HorizontalLayout();

		kwicBt = new Button("KWIC", VaadinIcons.SPLIT);
		kwicBt.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
		kwicBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		kwicBt.addStyleName("analyze_iconbar_icon");
		kwicBt.setWidth("100%");
		kwicBt.setHeight("100%");

		distBt = new Button("DISTRIBUTION", VaadinIcons.CHART_LINE);
		distBt.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
		distBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		distBt.addStyleName("analyze_iconbar_icon");
		distBt.setWidth("100%");
		distBt.setHeight("100%");

		wordCloudBt = new Button("WORDCLOUD", VaadinIcons.CLOUD);
		wordCloudBt.addStyleName(MaterialTheme.BUTTON_ICON_ALIGN_TOP);
		wordCloudBt.addStyleName(MaterialTheme.BUTTON_BORDERLESS);
		wordCloudBt.addStyleName("analyze_iconbar_icon");
		wordCloudBt.setWidth("100%");
		wordCloudBt.setHeight("100%");

		networkBt = new Button("NETWORK", VaadinIcons.CLUSTER);
		networkBt.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
		networkBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		networkBt.	addStyleName("analyze_iconbar_icon");

		networkBt.setWidth("100%");
		networkBt.setHeight("100%");

		visIconBar.addComponents(kwicBt, distBt, wordCloudBt, networkBt);

		visIconsPanel.addComponent(labelLayout);
		visIconsPanel.addComponent(visIconBar);
		return visIconsPanel;
	}
	
	private void setContent(Component component) {	
		removeAllComponents();		
		addComponent(component);	
		component.setHeight("100%");
		component.setWidth("100%");
	}
	
	private void setResourcesSlider() {
		addComponentAsFirst(drawer);
	}
	
	private void initActions() {
		
		btExecuteSearch.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				if(newItem) {
					queryComboBox.setValue(searchInput);
					executeSearch();			
				}else {
					searchInput= queryComboBox.getValue();	
					executeSearch();	
				}			
			}
		});

		kwicBt.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {

				VizSnapshot kwicSnapshot = new VizSnapshot("Kwic Visualisation");
				KwicVizPanelNew kwic = new KwicVizPanelNew(getAllTreeGridDatas(), repository);
				kwicSnapshot.setKwicVizPanel(kwic);
				kwicSnapshot.setEditVizSnapshotListener(buildEditVizSnapshotListener(kwic));
				kwicSnapshot.setDeleteVizSnapshotListener(buildDeleteVizSnapshotListener(kwicSnapshot));
				minMaxPanel.addComponent(kwicSnapshot);

				kwic.setLeaveViewListener(new CloseVizViewListener() {

					@Override
					public void onClose() {
						
						setContent(contentPanel);
						setResourcesSlider();		
					}
				});

				setContent(kwic);
			}
		});
		
		resourcePanelAnalyze.setSelectionListener(new AnalyzeResourceSelectionListener() {	

			@Override
			public void updateQueryOptions(TreeGrid<DocumentTreeItem> treeGrid) {
				updateCorpusAndQueryOptions(treeGrid);
				
			}		
		});
	}
	

	private void addRelevantResources() throws Exception {		
		this.queryOptions.getRelevantSourceDocumentIDs().clear();
		this.queryOptions.getRelevantUserMarkupCollIDs().clear();
		this.indexInfoSet = new IndexInfoSet();

		if (corpus != null) {
			for (SourceDocument sd : corpus.getSourceDocuments()) {
				addSourceDocument(sd);
			}
		} else {
			for (SourceDocument sd : repository.getSourceDocuments()) {
				addSourceDocument(sd);
			}

		}

	}

	private void initListeners() {

		
		queryComboBox.setNewItemHandler(new NewItemHandler() {
			public void accept(String t) {
				newItem = true;
			searchInput = t;
			btExecuteSearch.click();
			
			}
		});
		

		
/*	 this newItemProvider provider ,even though is the newer one  is triggered too late...
		queryComboBox.setNewItemProvider(inputString -> {

			searchInput= inputString;
			queryComboBox.setValue(inputString);
		
		    return Optional.of(inputString);
		
		});*/
		
/* 	
 	queryComboBox.addShortcutListener(new ShortcutListener() {
			
			@Override
			public void handleAction(Object sender, Object target) {
				// TODO Auto-generated method stub
				
			}
		});*/

	}



	private void executeSearch() {
	
		newItem= false;

		QueryJob job = new QueryJob(searchInput.toString(), queryOptions);

		((BackgroundServiceProvider) UI.getCurrent()).submit(Messages.getString("AnalyzerView.Searching"), //$NON-NLS-1$
				job, new ExecutionListener<QueryResult>() {
					public void done(QueryResult result) {

						try {
							queryResultPanel = new ResultPanelNew(repository, result,
									"result for query: " + searchInput.toString(), new ResultPanelCloseListener() {

										@Override
										public void closeRequest(ResultPanelNew queryResultPanel) {
											resultsPanel.removeComponent(queryResultPanel);

										}
									});
						} catch (Exception e) {
							
							e.printStackTrace();
						}

						queryResultPanel.setWidth("100%");
						resultsPanel.addComponentAsFirst(queryResultPanel);

					};

					public void error(Throwable t) {

						if (t instanceof QueryException) {
							QueryJob.QueryException qe = (QueryJob.QueryException) t;
							String input = qe.getInput();
							int idx = ((RecognitionException) qe.getCause()).charPositionInLine;
							if ((idx >= 0) && (input.length() > idx)) {
								char character = input.charAt(idx);
								String message = MessageFormat.format(
										Messages.getString("AnalyzerView.queryFormatError"), //$NON-NLS-1$
										input, idx + 1, character);
								HTMLNotification.show(Messages.getString("AnalyzerView.InfoTitle"), message, //$NON-NLS-1$
										Type.TRAY_NOTIFICATION);
							} else {
								String message = MessageFormat.format(
										Messages.getString("AnalyzerView.generalQueryFormatError"), //$NON-NLS-1$
										input);
								HTMLNotification.show(Messages.getString("AnalyzerView.InfoTitle"), message, //$NON-NLS-1$
										Type.TRAY_NOTIFICATION);
							}
						} else {
							((CatmaApplication) UI.getCurrent())
									.showAndLogError(Messages.getString("AnalyzerView.errorDuringSearch"), t); //$NON-NLS-1$
						}
					}
				});

	}

	private ArrayList<CurrentTreeGridData> getAllTreeGridDatas() {
		
		Iterator<Component> iterator = resultsPanel.getComponentIterator();
		ArrayList<CurrentTreeGridData> toReturnList = new ArrayList<CurrentTreeGridData>();
		while (iterator.hasNext()) {
			ResultPanelNew onePanel = (ResultPanelNew) iterator.next();
			CurrentTreeGridData current = new CurrentTreeGridData(onePanel.getQueryAsString(),
					(TreeData<TreeRowItem>) onePanel.getCurrentTreeGridData(), onePanel.getCurrentView());
			toReturnList.add(current);
		}
		return toReturnList;
	}

	private void addSourceDocument(SourceDocument sd) {
	
		this.relevantSourceDocumentIDs.add(sd.getID());

		indexInfoSet = sd.getSourceContentHandler().getSourceDocumentInfo().getIndexInfoSet();
		MarkupCollectionItem umc = new MarkupCollectionItem(sd, userMarkupItemDisplayString, true);
		
		for (UserMarkupCollectionReference umcRef : sd.getUserMarkupCollectionRefs()) {
			if (corpus.getUserMarkupCollectionRefs().contains(umcRef)) {
				addUserMarkupCollection(umcRef, umc);
			}
		}
	}

	private void addUserMarkupCollection(UserMarkupCollectionReference umcRef, MarkupCollectionItem umc) {
		this.relevantUserMarkupCollIDs.add(umcRef.getId());
	}
	
	private EditVizSnapshotListener buildEditVizSnapshotListener(Component component) {
		return new EditVizSnapshotListener() {
		
			public void reopenKwicView() {
				setContent(component);

			}
		};
	}

	private DeleteVizSnapshotListener buildDeleteVizSnapshotListener(Component component) {
		return new DeleteVizSnapshotListener() {

			@Override
			public void deleteSnapshot() {
				minMaxPanel.removeComponent(component);

			}
		};
	}
	
	private void updateCorpusAndQueryOptions(TreeGrid<DocumentTreeItem> treeGrid) {
		this.corpus = new Corpus("new Corpus");

		Set<DocumentTreeItem> selecteItems = treeGrid.getSelectedItems();

		for (DocumentTreeItem documentTreeItem : selecteItems) {
			if (documentTreeItem.getClass()==DocumentDataItem.class) {

				DocumentDataItem documentDataItem = (DocumentDataItem) documentTreeItem;
				this.corpus.addSourceDocument(documentDataItem.getDocument());
			}
			if (documentTreeItem.getClass()==CollectionDataItem.class) {
				CollectionDataItem collectionDataItem = (CollectionDataItem) documentTreeItem;
				this.corpus.addUserMarkupCollectionReference(collectionDataItem.getCollectionRef());
			}
		}
		try {
			addRelevantResources();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "error  updating query options", e); 
			e.printStackTrace();
		}	
	}


	public Corpus getCorpus() {
		return corpus;
	}

	@Override
	public void addClickshortCuts() {
		btExecuteSearch.setClickShortcut(KeyCode.ENTER);	
	}

	@Override
	public void removeClickshortCuts() {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

}
