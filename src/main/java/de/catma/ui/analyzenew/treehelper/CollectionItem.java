package de.catma.ui.analyzenew.treehelper;

import java.util.ArrayList;
import java.util.Iterator;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;

public class CollectionItem implements TreeRowItem{
private String treeKey;
private GroupedQueryResult singleItems;
private ArrayList<TreeRowItem> singleItemsArray;
	


	public String getTreeKey() {
		return this.treeKey;
	}
	
	
	public void setTreeKey(String treeKey) {
		this.treeKey= treeKey;	
	}


	public int getFrequency() {
		return singleItems.getTotalFrequency();
	}


	public GroupedQueryResult getRows() {	
		return singleItems;
	}
	
	public void setRows(GroupedQueryResult groupedQueryResult) {
		this.singleItems = groupedQueryResult;
		
	}
	
	public void setRowsAsArray(GroupedQueryResult groupedQueryResult) {
	Iterator<QueryResultRow> resultIterator=	groupedQueryResult.iterator();
	ArrayList<QueryResultRow> rowList = new ArrayList<>();
	while (resultIterator.hasNext()) {
		QueryResultRow queryResultRow = (QueryResultRow) resultIterator.next();
		rowList.add(queryResultRow);
		SingleItem item = new SingleItem();
		item.
		singleItemsArray.add()
		
	}
		
	}

}
