package de.catma.ui.module.analyze;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import org.antlr.runtime.RecognitionException;
import org.vaadin.sliderpanel.SliderPanel;
import org.vaadin.sliderpanel.SliderPanelBuilder;
import org.vaadin.sliderpanel.client.SliderMode;

import com.github.appreciated.material.MaterialTheme;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.EventBus;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ClassResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.corpus.Corpus;
import de.catma.document.source.IndexInfoSet;
import de.catma.indexer.IndexedProject;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.QueryId;
import de.catma.queryengine.QueryJob;
import de.catma.queryengine.QueryJob.QueryException;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.querybuilder.QueryTree;
import de.catma.queryengine.result.QueryResult;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.tabbedview.ClosableTab;
import de.catma.ui.component.tabbedview.TabCaptionChangeListener;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.wizard.WizardContext;
import de.catma.ui.module.analyze.querybuilder.QueryBuilder;
import de.catma.ui.module.analyze.queryresultpanel.QueryResultPanel;
import de.catma.ui.module.analyze.queryresultpanel.QueryResultPanelSetting;
import de.catma.ui.module.analyze.queryresultpanel.RefreshQueryResultPanel;
import de.catma.ui.module.analyze.resourcepanel.AnalyzeResourcePanel;
import de.catma.ui.module.analyze.visualization.doubletree.DoubleTreePanel;
import de.catma.ui.module.analyze.visualization.doubletree.DoubleTreePanel;
import de.catma.ui.module.analyze.visualization.kwic.KwicPanel;
import de.catma.ui.module.analyze.visualization.vega.DistributionDisplaySettingHandler;
import de.catma.ui.module.analyze.visualization.vega.VegaPanel;
import de.catma.ui.module.analyze.visualization.vega.WordCloudDisplaySettingHandler;
import de.catma.ui.module.main.ErrorHandler;

public class AnalyzeView extends HorizontalLayout
		implements ClosableTab {
	
	private static final class NamedQuery {
		private final String name;
		private final String query;
		private boolean hideName;
		
		public NamedQuery(String query) {
			this(query, query, true);
		}		
		
		public NamedQuery(String name, String query) {
			this(name, query, false);
		}
		
		public NamedQuery(String name, String query, boolean hideName) {
			super();
			this.hideName = hideName;
			this.name = name;
			this.query = query;
		}

		public String getQuery() {
			return query;
		}
		
		@Override
		public String toString() {
			return hideName?this.query:(this.name + " (" + this.query + ")");
		}
	}

	private IndexedProject project;
	private LoadingCache<String, KwicProvider> kwicProviderCache;
	
	private Corpus currentCorpus;
	private List<NamedQuery> queryProposals;
	
	private Button btExecuteSearch;
	private Button btQueryBuilder;
	private Button kwicBt;
	private Button distBt;
	private Button wordCloudBt;
	private Button doubleTreeBt;
	private Button btQueryOptions;
	private Button btVizOptions;
	
	private ComboBox<NamedQuery> queryBox;
	
	private VerticalLayout resultsPanel;
	private VerticalLayout vizCardsPanel;
	private HorizontalLayout contentPanel;

	private AnalyzeResourcePanel analyzeResourcePanel;
	private SliderPanel drawer;
	private IndexInfoSet indexInfoSet;
	private TabCaptionChangeListener tabCaptionChangeListener;
	private AnalyzeCaption analyzeCaption;
	
	private EventBus eventBus;
	private ProgressBar progressBar;

	public AnalyzeView(
			Corpus corpus, 
			IndexedProject project, EventBus eventBus) {
		this.eventBus = eventBus;
		this.project = project;
		this.kwicProviderCache = KwicProvider.buildKwicProviderByDocumentIdCache(project);
		
		this.indexInfoSet = new IndexInfoSet(Collections.<String>emptyList(), Collections.<Character>emptyList(),
				Locale.ENGLISH);
		this.analyzeCaption = new AnalyzeCaption(corpus);
		initComponents(corpus);
		initActions();
		
		corpusChanged();

	}
	
	@Override
	public String getCaption() {
		return analyzeCaption.getCaption();
	}

	private void initComponents(Corpus corpus) {
		setSizeFull();
		setSpacing(true);
		

		// left column Queries
		
		VerticalLayout queryPanel = new VerticalLayout();
		queryPanel.setSizeFull();

		Label searchPanelLabel = new Label("Queries");
		
		btQueryOptions = new IconButton(VaadinIcons.ELLIPSIS_DOTS_V);
		btQueryOptions.setVisible(false); // TODO: no query options so far
		
	    HorizontalLayout queryHeaderPanel = new HorizontalLayout(searchPanelLabel, btQueryOptions);
	    queryHeaderPanel.setWidth("100%");

	    queryHeaderPanel.setExpandRatio(searchPanelLabel, 1.0f);
	    queryHeaderPanel.setComponentAlignment(searchPanelLabel, Alignment.MIDDLE_CENTER);
	    queryHeaderPanel.setComponentAlignment(btQueryOptions, Alignment.MIDDLE_RIGHT);
	    queryPanel.addComponent(queryHeaderPanel);
	    
		VerticalLayout searchPanel = createSearchPanel();
		queryPanel.addComponent(searchPanel);
		
		
		resultsPanel = new VerticalLayout();
		resultsPanel.setMargin(new MarginInfo(false, true, false, false));
		resultsPanel.setWidth("100%");

		Panel resultsScrollPanel = new Panel();
		resultsScrollPanel.setSizeFull();
		resultsScrollPanel.addStyleName(MaterialTheme.PANEL_BORDERLESS);
		
		resultsScrollPanel.setContent(resultsPanel);
		
		queryPanel.addComponent(resultsScrollPanel);
		queryPanel.setExpandRatio(resultsScrollPanel, 1f);
		
		
		// right column Visualizations
		
		VerticalLayout vizPanel = new VerticalLayout();
		vizPanel.setSizeFull();
		
		Label vizPanelLabel = new Label("Visualisations");

		btVizOptions = new IconButton(VaadinIcons.ELLIPSIS_DOTS_V);
		btVizOptions.setVisible(false); // TODO: no viz options so far

	    HorizontalLayout vizHeaderPanel = new HorizontalLayout(vizPanelLabel, btVizOptions);
	    vizHeaderPanel.setWidth("100%");
	    vizHeaderPanel.setExpandRatio(vizPanelLabel, 1.0f);
	    vizHeaderPanel.setComponentAlignment(vizPanelLabel, Alignment.MIDDLE_CENTER);
	    vizHeaderPanel.setComponentAlignment(btVizOptions, Alignment.MIDDLE_RIGHT);
	    vizPanel.addComponent(vizHeaderPanel);
		
		HorizontalLayout vizIconsPanel = createVizIconsPanel();
		vizIconsPanel.setWidth("100%");
		vizPanel.addComponent(vizIconsPanel);
		
		vizCardsPanel = new VerticalLayout();
		vizCardsPanel.setWidth("100%");
		
		Panel vizCardsScrollPanel = new Panel();
		vizCardsScrollPanel.setSizeFull();
		vizCardsScrollPanel.addStyleName(MaterialTheme.PANEL_BORDERLESS);
		
		vizCardsScrollPanel.setContent(vizCardsPanel);

		vizPanel.addComponent(vizCardsScrollPanel);
		vizPanel.setExpandRatio(vizCardsScrollPanel, 1.0f);
		
		
		// drawer
		
		analyzeResourcePanel = new AnalyzeResourcePanel(
				this.eventBus,
				this.project, 
				corpus,
				() -> corpusChanged()); 
		drawer = new SliderPanelBuilder(analyzeResourcePanel)
				.mode(SliderMode.LEFT).expanded(corpus.isEmpty()).build();
		
		addComponent(drawer);

		
		// content
		contentPanel = new HorizontalLayout();
		contentPanel.setSpacing(false);
		contentPanel.setMargin(false);
		contentPanel.setSizeFull();
		
		contentPanel.addComponent(queryPanel);
		contentPanel.setExpandRatio(queryPanel, 0.5f);
		contentPanel.addComponent(vizPanel);
		contentPanel.setExpandRatio(vizPanel, 0.5f);
		addComponent(contentPanel);
		setExpandRatio(contentPanel, 1f);
	}
	
	private void corpusChanged() {

		Corpus corpus = analyzeResourcePanel.getCorpus();
		
		if (!corpus.isEmpty()) {
			
			//TODO: provide a facility where the user can select between different IndexInfoSets -> AnalyzeResourcePanel
			indexInfoSet = corpus.getSourceDocuments().get(0).getSourceContentHandler().getSourceDocumentInfo().getIndexInfoSet();
			btQueryBuilder.setEnabled(true);
			btExecuteSearch.setEnabled(true);
		}
		else {
			btQueryBuilder.setEnabled(false);
			btExecuteSearch.setEnabled(false);
		}
		
		currentCorpus = corpus;
		
		if (!analyzeCaption.isSetManually()) {
			analyzeCaption.setCaption(currentCorpus);
			if (tabCaptionChangeListener != null) {
				tabCaptionChangeListener.tabCaptionChange(this);
			}
		}
		
		for (int i=0; i<resultsPanel.getComponentCount(); i++) {
			Component component = resultsPanel.getComponent(i);
			if (component instanceof QueryResultPanel) {
			    QueryResultPanel queryResultPanel = (QueryResultPanel)component;
			    handleMarkAsStale(queryResultPanel);
			}
		}
	}

	private VerticalLayout createSearchPanel() {	
		VerticalLayout searchPanel = new VerticalLayout();
		searchPanel.setWidth("100%");
		searchPanel.addStyleName("analyze-search-panel");
		searchPanel.setMargin(new MarginInfo(false, true, true, false));

		btQueryBuilder = new Button("Build Query");

		queryProposals = new ArrayList<>();
		queryProposals.add(new NamedQuery("Wordlist", "freq>0"));
		queryProposals.add(new NamedQuery("Wildcard 'a'", "wild = \"a%\""));
		queryProposals.add(new NamedQuery("Taglist", "tag=\"%\""));
		queryProposals.add(new NamedQuery("Taglist with Properties", "property=\"%\""));

		queryBox = new ComboBox<>();
		queryBox.setDataProvider(new ListDataProvider<>(queryProposals));
		queryBox.setPlaceholder("Select or enter a free query");
		queryBox.setWidth("100%");
		
		progressBar = new ProgressBar();
		progressBar.setIndeterminate(false);
		progressBar.setVisible(false);
		
		btExecuteSearch = new Button("Search", VaadinIcons.SEARCH);	
		btExecuteSearch.addStyleName(MaterialTheme.BUTTON_PRIMARY);
		btExecuteSearch.setWidth("100%");
		
		HorizontalLayout queryPanel = new HorizontalLayout();
		queryPanel.setSizeFull();
		queryPanel.addComponents(btQueryBuilder, queryBox, progressBar);
		queryPanel.setExpandRatio(queryBox, 1.0f);
		
		searchPanel.addComponents(queryPanel, btExecuteSearch);

		return searchPanel;
	}

	private HorizontalLayout createVizIconsPanel() {	
		
	/*	kwicBt = new Button("Kwic", VaadinIcons.TABLE);
		kwicBt.addStyleName(MaterialTheme.BUTTON_ICON_ALIGN_TOP);
		kwicBt.addStyleName(MaterialTheme.BUTTON_BORDERLESS);
		kwicBt.addStyleName("analyze_viz_icon");
		kwicBt.setSizeFull();*/
		
		kwicBt = new Button("");
		kwicBt.setIcon(new ThemeResource("kwic_xs.png"));
		kwicBt.addStyleName(MaterialTheme.BUTTON_ICON_ALIGN_TOP);
		kwicBt.addStyleName(MaterialTheme.BUTTON_BORDERLESS);
		kwicBt.addStyleName("analyze_viz_icon");
		kwicBt.setSizeFull();

		distBt = new Button("");
		distBt.setIcon(new ThemeResource("distribution_xs.png"));
		distBt.addStyleName(MaterialTheme.BUTTON_ICON_ALIGN_TOP);
		distBt.addStyleName(MaterialTheme.BUTTON_BORDERLESS);
		distBt.addStyleName("analyze_viz_icon");
		distBt.setSizeFull();

		wordCloudBt = new Button("");
		wordCloudBt.setIcon(new ThemeResource("wordcloud_xs.png"));
		wordCloudBt.addStyleName(MaterialTheme.BUTTON_ICON_ALIGN_TOP);
		wordCloudBt.addStyleName(MaterialTheme.BUTTON_BORDERLESS);
		wordCloudBt.addStyleName("analyze_viz_icon");
		//wordCloudBt.setSizeFull();

		doubleTreeBt = new Button("");
		doubleTreeBt.setIcon(new ThemeResource("doubletree_xs.png"));
		doubleTreeBt.addStyleName(MaterialTheme.BUTTON_ICON_ALIGN_TOP);
		doubleTreeBt.addStyleName(MaterialTheme.BUTTON_BORDERLESS);
		doubleTreeBt.addStyleName("analyze_viz_icon");
		doubleTreeBt.setSizeFull();

		return new HorizontalLayout(kwicBt, distBt, wordCloudBt, doubleTreeBt);
	}
	
	private void setContent(Component newContent, Component oldContent) {	
		replaceComponent(oldContent, newContent);		
	}
	
	private void initActions() {
		queryBox.setNewItemProvider(inputString -> {
		    return Optional.of(new NamedQuery(inputString));
		});
		
		btExecuteSearch.addClickListener(clickEvent -> executeSearch());	
		queryBox.addValueChangeListener(valueChange -> btExecuteSearch.click());
//		queryBox.addFocusListener(event -> queryBox.setValue(null));
		
		kwicBt.addClickListener(event -> addKwicViz());

		distBt.addClickListener(event -> addDistViz());
		
		wordCloudBt.addClickListener(event -> addWCViz());
		
		doubleTreeBt.addClickListener(event -> addDoubleTreeViz());
		
		btQueryBuilder.addClickListener(clickEvent -> showQueryBuilder());
	}

	private void showQueryBuilder() {
		WizardContext wizardContext = new WizardContext();
		wizardContext.put(QueryBuilder.ContextKey.QUERY_TREE, new QueryTree());
		
		QueryBuilder queryBuilder = new QueryBuilder(
			project, 
			wizardContext, 
			new SaveCancelListener<WizardContext>() {
				@Override
				public void savePressed(WizardContext result) {
					QueryTree queryTree = (QueryTree) result.get(QueryBuilder.ContextKey.QUERY_TREE);
					String query = queryTree.toString();
					queryBox.setValue(new NamedQuery(query));
				}
		});
		queryBuilder.show();
	}

	private void addKwicViz() {
		if (getQueryResultPanelSettings().isEmpty()) {
			Notification.show("Info", "Please query some data first!", Type.HUMANIZED_MESSAGE);
			return;
		}
		String name = 
				"KWIC - KeyWord In Context " 
						+ LocalDateTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM));
		VizMaxPanel vizMaxPanel = 
				new VizMaxPanel(
						name,
						new KwicPanel(
								eventBus,
								project, 
								kwicProviderCache),
						getQueryResultPanelSettings(),
						project, 
						kwicProviderCache,
						closedVizMaxPanel -> setContent(contentPanel, closedVizMaxPanel));
		
		VizMinPanel vizMinPanel = 
				new VizMinPanel(
					name, 
					vizMaxPanel,
					toBeRemovedVizMinPanel -> vizCardsPanel.removeComponent(toBeRemovedVizMinPanel),
					() -> setContent(vizMaxPanel, contentPanel));
		

		vizCardsPanel.addComponent(vizMinPanel);
		setContent(vizMaxPanel, contentPanel);
	}

	private void addDistViz() {
		if (getQueryResultPanelSettings().isEmpty()) {
			Notification.show("Info", "Please query some data first!", Type.HUMANIZED_MESSAGE);
			return;
		}
		VegaPanel vegaPanel = 
			new VegaPanel(
				eventBus,
				project, 
				kwicProviderCache, 
				() -> new QueryOptions(
					new QueryId(""), //TODO: ok?
					currentCorpus.getDocumentIds(), 
					currentCorpus.getCollectionIds(),
					indexInfoSet.getUnseparableCharacterSequences(),
					indexInfoSet.getUserDefinedSeparatingCharacters(), indexInfoSet.getLocale(), project),
				new DistributionDisplaySettingHandler());
		String name = 
				"Distribution Chart " 
						+ LocalDateTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM));
		VizMaxPanel vizMaxPanel = 
			new VizMaxPanel(
					name,
					vegaPanel,
					getQueryResultPanelSettings(),
					project, 
					kwicProviderCache,
					closedVizMaxPanel -> setContent(contentPanel, closedVizMaxPanel));
		
		VizMinPanel vizMinPanel = 
				new VizMinPanel(
					name, 
					vizMaxPanel,
					toBeRemovedVizMinPanel -> vizCardsPanel.removeComponent(toBeRemovedVizMinPanel),
					() -> setContent(vizMaxPanel, contentPanel));
		

		vizCardsPanel.addComponent(vizMinPanel);
		setContent(vizMaxPanel, contentPanel);
	}
	
	private void addWCViz() {
		if (getQueryResultPanelSettings().isEmpty()) {
			Notification.show("Info", "Please query some data first!", Type.HUMANIZED_MESSAGE);
			return;
		}
		VegaPanel vegaPanel = 
			new VegaPanel(
				eventBus,
				project, 
				kwicProviderCache, 
				() -> new QueryOptions(
					new QueryId(""), //TODO: ok?
					currentCorpus.getDocumentIds(), 
					currentCorpus.getCollectionIds(),
					indexInfoSet.getUnseparableCharacterSequences(),
					indexInfoSet.getUserDefinedSeparatingCharacters(), indexInfoSet.getLocale(), project),
				new WordCloudDisplaySettingHandler());
		
		String name = 
				"Wordcloud " 
						+ LocalDateTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM));
		
		VizMaxPanel vizMaxPanel = 
			new VizMaxPanel(
					name,
					vegaPanel,
					getQueryResultPanelSettings(),
					project, 
					kwicProviderCache,
					closedVizMaxPanel -> setContent(contentPanel, closedVizMaxPanel));
		
		VizMinPanel vizMinPanel = 
				new VizMinPanel(
					"WordCloud", 
					vizMaxPanel,
					toBeRemovedVizMinPanel -> vizCardsPanel.removeComponent(toBeRemovedVizMinPanel),
					() -> setContent(vizMaxPanel, contentPanel));
		

		vizCardsPanel.addComponent(vizMinPanel);
		setContent(vizMaxPanel, contentPanel);
	}
	
	private void addDoubleTreeViz() {
		if (getQueryResultPanelSettings().isEmpty()) {
			Notification.show("Info", "Please query some data first!", Type.HUMANIZED_MESSAGE);
			return;
		}
		DoubleTreePanel doubleTreePanel = 
			new DoubleTreePanel(	kwicProviderCache);
		
		
		String name = 
				"Doubletree " 
						+ LocalDateTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM));
		
		VizMaxPanel vizMaxPanel = 
			new VizMaxPanel(
					name,
					doubleTreePanel,
					getQueryResultPanelSettings(),
					project, 
					kwicProviderCache,
					closedVizMaxPanel -> setContent(contentPanel, closedVizMaxPanel));
		
		VizMinPanel vizMinPanel = 
				new VizMinPanel(
					"Doubletree", 
					vizMaxPanel,
					toBeRemovedVizMinPanel -> vizCardsPanel.removeComponent(toBeRemovedVizMinPanel),
					() -> setContent(vizMaxPanel, contentPanel));
		

		vizCardsPanel.addComponent(vizMinPanel);
		setContent(vizMaxPanel, contentPanel);
	}

	private List<QueryResultPanelSetting> getQueryResultPanelSettings() {
		List<QueryResultPanelSetting> settings = new ArrayList<QueryResultPanelSetting>();
		for (Iterator<Component> iter = resultsPanel.iterator(); iter.hasNext(); ) {
			Component component = iter.next();
			if (component instanceof QueryResultPanel) {
			    QueryResultPanel queryResultPanel = (QueryResultPanel)component;
			    settings.add(queryResultPanel.getQueryResultPanelSetting());
			}
		}
		    
		return settings;
	}
	
	private void showProgress(boolean visible) {
		progressBar.setVisible(visible);
		progressBar.setIndeterminate(visible);
		btExecuteSearch.setEnabled(!visible);
		btQueryBuilder.setEnabled(!visible);
	}
	
	private void executeSearch() {
		NamedQuery namedQuery = queryBox.getValue();
		if (namedQuery != null) {
			kwicBt.focus();
			String searchInput = namedQuery.getQuery();
			
			if (searchInput == null || searchInput.trim().isEmpty()) {
				Notification.show("Info", "Please enter or select a query first!", Type.HUMANIZED_MESSAGE);
				return;
			}
			executeSearch(searchInput, queryResultPanel -> resultsPanel.addComponentAsFirst(queryResultPanel));
		}
	}

	private void executeSearch(String searchInput, Consumer<QueryResultPanel> addToLayoutFunction) {
		QueryOptions queryOptions = new QueryOptions(
				new QueryId(searchInput),
				currentCorpus.getDocumentIds(), 
				currentCorpus.getCollectionIds(),
				indexInfoSet.getUnseparableCharacterSequences(),
				indexInfoSet.getUserDefinedSeparatingCharacters(), indexInfoSet.getLocale(), project);
		QueryJob job = new QueryJob(searchInput, queryOptions);
		
		showProgress(true);
		
		((BackgroundServiceProvider) UI.getCurrent()).submit("Searching...",
				job, new ExecutionListener<QueryResult>() {
					public void done(QueryResult result) {
						try {
							QueryResultPanel queryResultPanel = new QueryResultPanel(project, result,
									new QueryId(searchInput.toString()),
									kwicProviderCache, 
									closingPanel -> handleRemoveQueryResultPanel(closingPanel));
							
							addToLayoutFunction.accept(queryResultPanel);
							addQueryResultPanelSetting(queryResultPanel.getQueryResultPanelSetting());
						}
						finally {
							showProgress(false);
						}
					};

					public void error(Throwable t) {
						showProgress(false);

						if (t instanceof QueryException) {
							QueryJob.QueryException qe = (QueryJob.QueryException) t;
							String input = qe.getInput();
							int idx = ((RecognitionException) qe.getCause()).charPositionInLine;
							if ((idx >= 0) && (input.length() > idx)) {
								char character = input.charAt(idx);
								String message = MessageFormat.format(
										"<html><p>There is something wrong with your query <b>{0}</b> approximately at positon {1} character <b>{2}</b>.</p> <p>If you are unsure about how to construct a query try the Query Builder!</p></html>",
										input, idx + 1, character);
								HTMLNotification.show("Info", message, 
										Type.TRAY_NOTIFICATION);
							} else {
								String message = MessageFormat.format(
										"<html><p>There is something wrong with your query <b>{0}</b>.</p> <p>If you are unsure about how to construct a query try the Query Builder!</p></html>",
										input);
								HTMLNotification.show("Info", message, 
										Type.TRAY_NOTIFICATION);
							}
						} else {
							((ErrorHandler) UI.getCurrent())
									.showAndLogError("Error during search!", t);
						}
					}
				});

	}

	private void handleRemoveQueryResultPanel(QueryResultPanel queryResultPanel) {
		resultsPanel.removeComponent(queryResultPanel);
		removeQueryResultPanelSetting(queryResultPanel.getQueryResultPanelSetting());
	}
	
	private void handleMarkAsStale(QueryResultPanel queryResultPanel) {
		RefreshQueryResultPanel refreshQueryResultPanel = new RefreshQueryResultPanel(queryResultPanel.getQueryId());
		resultsPanel.replaceComponent(queryResultPanel, refreshQueryResultPanel);
		refreshQueryResultPanel.addRemoveClickListener(clickEvent -> resultsPanel.removeComponent(refreshQueryResultPanel));
		refreshQueryResultPanel.addRefreshClickListener(clickEvent -> handleRefreshQueryResultPanel(refreshQueryResultPanel));
		removeQueryResultPanelSetting(queryResultPanel.getQueryResultPanelSetting());
	}

	private void handleRefreshQueryResultPanel(RefreshQueryResultPanel refreshQueryResultPanel) {
		QueryId queryId = refreshQueryResultPanel.getQueryId();
		
		executeSearch(
			queryId.getQuery(), 
			queryResultPanel -> resultsPanel.replaceComponent(refreshQueryResultPanel, queryResultPanel));
	}

	private void removeQueryResultPanelSetting(QueryResultPanelSetting queryResultPanelSetting) {
		for (Iterator<Component> compIter=vizCardsPanel.iterator(); compIter.hasNext();) {
			VizMinPanel vizMinPanel = (VizMinPanel)compIter.next();
			vizMinPanel.removeQueryResultPanelSetting(queryResultPanelSetting);
		}
	}

	private void addQueryResultPanelSetting(QueryResultPanelSetting queryResultPanelSetting) {
		for (Iterator<Component> compIter=vizCardsPanel.iterator(); compIter.hasNext();) {
			VizMinPanel vizMinPanel = (VizMinPanel)compIter.next();
			vizMinPanel.addQueryResultPanelSetting(queryResultPanelSetting);
		}
		
	}

	@Override
	public void addClickshortCuts() {
		// noop
	}

	@Override
	public void removeClickshortCuts() {
		// noop
	}

	@Override
	public void close() {
		analyzeResourcePanel.close();
		for (Iterator<Component> compIter=vizCardsPanel.iterator(); compIter.hasNext();) {
			VizMinPanel vizMinPanel = (VizMinPanel)compIter.next();
			vizMinPanel.close();
		}
	}
	
	@Override
	public void setTabNameChangeListener(TabCaptionChangeListener tabNameChangeListener) {
		this.tabCaptionChangeListener = tabNameChangeListener;
	}

}
