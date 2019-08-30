package de.catma.ui.analyzenew;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

import org.antlr.runtime.RecognitionException;
import org.vaadin.sliderpanel.SliderPanel;
import org.vaadin.sliderpanel.SliderPanelBuilder;
import org.vaadin.sliderpanel.client.SliderMode;

import com.github.appreciated.material.MaterialTheme;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.EventBus;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.Corpus;
import de.catma.document.source.IndexInfoSet;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.KwicProvider;
import de.catma.queryengine.QueryId;
import de.catma.queryengine.QueryJob;
import de.catma.queryengine.QueryJob.QueryException;
import de.catma.queryengine.QueryOptions;
import de.catma.queryengine.result.QueryResult;
import de.catma.ui.CatmaApplication;
import de.catma.ui.analyzenew.queryresultpanel.QueryResultPanel;
import de.catma.ui.analyzenew.queryresultpanel.QueryResultPanelSetting;
import de.catma.ui.analyzenew.resourcepanel.AnalyzeResourcePanel;
import de.catma.ui.analyzenew.visualization.kwic.KwicPanelNew;
import de.catma.ui.analyzer.Messages;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.component.IconButton;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.ui.tabbedview.TabCaptionChangeListener;
import de.catma.util.StopWatch;

public class AnalyzeNewView extends HorizontalLayout
		implements ClosableTab {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private IndexedRepository project;
	private LoadingCache<String, KwicProvider> kwicProviderCache;
	
	private Corpus currentCorpus;
	private List<String> queryProposals;
	
	private Button btExecuteSearch;
	private Button btQueryBuilder;
	private Button kwicBt;
	private Button distBt;
	private Button wordCloudBt;
	private Button networkBt;
	private Button btQueryOptions;
	private Button btVizOptions;
	
	private ComboBox<String> queryBox;
	
	private VerticalLayout resultsPanel;
	private VerticalLayout vizCardsPanel;
	private HorizontalLayout contentPanel;

	private AnalyzeResourcePanel analyzeResourcePanel;
	private SliderPanel drawer;
	private IndexInfoSet indexInfoSet;
	private TabCaptionChangeListener tabCaptionChangeListener;
	private AnalyzeCaption analyzeCaption;

	public AnalyzeNewView(
			Corpus corpus, 
			IndexedRepository project, EventBus eventBus) {

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
		}
		
		
		//TODO: update queries and vizs
		
		currentCorpus = corpus;
		
		if (!analyzeCaption.isSetManually()) {
			analyzeCaption.setCaption(currentCorpus);
			if (tabCaptionChangeListener != null) {
				tabCaptionChangeListener.tabCaptionChange(this);
			}
		}
	}

	private VerticalLayout createSearchPanel() {	
		VerticalLayout searchPanel = new VerticalLayout();
		searchPanel.setWidth("100%");
		searchPanel.addStyleName("analyze-search-panel");
		searchPanel.setMargin(new MarginInfo(false, true, true, false));

		btQueryBuilder = new Button("Build Query");

		//TODO: add Queries with descriptive names
		queryProposals = new ArrayList<>();
		queryProposals.add("property= \"%\"");
		queryProposals.add("tag=\"%\"");
		queryProposals.add("wild= \"Blumen%\",tag=\"%\"");
		queryProposals.add("wild= \"%\"");
		queryProposals.add("freq>0");

		queryBox = new ComboBox<>();
		queryBox.setDataProvider(new ListDataProvider<>(queryProposals));
		queryBox.setEmptySelectionCaption("Select or enter a free query");
		queryBox.setWidth("100%");
		
		btExecuteSearch = new Button("Search", VaadinIcons.SEARCH);	
		btExecuteSearch.addStyleName(MaterialTheme.BUTTON_PRIMARY);
		btExecuteSearch.setWidth("100%");
		
		HorizontalLayout queryPanel = new HorizontalLayout();
		queryPanel.setSizeFull();
		queryPanel.addComponents(btQueryBuilder, queryBox);
		queryPanel.setExpandRatio(queryBox, 1.0f);
		
		searchPanel.addComponents(queryPanel, btExecuteSearch);

		return searchPanel;
	}

	private HorizontalLayout createVizIconsPanel() {		
		kwicBt = new Button("Kwic", VaadinIcons.SPLIT);
		kwicBt.addStyleName(MaterialTheme.BUTTON_ICON_ALIGN_TOP);
		kwicBt.addStyleName(MaterialTheme.BUTTON_BORDERLESS);
		kwicBt.addStyleName("analyze_viz_icon");
		kwicBt.setSizeFull();

		distBt = new Button("Distribution", VaadinIcons.CHART_LINE);
		distBt.addStyleName(MaterialTheme.BUTTON_ICON_ALIGN_TOP);
		distBt.addStyleName(MaterialTheme.BUTTON_BORDERLESS);
		distBt.addStyleName("analyze_viz_icon");
		distBt.setSizeFull();

		wordCloudBt = new Button("Wordcloud", VaadinIcons.CLOUD);
		wordCloudBt.addStyleName(MaterialTheme.BUTTON_ICON_ALIGN_TOP);
		wordCloudBt.addStyleName(MaterialTheme.BUTTON_BORDERLESS);
		wordCloudBt.addStyleName("analyze_viz_icon");
		wordCloudBt.setSizeFull();

		networkBt = new Button("NETWORK", VaadinIcons.CLUSTER);
		networkBt.addStyleName(MaterialTheme.BUTTON_ICON_ALIGN_TOP);
		networkBt.addStyleName(MaterialTheme.BUTTON_BORDERLESS);
		networkBt.addStyleName("analyze_viz_icon");
		networkBt.setSizeFull();

		return new HorizontalLayout(kwicBt, distBt, wordCloudBt, networkBt);
	}
	
	private void setContent(Component newContent, Component oldContent) {	
		replaceComponent(oldContent, newContent);		
	}
	
	private void initActions() {
		queryBox.setNewItemProvider(inputString -> {
		    return Optional.of(inputString);
		});
		
		btExecuteSearch.addClickListener(clickEvent -> executeSearch());	
		queryBox.addValueChangeListener(valueChange -> btExecuteSearch.click());

		kwicBt.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {

				VizMaxPanel vizMaxPanel = 
						new VizMaxPanel(
								new KwicPanelNew(project, kwicProviderCache),
								getQueryResultPanelSettings(),
								project, 
								kwicProviderCache,
								closedVizMaxPanel -> setContent(contentPanel, closedVizMaxPanel));
				
				VizMinPanel vizMinPanel = 
						new VizMinPanel(
							"Kwic Visualisation", 
							vizMaxPanel,
							toBeRemovedVizMinPanel -> vizCardsPanel.removeComponent(toBeRemovedVizMinPanel),
							() -> setContent(vizMaxPanel, contentPanel));
				

				vizCardsPanel.addComponent(vizMinPanel);
				setContent(vizMaxPanel, contentPanel);
			}
		});

	}

	private List<QueryResultPanelSetting> getQueryResultPanelSettings() {
		List<QueryResultPanelSetting> settings = new ArrayList<QueryResultPanelSetting>();
		for (Iterator<Component> iter = resultsPanel.iterator(); iter.hasNext(); ) {
		    QueryResultPanel queryResultPanel = (QueryResultPanel)iter.next();
		    settings.add(queryResultPanel.getQueryResultPanelSetting());
		}
		    
		return settings;
	}

	private void executeSearch() {

		String searchInput = queryBox.getValue();
		if (searchInput == null || searchInput.trim().isEmpty()) {
			Notification.show("Info", "Please enter or select a query first!", Type.HUMANIZED_MESSAGE);
			return;
		}
		
		QueryOptions queryOptions = new QueryOptions(
				new QueryId(searchInput),
				currentCorpus.getDocumentIds(), 
				currentCorpus.getCollectionIds(),
				indexInfoSet.getUnseparableCharacterSequences(),
				indexInfoSet.getUserDefinedSeparatingCharacters(), indexInfoSet.getLocale(), project);
		QueryJob job = new QueryJob(searchInput, queryOptions);
		
		//TODO: show progress
		((BackgroundServiceProvider) UI.getCurrent()).submit(Messages.getString("AnalyzerView.Searching"), //$NON-NLS-1$
				job, new ExecutionListener<QueryResult>() {
					public void done(QueryResult result) {
						StopWatch watch = new StopWatch();
						System.out.println(watch);
						try {
							QueryResultPanel queryResultPanel = new QueryResultPanel(project, result,
									new QueryId(searchInput.toString()),
									kwicProviderCache, 
									closingPanel -> resultsPanel.removeComponent(closingPanel));
							
							resultsPanel.addComponentAsFirst(queryResultPanel);
							addQueryResultPanelSetting(queryResultPanel.getQueryResultPanelSetting());
						} catch (Exception e) {
							//TODO: error handling
							e.printStackTrace();
						}

						System.out.println(watch);
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
		// TODO Auto-generated method stub

	}
	
	@Override
	public void setTabNameChangeListener(TabCaptionChangeListener tabNameChangeListener) {
		this.tabCaptionChangeListener = tabNameChangeListener;
	}

}
