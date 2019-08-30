package de.catma.ui.analyzenew;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import de.catma.ui.analyzenew.queryresultpanel.QueryResultPanelSetting;
import de.catma.ui.component.IconButton;



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

	public VizMinPanel(String title, VizMaxPanel vizMaxPanel, RemoveListener removeListener, MaximizeListener maximizeListener) {
		this.vizMaxPanel = vizMaxPanel;
		initComponents(title);
		initActions(removeListener, maximizeListener);
	}
	
	private void initActions(RemoveListener removeListener, MaximizeListener maximizeListener) {
		btMaximize.addClickListener(clickEvent -> maximizeListener.onMaximize());
		btRemove.addClickListener(clickEvent -> removeListener.onRemove(this));
	}

	private void initComponents(String title) {
		addStyleName("analyze-card");
		setMargin(false);
		setSpacing(false);
		
		Label titleLabel = new Label(title);
		titleLabel.addStyleName("analyze-card-infobar");
		
		HorizontalLayout buttonBar = new HorizontalLayout();
		buttonBar.setWidth("100%");
		buttonBar.addStyleName("analyze-card-buttonbar");

		btRemove = new IconButton (VaadinIcons.ERASER);
		
		btMaximize = new IconButton (VaadinIcons.ARROW_RIGHT);
		
		buttonBar.addComponents(btRemove,btMaximize);
		buttonBar.setComponentAlignment(btRemove, Alignment.MIDDLE_RIGHT);
		buttonBar.setComponentAlignment(btMaximize, Alignment.MIDDLE_RIGHT);
		buttonBar.setExpandRatio(btRemove, 1f);
		
		addComponents(titleLabel, buttonBar);
	}

	public void addQueryResultPanelSetting(QueryResultPanelSetting queryResultPanelSetting) {
		vizMaxPanel.addQueryResultPanelSetting(queryResultPanelSetting);
	}
}
