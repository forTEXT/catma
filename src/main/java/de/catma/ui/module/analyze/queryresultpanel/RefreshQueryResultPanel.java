package de.catma.ui.module.analyze.queryresultpanel;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import de.catma.queryengine.QueryId;
import de.catma.ui.component.IconButton;

public class RefreshQueryResultPanel extends VerticalLayout {

	private Label queryInfo;
	private QueryId queryId;
	private IconButton removeBt;
	private IconButton refreshButton;
	
	
	public RefreshQueryResultPanel(QueryId queryId) {
		this.queryId = queryId;
		initComponents();
	}
	
	public QueryId getQueryId() {
		return queryId;
	}

	private void initComponents() {
		addStyleName("analyze-card");
		setMargin(false);
		setSpacing(false);
		queryInfo = new Label(queryId.toString());
		queryInfo.addStyleName("analyze-card-infobar");
		
		addComponent(queryInfo);
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		buttonPanel.setWidth("100%");
		
		refreshButton = new IconButton(VaadinIcons.REFRESH);
		refreshButton.addStyleName("refresh-query-result-panel-refresh-button");
		buttonPanel.addComponent(refreshButton);
		
		TextField searchField = new TextField();
        searchField.setPlaceholder("\u2315");
        searchField.setEnabled(false);
        
        buttonPanel.addComponent(searchField);
        buttonPanel.setComponentAlignment(searchField, Alignment.MIDDLE_RIGHT);
        buttonPanel.setExpandRatio(searchField, 1f);
		IconButton caretRightBt = new IconButton(VaadinIcons.CARET_RIGHT);
		caretRightBt.setEnabled(false);
		
		IconButton optionsBt = new IconButton(VaadinIcons.ELLIPSIS_DOTS_V);
		optionsBt.setEnabled(false);

		removeBt = new IconButton(VaadinIcons.ERASER);

		buttonPanel.addComponents(removeBt, optionsBt, caretRightBt);
		buttonPanel.setComponentAlignment(caretRightBt, Alignment.MIDDLE_RIGHT);
		buttonPanel.setComponentAlignment(optionsBt, Alignment.MIDDLE_RIGHT);
		buttonPanel.setComponentAlignment(removeBt, Alignment.MIDDLE_RIGHT);
		
		buttonPanel.addStyleName("analyze-card-buttonbar");
		searchField.setEnabled(false);

		addComponent(buttonPanel);
	}
	
	public void addRefreshClickListener(ClickListener clickListener) {
		refreshButton.addClickListener(clickListener);
	}
	
	public void addRemoveClickListener(ClickListener clickListener) {
		removeBt.addClickListener(clickListener);
	}
}
