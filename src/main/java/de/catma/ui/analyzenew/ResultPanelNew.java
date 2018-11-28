package de.catma.ui.analyzenew;

import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.shared.ui.label.ContentMode;
import com.vaadin.v7.ui.Table;

import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class ResultPanelNew extends Panel {
VerticalLayout contentVerticalLayout ;
Table queryResultTable;
Grid<QueryResultRow> queryResultGrid ;
TextArea textArea;
Label queryInfo;
HorizontalLayout iconBar;
private Label caretDownIcon;
private Label optionsIcon;
private Label trashIcon;

public ResultPanelNew(QueryResult result, String queryAsString)  {
	
	queryInfo = new Label(queryAsString);
 
 	iconBar = new HorizontalLayout();
 	iconBar.setMargin(false);

 
 	contentVerticalLayout = new VerticalLayout();
 	caretDownIcon = new Label();
 	caretDownIcon.setContentMode(com.vaadin.shared.ui.ContentMode.HTML);
	caretDownIcon.setValue(FontAwesome.CARET_DOWN.getHtml());
	
	optionsIcon = new Label();
	optionsIcon.setContentMode(com.vaadin.shared.ui.ContentMode.HTML);
	optionsIcon.setValue(FontAwesome.ELLIPSIS_V.getHtml());
	
	trashIcon = new Label();
	trashIcon.setContentMode(com.vaadin.shared.ui.ContentMode.HTML);
	trashIcon.setValue(FontAwesome.TRASH.getHtml());
	
	iconBar.addComponents(trashIcon,optionsIcon,caretDownIcon);
	iconBar.setComponentAlignment(trashIcon,Alignment.MIDDLE_RIGHT);

 
	
    QueryResultRowArray resultRowArrayArrayList=   result.asQueryResultRowArray();
	

    queryResultTable = new Table("Results");
    queryResultTable.addContainerProperty("Phrase", String.class, null);
    queryResultTable.addContainerProperty("Range", String.class, null);


	 for (QueryResultRow queryResultRow : resultRowArrayArrayList) {
		 Object newItemId = queryResultTable.addItem();
		 Item row1 = queryResultTable.getItem(newItemId);
		 
		 row1.getItemProperty("Phrase").setValue(queryResultRow.getPhrase());
		 row1.getItemProperty("Range").setValue(queryResultRow.getRange().toString());
	
		//queryResultTable.addItem( new Object[]{queryResultRow.getPhrase(),queryResultRow.getRange().toString()});
	}
	 


    queryResultTable.setWidth("100%");
	
	contentVerticalLayout.addComponent(queryInfo);
    contentVerticalLayout.addComponent(iconBar);
	contentVerticalLayout.addComponent(queryResultTable);
	

	//contentVerticalLayout.setWidth("100%");
	
	setContent(contentVerticalLayout);
	//setWidth("100%");

	
}
private void setData() {
	
}
private void initComponents() {
	
}

private void createInfoBar() {
	
}
private void createbuttonsBar() {
	
}
private void showResult() {
	//arrowupIcon. set Listener.Listener..Listener
	//contentVerticalLayout.addComponent(table,)
}
private void handleArrawUpDownAction() {
	
}
	

}
