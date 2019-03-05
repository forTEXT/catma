package de.catma.ui.analyzenew;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.antlr.runtime.RecognitionException;

import com.github.appreciated.material.MaterialTheme;
import com.vaadin.data.TreeData;
import com.vaadin.event.MouseEvents;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComboBox.NewItemHandler;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
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
import de.catma.ui.analyzenew.treehelper.TreeRowItem;
import de.catma.ui.analyzer.GroupedQueryResultSelectionListener;
import de.catma.ui.analyzer.Messages;
import de.catma.ui.analyzer.RelevantUserMarkupCollectionProvider;
import de.catma.ui.analyzer.TagKwicResultsProvider;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.repository.MarkupCollectionItem;
import de.catma.ui.tabbedview.ClosableTab;
import de.catma.ui.tabbedview.TabComponent;

public class AnalyzeNewView extends VerticalLayout
		implements ClosableTab, TabComponent, GroupedQueryResultSelectionListener, RelevantUserMarkupCollectionProvider,
		TagKwicResultsProvider, HasComponents {

	public static interface CloseListenerNew {
		public void closeRequest(AnalyzeNewView analyzeNewView);
	}

	private String userMarkupItemDisplayString = "Markup Collections";
	private IndexedRepository repository;
	private Corpus corpus;
	private CloseListenerNew closeListener;
	private List<String> relevantSourceDocumentIDs;
	private List<String> relevantUserMarkupCollIDs;
	private List<String> relevantStaticMarkupCollIDs;
	private IndexInfoSet indexInfoSet;
	private Button btExecuteSearch;
	private Button btQueryBuilder;
	private Button kwicBt;
	private Button distBt;
	private Button wordCloudBt;
	private Button networkBt;
	private String searchInput = new String();
	private ComboBox<String> queryComboBox;
	private ResultPanelNew queryResultPanel;
	private HorizontalLayout resultAndVizSnapshotsPanel;
	private VerticalLayout resultsPanel;
	private VerticalLayout snapshotsPanel;
	private MarginInfo margin;
	private Panel resultScrollPanel;
	private Iterator<Component> allResultPanelsIterator;
	private VerticalLayout contentPanel;
	private HorizontalLayout searchAndVisIconsPanel;
	private HashSet currentResults;
	private Panel resultsFramePanel;
	private Panel snapshotsFramePanel;

	private Component searchPanel;
	private Component visIconsPanel;

	public AnalyzeNewView(Corpus corpus, IndexedRepository repository, CloseListenerNew closeListener)
			throws Exception {
		this.corpus = corpus;
		this.repository = repository;
		this.closeListener = closeListener;
		this.relevantSourceDocumentIDs = new ArrayList<String>();
		this.relevantUserMarkupCollIDs = new ArrayList<String>();
		this.relevantStaticMarkupCollIDs = new ArrayList<String>();
		this.indexInfoSet = new IndexInfoSet(Collections.<String>emptyList(), Collections.<Character>emptyList(),
				Locale.ENGLISH);

		initComponents();
		initListeners();
		initActions();
	}

	private void initComponents() throws Exception {
		margin = new MarginInfo(true, true, true, true);
		createHeaderInfo();

		searchPanel = createSearchPanel();
		visIconsPanel = createVisIconsPanel();

		searchAndVisIconsPanel = new HorizontalLayout();
		searchAndVisIconsPanel.addComponents(searchPanel, visIconsPanel);
		searchAndVisIconsPanel.setWidth("100%");
		searchAndVisIconsPanel.setExpandRatio(searchPanel, 1);
		searchAndVisIconsPanel.setExpandRatio(visIconsPanel, 1);
		searchAndVisIconsPanel.setMargin(margin);
		// addComponent(searchAndVisIconsPanel);

		resultAndVizSnapshotsPanel = new HorizontalLayout();
		resultAndVizSnapshotsPanel.setWidth("100%");
		resultAndVizSnapshotsPanel.setHeight("100%");
		resultsPanel = new VerticalLayout();
		resultsPanel.setHeightUndefined();

		// this didnt work for making the resultpanel scrollable
		// resultScrollPanel = new Panel();
		// resultScrollPanel.setContent(resultPanel);
		// resultScrollPanel.setHeightUndefined();

		contentPanel = new VerticalLayout();

		snapshotsPanel = new VerticalLayout();
		snapshotsPanel.setHeightUndefined();
		 resultsFramePanel = new Panel();
		resultsFramePanel.setContent(resultsPanel);
		resultsFramePanel.setHeight("380px");
		snapshotsFramePanel = new Panel();
		snapshotsFramePanel.setContent(snapshotsPanel);
		snapshotsFramePanel.setHeight("380px");
		resultAndVizSnapshotsPanel.addComponents(resultsFramePanel, snapshotsFramePanel);
		resultAndVizSnapshotsPanel.setExpandRatio(resultsFramePanel, 1);
		resultAndVizSnapshotsPanel.setExpandRatio(snapshotsFramePanel, 1);

		resultAndVizSnapshotsPanel.setMargin(margin);
		setMargin(true);

		contentPanel.addComponent(searchAndVisIconsPanel);
		contentPanel.addComponent(resultAndVizSnapshotsPanel);

		addComponent(contentPanel);
	}

	private void initListeners() {

		queryComboBox.addValueChangeListener(event -> {
			if (event.getSource().isEmpty()) {
				Notification.show("Error", "query field is empty", Notification.Type.HUMANIZED_MESSAGE);
			} else {
				String predefQueryString = event.getSource().getValue();
				searchInput = predefQueryString;
				Notification.show("Query", "is : " + searchInput.toString(), Notification.Type.HUMANIZED_MESSAGE);
			}
		});

		/*
		 * queryComboBox.addSelectionListener(new SingleSelectionListener<String>() {
		 * 
		 * @Override public void selectionChange(SingleSelectionEvent<String> event) {
		 * searchInput= (event.getValue().toString());
		 * 
		 * } });
		 */

		queryComboBox.setNewItemHandler(new NewItemHandler() {
			@Override
			public void accept(String t) {
				searchInput = t;
			}
		});

	}

	private void initActions() {
		btExecuteSearch.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				searchInput.toString();

				// String predefQuery= event.getSource().toString();
				// searchInput.setValue(predefQuery);
				executeSearch();
			}

		});

		/*
		 * kwicBt.addClickListener(new ClickListener() {
		 * 
		 * @Override public void buttonClick(ClickEvent event) {
		 * 
		 * KwicVizPanel kwicPanel = new KwicVizPanel("KWIC PANEL", getAllTreeGrids(),
		 * new SaveCancelListener<VizSnapshot>() {
		 * 
		 * @Override public void savePressed(VizSnapshot vizSnapshot) {
		 * visualizationPreviewPanel.addComponent(vizSnapshot); } }); kwicPanel.show();
		 * }
		 * 
		 * });
		 */

		kwicBt.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {

				KwicVizPanelNew kwic = new KwicVizPanelNew(new CloseVizViewListener() {

					@Override
					public void onClose() {
						VizSnapshot kwivSnapshot = new VizSnapshot("Kwic Snapshot");
						snapshotsPanel.addComponent(kwivSnapshot);
					
						setContent(contentPanel);
					}
				},getAllTreeGridDatas(),repository);
				setContent(kwic);
			}
		});
	}

	private Component getAnalyzerView() {
		return contentPanel;
	}

	private void setContent(Component component) {
		removeAllComponents();
		addComponent(component);
		component.setHeight("100%");
		component.setWidth("100%");

	}

	private void createHeaderInfo() throws Exception {
		// documentsContainer = new HierarchicalContainer();
		// documentsTree = new Tree();
		// documentsTree.setContainerDataSource(documentsContainer);
		// documentsTree.setCaption(
		// Messages.getString("AnalyzerView.docsConstrainingThisSearch")); //$NON-NLS-1$

		if (corpus != null) {
			for (SourceDocument sd : corpus.getSourceDocuments()) {
				addSourceDocument(sd);
			}
		} else {
			for (SourceDocument sd : repository.getSourceDocuments()) {
				addSourceDocument(sd);
			}
			// documentsTree.addItem(Messages.getString("AnalyzerView.AllDocuments"));
			// //$NON-NLS-1$
		}

	}

	private Component createSearchPanel() {
		VerticalLayout searchPanel = new VerticalLayout();
		Label searchPanelLabel = new Label("Queries");

		HorizontalLayout searchRow = new HorizontalLayout();
		searchRow.setWidth("100%");

		btQueryBuilder = new Button("+ BUILD QUERY");
		btQueryBuilder.setStyleName("body");

		List<String> predefQueries = new ArrayList<>();

		predefQueries.add("property= \"%\"");
		predefQueries.add("tag=\"Tag%\"");
		predefQueries.add("tag= \"Tag1\"");
		predefQueries.add("wild= \"und\"");
		predefQueries.add("wild= \"Blumen%\"");
		predefQueries.add("wild= \"%\"");

		predefQueries.add("freq>0");

		queryComboBox = new ComboBox<>();
		queryComboBox.setItems(predefQueries);

		btExecuteSearch = new Button("SEARCH", VaadinIcons.SEARCH);
		btExecuteSearch.setWidth("100%");
		btExecuteSearch.setStyleName("primary");

		queryComboBox.setWidth("100%");
		searchRow.addComponents(btQueryBuilder, queryComboBox);
		searchRow.setComponentAlignment(queryComboBox, Alignment.MIDDLE_CENTER);
		searchRow.setExpandRatio(queryComboBox, 0.6f);
		searchPanel.addComponents(searchPanelLabel, searchRow, btExecuteSearch);
		return searchPanel;
	}

	private Component createVisIconsPanel() {
		VerticalLayout visIconsPanel = new VerticalLayout();
		Label visIconsLabel = new Label("Visualisations");
		visIconsLabel.setWidth("100%");
		HorizontalLayout visIconBar = new HorizontalLayout();

		kwicBt = new Button("KWIC", VaadinIcons.SPLIT);
		kwicBt.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
		kwicBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		kwicBt.setWidth("100%");
		kwicBt.setHeight("100%");

		distBt = new Button("DISTRIBUTION", VaadinIcons.CHART_LINE);
		distBt.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
		distBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		distBt.setWidth("100%");
		distBt.setHeight("100%");
		distBt.setDescription(
			    "<h2>"+
			    	    "A richtext tooltip</h2>"+
			    	    "<ul>"+
			    	    "  <li>Use rich formatting with HTML</li>"+
			    	    "  <li>Include images from themes</li>"+
			    	    "  <li>etc.</li>"+
			    	    "</ul>",ContentMode.HTML);

		wordCloudBt = new Button("WORDCLOUD", VaadinIcons.CLOUD);
		wordCloudBt.addStyleName(MaterialTheme.BUTTON_ICON_ALIGN_TOP);
		wordCloudBt.addStyleName(MaterialTheme.BUTTON_BORDERLESS);
		wordCloudBt.setWidth("100%");
		wordCloudBt.setHeight("100%");

		networkBt = new Button("NETWORK", VaadinIcons.CLUSTER);
		networkBt.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
		networkBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		networkBt.setWidth("100%");
		networkBt.setHeight("100%");

		/*
		 * VerticalLayout barchartIconLayout = new VerticalLayout(); Label icon = new
		 * Label(FontAwesome.BAR_CHART.getHtml(), ContentMode.HTML);
		 * icon.addStyleName(ValoTheme.LABEL_H1); icon.setHeight("100%");
		 * icon.setWidth("100%"); Label iconText = new Label("barchart");
		 * barchartIconLayout.addComponents(icon,iconText);
		 */

		visIconBar.addComponents(kwicBt, distBt, wordCloudBt, networkBt);
		visIconBar.setWidth("100%");
		visIconBar.setHeight("100%");

		visIconsPanel.addComponent(visIconsLabel);
		visIconsPanel.setComponentAlignment(visIconsLabel, Alignment.MIDDLE_CENTER);
		visIconsPanel.addComponent(visIconBar);
		visIconsPanel.setHeight("100%");
		return visIconsPanel;
	}

/*	private ArrayList<TreeGrid<TagRowItem>> getAllTreeGrids() {
		Iterator<Component> iterator = resultPanel.getComponentIterator();
		ArrayList<TreeGrid<TagRowItem>> toReturnList = new ArrayList<TreeGrid<TagRowItem>>();
		while (iterator.hasNext()) {
			ResultPanelNew onePanel = (ResultPanelNew) iterator.next();
			toReturnList.add(onePanel.getCurrentTreeGrid());
		}
		return toReturnList;
	}*/

	private ArrayList<CurrentTreeGridData> getAllTreeGridDatas() {
		Iterator<Component> iterator = resultsPanel.getComponentIterator();
		ArrayList<CurrentTreeGridData> toReturnList = new ArrayList<CurrentTreeGridData>();
		while (iterator.hasNext()) {
			ResultPanelNew onePanel = (ResultPanelNew) iterator.next();
			CurrentTreeGridData current= new CurrentTreeGridData(onePanel.getQueryAsString(),  (TreeData<TreeRowItem>) onePanel.getCurrentTreeGridData(),onePanel.getCurrentView());
			toReturnList.add(current);
		}
		return toReturnList;
	}



	private void executeSearch() {

		QueryOptions queryOptions = new QueryOptions(relevantSourceDocumentIDs, relevantUserMarkupCollIDs,
				relevantStaticMarkupCollIDs, indexInfoSet.getUnseparableCharacterSequences(),
				indexInfoSet.getUserDefinedSeparatingCharacters(), indexInfoSet.getLocale(), repository);

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
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						queryResultPanel.setWidth("100%");
						queryResultPanel.addClickListener(new MouseEvents.ClickListener() {
							@Override
							public void click(com.vaadin.event.MouseEvents.ClickEvent event) {
								// Notification.show(
								// Messages.getString("Clickbares Zeug"), Type.HUMANIZED_MESSAGE);
							}
						});

						resultsPanel.setSpacing(true);
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

	private void addSourceDocument(SourceDocument sd) {
		relevantSourceDocumentIDs.add(sd.getID());
		// TODO: provide a facility where the user can select between different
		// IndexInfoSets
		indexInfoSet = sd.getSourceContentHandler().getSourceDocumentInfo().getIndexInfoSet();

		// documentsTree.addItem(sd);
		MarkupCollectionItem umc = new MarkupCollectionItem(sd, userMarkupItemDisplayString, true);
		// documentsTree.addItem(umc);
		// documentsTree.setParent(umc, sd);
		for (UserMarkupCollectionReference umcRef : sd.getUserMarkupCollectionRefs()) {
			if (corpus.getUserMarkupCollectionRefs().contains(umcRef)) {
				addUserMarkupCollection(umcRef, umc);
			}
		}
	}

	private void addUserMarkupCollection(UserMarkupCollectionReference umcRef, MarkupCollectionItem umc) {
		this.relevantUserMarkupCollIDs.add(umcRef.getId());
	}

	@Override
	public void tagResults() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<String> getRelevantUserMarkupCollectionIDs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Corpus getCorpus() {
		return corpus;
	}

	@Override
	public void resultsSelected(GroupedQueryResultSet groupedQueryResultSet) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addClickshortCuts() {
		// TODO Auto-generated method stub

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
