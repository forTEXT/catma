package de.catma.ui.module.analyze;

import java.util.Collection;
import java.util.List;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.event.selection.SingleSelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.ui.component.IconButton;
import de.catma.ui.module.analyze.queryresultpanel.DisplaySetting;
import de.catma.ui.module.analyze.queryresultpanel.QueryResultPanel;
import de.catma.ui.module.analyze.queryresultpanel.QueryResultPanelSetting;
import de.catma.ui.module.analyze.queryresultpanel.QueryResultRowItem;
import de.catma.ui.module.analyze.visualization.ExpansionListener;
import de.catma.ui.module.analyze.visualization.Visualization;
import de.catma.ui.module.analyze.visualization.kwic.KwicPanel;

public class VizMaxPanel extends VerticalLayout  {
	
	private static class QuerySelection {
		private QueryResultPanelSetting setting;
		private QueryResultPanel panel;
		public QuerySelection(QueryResultPanelSetting setting) {
			super();
			this.setting = setting;
		}
		
		public void setPanel(QueryResultPanel panel) {
			this.panel = panel;
		}
		
		public QueryResultPanel getPanel() {
			return panel;
		}
		
		public QueryResultPanelSetting getSetting() {
			return setting;
		}
	}
	
	interface MinimizeListener {
		public void onMinimize(VizMaxPanel vizMaxPanel);
	}

	private LoadingCache<String, KwicProvider> kwicProviderCache;

	private HorizontalSplitPanel mainContentSplitPanel;
	private Button btMinViz;
	private ComboBox<QuerySelection> queryResultBox;
	
	private Visualization visualization;

	private QueryResultPanel currentQueryResultPanel;

	private VerticalLayout topLeftPanel;

	private Project project;

	private QueryResultPanel selectedResultsPanel;

	private TextField nameLabel;

	public VizMaxPanel( 
			String name,
			Visualization visualization,
			List<QueryResultPanelSetting> queryResultPanelSettings, Project project,
			LoadingCache<String, KwicProvider> kwicProviderCache, MinimizeListener leaveListener) {
		this.visualization = visualization;
		this.project = project;
		this.kwicProviderCache = kwicProviderCache;
		initComponents(name);
		initActions(leaveListener);
		initData(queryResultPanelSettings);
		visualization.setDisplaySetting(selectedResultsPanel.getDisplaySetting());
		
	}
	
	private void setQueryResultPanel(QuerySelection querySelection) {
		if (querySelection != null) {
			if (currentQueryResultPanel != null) {
				((ComponentContainer)currentQueryResultPanel.getParent()).removeComponent(currentQueryResultPanel);
			}
			if (querySelection.getPanel() == null) {
				QueryResultPanel queryResultPanel = 
					new QueryResultPanel(
						project, 
						querySelection.getSetting().getQueryResult(), 
						querySelection.getSetting().getQueryId(), 
						kwicProviderCache,
						querySelection.getSetting().getDisplaySetting(),
						item -> handleItemSelection(item));
				
				queryResultPanel.addOptionsMenuItem(
					"Select all", 
					mi -> handleRowsSelection(queryResultPanel.getFilteredQueryResult()));
				
				queryResultPanel.setSizeFull();
				querySelection.setPanel(queryResultPanel);
			}
			AbstractOrderedLayout boxContainer = 
					(AbstractOrderedLayout)queryResultBox.getParent();
			if (boxContainer != null) {
				boxContainer.removeComponent(queryResultBox);
			}
			querySelection.getPanel().addToButtonBarLeft(queryResultBox);
			boxContainer = 
					(AbstractOrderedLayout)queryResultBox.getParent();
			boxContainer.setExpandRatio(queryResultBox, 1f);
			
			topLeftPanel.addComponent(querySelection.getPanel());
			topLeftPanel.setExpandRatio(querySelection.getPanel(), 1f);
			currentQueryResultPanel = querySelection.getPanel();

		}
	}

	private void handleItemSelection(QueryResultRowItem item) {		 
		handleRowsSelection(item.getRows());
	}

	private void handleRowsSelection(QueryResultRowArray rows) {

		if (!currentQueryResultPanel.getDisplaySetting().equals(selectedResultsPanel.getDisplaySetting())
				&& selectedResultsPanel.isEmpty()) {
			selectedResultsPanel.addQueryResultRows(rows);
			DisplaySetting currentQueryDisplaySetting = currentQueryResultPanel.getDisplaySetting();
			currentQueryDisplaySetting.init(selectedResultsPanel);
		} else {
			selectedResultsPanel.addQueryResultRows(rows);
		}
		visualization.addQueryResultRows(rows);
	}
	
	private void initData(List<QueryResultPanelSetting> queryResultPanelSettings) {
		queryResultBox.setItems(queryResultPanelSettings.stream().map(settings -> new QuerySelection(settings)));
		@SuppressWarnings("unchecked")
		Collection<QuerySelection> items = 
			((ListDataProvider<QuerySelection>)queryResultBox.getDataProvider()).getItems();
		if (!items.isEmpty()) {
			queryResultBox.setValue(items.iterator().next());
		}
	}

	private void initComponents(String name) {
		setSizeFull();
		setMargin(false);
		HorizontalLayout titlePanel = new HorizontalLayout();
		titlePanel.setMargin(false);
		titlePanel.setWidth("100%");
		
		nameLabel = new TextField(null, name);
		nameLabel.addStyleName("viz-max-panel-name");
		
		titlePanel.addComponent(nameLabel);
		titlePanel.setComponentAlignment(nameLabel, Alignment.TOP_CENTER);
		titlePanel.setExpandRatio(nameLabel, 1.f);
		
		btMinViz = new IconButton(VaadinIcons.COMPRESS_SQUARE);
		titlePanel.addComponent(btMinViz);
		titlePanel.setComponentAlignment(btMinViz, Alignment.TOP_CENTER);
		addComponent(titlePanel);
		
		mainContentSplitPanel = new HorizontalSplitPanel();
		mainContentSplitPanel.setSplitPosition(40, Sizeable.Unit.PERCENTAGE);
		
		addComponent(mainContentSplitPanel);
		setExpandRatio(mainContentSplitPanel, 1f);
		
		// left column
		
		VerticalSplitPanel resultSelectionSplitPanel = new VerticalSplitPanel();
		mainContentSplitPanel.addComponent(resultSelectionSplitPanel);
		
		// top left 
		
		topLeftPanel = new VerticalLayout();
		topLeftPanel.setSizeFull();
		topLeftPanel.setMargin(new MarginInfo(false, false, false, false));
		resultSelectionSplitPanel.addComponent(topLeftPanel);
		
		queryResultBox = new ComboBox<QuerySelection>();
		queryResultBox.setWidth("100%");
		queryResultBox.setEmptySelectionCaption("Select a resultset");
		queryResultBox.setEmptySelectionAllowed(false);
		
		queryResultBox.setItemCaptionGenerator(
			querySelection -> querySelection.getSetting().getQueryId().toString());
		

		// bottom left

		selectedResultsPanel = new QueryResultPanel(
			project, kwicProviderCache, DisplaySetting.GROUPED_BY_PHRASE,
			item -> handleItemRemoval(item));
		
		selectedResultsPanel.addToButtonBarLeft(queryResultBox);
		
		selectedResultsPanel.setSizeFull();
		selectedResultsPanel.setMargin(new MarginInfo(false, false, false, false));
		
		resultSelectionSplitPanel.addComponent(selectedResultsPanel);
		
		// right column

		mainContentSplitPanel.addComponent(visualization);
	}

	private void handleItemRemoval(QueryResultRowArray rows) {
		visualization.removeQueryResultRows(rows);
		selectedResultsPanel.removeQueryResultRows(rows);
	
	}
	
	private void handleItemRemoval(QueryResultRowItem item) {
		handleItemRemoval(item.getRows());
	}

	private void initActions(MinimizeListener leaveListener) {
		btMinViz.addClickListener(clickEvent -> leaveListener.onMinimize(this));
		queryResultBox.addSelectionListener(new SingleSelectionListener<QuerySelection>() {
			@Override
			public void selectionChange(SingleSelectionEvent<QuerySelection> event) {
				setQueryResultPanel(event.getValue());
			}
		});
		
		visualization.setExpansionListener(new ExpansionListener() {
			
			@Override
			public void expand() {
				mainContentSplitPanel.setSplitPosition(0);
			}
			
			@Override
			public void compress() {
				mainContentSplitPanel.setSplitPosition(50);
			}
		});
		
		selectedResultsPanel.setDisplaySettingChangeListener(
				displaySettings -> visualization.setDisplaySetting(displaySettings));
		selectedResultsPanel.addOptionsMenuItem(
			"Remove all", mi -> handleItemRemoval(selectedResultsPanel.getFilteredQueryResult()));
		
		selectedResultsPanel.addItemSelectionListener(selectionEvent -> handleSelectedResultItemClick(selectionEvent));
	}

	private void handleSelectedResultItemClick(SelectionEvent<QueryResultRowItem> selectionEvent) {
		selectionEvent.getFirstSelectedItem().ifPresent(
				item ->visualization.setSelectedQueryResultRows(item.getRows()));
	}

	@SuppressWarnings("unchecked")
	public void addQueryResultPanelSetting(QueryResultPanelSetting setting) {
		QuerySelection querySelection = new QuerySelection(setting);
		
		((ListDataProvider<QuerySelection>)queryResultBox.getDataProvider()).getItems().add(querySelection);
		queryResultBox.getDataProvider().refreshAll();
		queryResultBox.setValue(querySelection);
	}
	
	@SuppressWarnings("unchecked")
	private QuerySelection getQuerySelection(QueryResultPanelSetting setting) {
		for (QuerySelection querySelection : ((ListDataProvider<QuerySelection>)queryResultBox.getDataProvider()).getItems()) {
			if (querySelection.getSetting().getQueryId().equals(setting.getQueryId())) {
				return querySelection;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public void removeQueryResultPanelSetting(QueryResultPanelSetting setting) {
		QuerySelection querySelection = getQuerySelection(setting);
		
		if (queryResultBox.getValue() != null && queryResultBox.getValue().getSetting().getQueryId().equals(setting.getQueryId())) {
			querySelection.getPanel().clear();
		}
		
		if (querySelection != null) {
			((ListDataProvider<QuerySelection>)queryResultBox.getDataProvider()).getItems().remove(querySelection);
			queryResultBox.getDataProvider().refreshAll();
		}
	}
	
	public void close() {
		visualization.close();
	}

	public void setName(String name) {
		nameLabel.setValue(name);
	}
	
	public String getName() {
		return nameLabel.getValue();
	}

	public void addNameChangeListener(ValueChangeListener<String> valueChangeListener) {
		nameLabel.addValueChangeListener(valueChangeListener);
	}
}
