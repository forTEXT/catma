package de.catma.ui.analyzenew;

import java.util.List;

import com.google.common.cache.LoadingCache;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.event.selection.SingleSelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import de.catma.document.repository.Repository;
import de.catma.indexer.KwicProvider;
import de.catma.ui.analyzenew.queryresultpanel.DisplaySetting;
import de.catma.ui.analyzenew.queryresultpanel.QueryResultPanel;
import de.catma.ui.analyzenew.queryresultpanel.QueryResultPanelSetting;
import de.catma.ui.analyzenew.queryresultpanel.QueryResultRowItem;
import de.catma.ui.analyzenew.visualization.ExpansionListener;
import de.catma.ui.analyzenew.visualization.Visualisation;
import de.catma.ui.component.IconButton;

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
	private Button arrowLeftBt;
	private ComboBox<QuerySelection> queryResultBox;
	
	private Visualisation visualization;

	private QueryResultPanel currentQueryResultPanel;

	private VerticalLayout topLeftPanel;

	private Repository project;

	private QueryResultPanel selectedResultsPanel;

	public VizMaxPanel( 
			Visualisation visualization,
			List<QueryResultPanelSetting> queryResultPanelSettings, Repository project,
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
			Label label = new Label(querySelection.getSetting().getQueryId().getShortName());
			label.setDescription(querySelection.getSetting().getQueryId().getName());
			queryResultPanel.addToButtonBarLeft(label);
			querySelection.setPanel(queryResultPanel);
		}

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
	}

	private void initComponents() {
		setSizeFull();
		setMargin(false);
		
		mainContentSplitPanel = new HorizontalSplitPanel();
		mainContentSplitPanel.setSplitPosition(40, Sizeable.Unit.PERCENTAGE);
		
		addComponent(mainContentSplitPanel);
		
		// left column
		
		VerticalSplitPanel resultSelectionSplitPanel = new VerticalSplitPanel();
		mainContentSplitPanel.addComponent(resultSelectionSplitPanel);
		
		// top left 
		
		topLeftPanel = new VerticalLayout();
		topLeftPanel.setSizeFull();
		topLeftPanel.setMargin(new MarginInfo(false, false, false, false));
		resultSelectionSplitPanel.addComponent(topLeftPanel);
		
		HorizontalLayout buttonAndBoxPanel = new HorizontalLayout();
		buttonAndBoxPanel.setWidth("100%");
		buttonAndBoxPanel.setMargin(false);
		topLeftPanel.addComponent(buttonAndBoxPanel);
		
		arrowLeftBt = new IconButton(VaadinIcons.ARROW_LEFT);
		buttonAndBoxPanel.addComponent(arrowLeftBt);
		
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
		
		selectedResultsPanel.addToButtonBarLeft(new Label("Selection"));
		
		selectedResultsPanel.setSizeFull();
		
		resultSelectionSplitPanel.addComponent(selectedResultsPanel);
		
		// right column

		mainContentSplitPanel.addComponent(visualization);
	}

	private void handleItemRemoval(QueryResultRowItem item) {
		visualization.removeQueryResultRows(item.getRows());
		selectedResultsPanel.removeQueryResultRows(item.getRows());
	}

	private void initActions(LeaveListener leaveListener) {
		arrowLeftBt.addClickListener(clickEvent -> leaveListener.onLeave(this));
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
