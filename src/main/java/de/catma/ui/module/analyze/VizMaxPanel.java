package de.catma.ui.module.analyze;

import java.util.Collection;
import java.util.List;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.event.selection.SingleSelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.indexer.KwicProvider;
import de.catma.project.Project;
import de.catma.ui.component.IconButton;
import de.catma.ui.module.analyze.queryresultpanel.DisplaySetting;
import de.catma.ui.module.analyze.queryresultpanel.QueryResultPanel;
import de.catma.ui.module.analyze.queryresultpanel.QueryResultPanelSetting;
import de.catma.ui.module.analyze.queryresultpanel.QueryResultRowItem;
import de.catma.ui.module.analyze.visualization.ExpansionListener;
import de.catma.ui.module.analyze.visualization.Visualisation;

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
	
	interface LeaveListener {
		public void onLeave(VizMaxPanel vizMaxPanel);
	}

	private LoadingCache<String, KwicProvider> kwicProviderCache;

	private HorizontalSplitPanel mainContentSplitPanel;
	private Button btMinViz;
	private ComboBox<QuerySelection> queryResultBox;
	
	private Visualisation visualization;

	private QueryResultPanel currentQueryResultPanel;

	private VerticalLayout topLeftPanel;

	private Project project;

	private QueryResultPanel selectedResultsPanel;

	private HorizontalLayout buttonAndBoxPanel;

	public VizMaxPanel( 
			Visualisation visualization,
			List<QueryResultPanelSetting> queryResultPanelSettings, Project project,
			LoadingCache<String, KwicProvider> kwicProviderCache, LeaveListener leaveListener) {
		this.visualization = visualization;
		this.project = project;
		this.kwicProviderCache = kwicProviderCache;
		initComponents();
		initActions(leaveListener);
		initData(queryResultPanelSettings);
		visualization.setDisplaySetting(selectedResultsPanel.getDisplaySetting());
		
	}
	
	private void setQueryResultPanel(QuerySelection querySelection) {
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
			queryResultPanel.setSizeFull();
			querySelection.setPanel(queryResultPanel);
		}
		AbstractOrderedLayout buttonAndBoxPanelContainer = 
				(AbstractOrderedLayout)buttonAndBoxPanel.getParent();
		if (buttonAndBoxPanelContainer != null) {
			buttonAndBoxPanelContainer.removeComponent(buttonAndBoxPanel);
		}
		querySelection.getPanel().addToButtonBarLeft(buttonAndBoxPanel);
		buttonAndBoxPanelContainer = 
				(AbstractOrderedLayout)buttonAndBoxPanel.getParent();
		buttonAndBoxPanelContainer.setExpandRatio(buttonAndBoxPanel, 1f);
		
		topLeftPanel.addComponent(querySelection.getPanel());
		topLeftPanel.setExpandRatio(querySelection.getPanel(), 1f);
		currentQueryResultPanel = querySelection.getPanel();
	}

	private void handleItemSelection(QueryResultRowItem item) {
		selectedResultsPanel.addQueryResultRows(item.getRows());
		visualization.addQueryResultRows(item.getRows());
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

	private void initComponents() {
		setSizeFull();
		setMargin(false);

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
		
		buttonAndBoxPanel = new HorizontalLayout();
		buttonAndBoxPanel.setWidth("100%");
		buttonAndBoxPanel.setMargin(false);
		
		btMinViz = new IconButton(VaadinIcons.COMPRESS_SQUARE);
		buttonAndBoxPanel.addComponent(btMinViz);
		
		queryResultBox = new ComboBox<QuerySelection>();
		queryResultBox.setWidth("100%");
		queryResultBox.setEmptySelectionCaption("Select a resultset");
		queryResultBox.setEmptySelectionAllowed(false);
		
		queryResultBox.setItemCaptionGenerator(
			querySelection -> querySelection.getSetting().getQueryId().toString());
		
		buttonAndBoxPanel.addComponent(queryResultBox);
		buttonAndBoxPanel.setExpandRatio(queryResultBox, 1f);
		// bottom left

		
		selectedResultsPanel = new QueryResultPanel(
			project, kwicProviderCache, DisplaySetting.GROUPED_BY_PHRASE,
			item -> handleItemRemoval(item));
		
		selectedResultsPanel.addToButtonBarLeft(buttonAndBoxPanel);
		
		selectedResultsPanel.setSizeFull();
		selectedResultsPanel.setMargin(new MarginInfo(false, false, false, false));
		
		resultSelectionSplitPanel.addComponent(selectedResultsPanel);
		
		// right column

		mainContentSplitPanel.addComponent(visualization);
	}

	private void handleItemRemoval(QueryResultRowItem item) {
		visualization.removeQueryResultRows(item.getRows());
		selectedResultsPanel.removeQueryResultRows(item.getRows());
	}

	private void initActions(LeaveListener leaveListener) {
		btMinViz.addClickListener(clickEvent -> leaveListener.onLeave(this));
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
		
		selectedResultsPanel.setDisplaySettingChangeListener(displaySettings -> visualization.setDisplaySetting(displaySettings));
	}

	@SuppressWarnings("unchecked")
	public void addQueryResultPanelSetting(QueryResultPanelSetting setting) {
		((ListDataProvider<QuerySelection>)queryResultBox.getDataProvider()).getItems().add(new QuerySelection(setting));
		queryResultBox.getDataProvider().refreshAll();
	}
}
