package de.catma.ui.analyzenew;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.shared.ui.label.ContentMode;
import com.vaadin.v7.ui.Table;

import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class ResultPanelNew extends Panel {

	private VerticalLayout contentVerticalLayout;
	private Table queryResultTable;
	private Grid<QueryResultRow> queryResultGrid;
	private TextArea textArea;
	private Label queryInfo;
	private HorizontalLayout groupedIcons;
	private Button caretDownBt;
	private Button caretUpBt;
	private Button trashBt;
	private Button optionsBt;

	private QueryResult queryResult;
	private String queryAsString;

	public ResultPanelNew(QueryResult result, String queryAsString) {
		this.queryResult = result;
		this.queryAsString = queryAsString;

		initComponents();
		initListeners();
		setData();

	}
	private void initComponents() {
		contentVerticalLayout = new VerticalLayout();
		setContent(contentVerticalLayout);
		createResultInfoBar();
		createButtonBar();
	}
	
	private void initListeners() {
		caretDownBt.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				contentVerticalLayout.addComponent(queryResultTable);
				groupedIcons.replaceComponent(caretDownBt,caretUpBt);
			
			}
		});
		
		caretUpBt.addClickListener(new ClickListener() {
			
			public void buttonClick(ClickEvent event) {
				contentVerticalLayout.removeComponent(queryResultTable);
				groupedIcons.replaceComponent(caretUpBt,caretDownBt);
			
			}
		});
	}

	private void setData() {
		QueryResultRowArray resultRowArrayArrayList = queryResult.asQueryResultRowArray();
		queryResultTable = new Table("Results");
		queryResultTable.addContainerProperty("Phrase", String.class, null);
		queryResultTable.addContainerProperty("Range", String.class, null);

		for (QueryResultRow queryResultRow : resultRowArrayArrayList) {
			Object newItemId = queryResultTable.addItem();
			Item row1 = queryResultTable.getItem(newItemId);

			row1.getItemProperty("Phrase").setValue(queryResultRow.getPhrase());
			row1.getItemProperty("Range").setValue(queryResultRow.getRange().toString());
		}
		queryResultTable.setWidth("100%");
	
	}

	private void createResultInfoBar() {
		QueryResultRowArray resultRowArrayArrayList = queryResult.asQueryResultRowArray();
		int resultSize = resultRowArrayArrayList.size();
		queryInfo = new Label(queryAsString + "(" + resultSize + ")");
		queryInfo.setStyleName("body");
		contentVerticalLayout.addComponent(queryInfo);
	}

	private void createButtonBar() {
		groupedIcons = new HorizontalLayout();
		groupedIcons.setMargin(false);

		caretDownBt = new Button(VaadinIcons.CARET_DOWN);
		caretDownBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		caretUpBt = new Button(VaadinIcons.CARET_UP);
		caretUpBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		optionsBt = new Button(VaadinIcons.ELLIPSIS_V);
		optionsBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		trashBt = new Button(VaadinIcons.TRASH);
		trashBt.addStyleName(ValoTheme.BUTTON_BORDERLESS);

		groupedIcons.addComponents(trashBt, optionsBt, caretDownBt);
		groupedIcons.setWidthUndefined();

		contentVerticalLayout.addComponent(groupedIcons);
		contentVerticalLayout.setComponentAlignment(groupedIcons, Alignment.MIDDLE_RIGHT);
	}


}
