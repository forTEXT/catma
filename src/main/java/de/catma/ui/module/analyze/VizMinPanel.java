package de.catma.ui.module.analyze;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.catma.ui.component.IconButton;
import de.catma.ui.module.analyze.queryresultpanel.QueryResultPanelSetting;



public class VizMinPanel extends VerticalLayout {
	
	static interface RemoveListener {
		public void onRemove(VizMinPanel vizMinPanel);
	}
	
	static interface MaximizeListener {
		public void onMaximize();
	}
	
	private Button btMaximize;
	private Button btRemove;
	private VizMaxPanel vizMaxPanel;
	private TextField titleLabel;

	public VizMinPanel(
			String title, VizMaxPanel vizMaxPanel,
			RemoveListener removeListener, MaximizeListener maximizeListener) {
		this.vizMaxPanel = vizMaxPanel;
		initComponents(title);
		initActions(removeListener, maximizeListener);
	}
	
	private void initActions(RemoveListener removeListener, MaximizeListener maximizeListener) {
		btMaximize.addClickListener(clickEvent -> maximizeListener.onMaximize());
		btRemove.addClickListener(clickEvent -> {
			close();
			removeListener.onRemove(this);
		});
		titleLabel.addValueChangeListener(event -> vizMaxPanel.setName(event.getValue()));
		vizMaxPanel.addNameChangeListener(event -> titleLabel.setValue(event.getValue()));
	}

	private void initComponents(String title) {
		addStyleName("analyze-card");
		setMargin(false);
		setSpacing(false);
		
		titleLabel = new TextField(null, title);
		titleLabel.setWidth("90%");
		titleLabel.addStyleName("analyze-card-infobar");
		
		HorizontalLayout buttonBar = new HorizontalLayout();
		buttonBar.setWidth("100%");
		buttonBar.addStyleName("analyze-card-buttonbar");

		btRemove = new IconButton (VaadinIcons.ERASER);
		
		btMaximize = new IconButton (VaadinIcons.EXPAND_SQUARE);
		
		buttonBar.addComponents(btRemove,btMaximize);
		buttonBar.setComponentAlignment(btRemove, Alignment.MIDDLE_RIGHT);
		buttonBar.setComponentAlignment(btMaximize, Alignment.MIDDLE_RIGHT);
		buttonBar.setExpandRatio(btRemove, 1f);
		
		addComponents(titleLabel, buttonBar);
	}

	public void addQueryResultPanelSetting(QueryResultPanelSetting queryResultPanelSetting) {
		vizMaxPanel.addQueryResultPanelSetting(queryResultPanelSetting);
	}

	public void removeQueryResultPanelSetting(QueryResultPanelSetting queryResultPanelSetting) {
		vizMaxPanel.removeQueryResultPanelSetting(queryResultPanelSetting);
	}
	
	public void close() {
		vizMaxPanel.close();
	}
}
