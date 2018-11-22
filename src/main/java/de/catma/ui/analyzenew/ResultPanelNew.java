package de.catma.ui.analyzenew;

import com.vaadin.ui.Grid;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.ui.Table;

import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class ResultPanelNew extends VerticalLayout {
Table queryResultTable;
Grid<QueryResultRow> queryResultGrid ;
TextArea textArea;

public ResultPanelNew(QueryResult result)  {
	
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

	
	addComponent(queryResultTable);
}
	
	

}
