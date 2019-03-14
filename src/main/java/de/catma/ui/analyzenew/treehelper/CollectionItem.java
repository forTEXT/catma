package de.catma.ui.analyzenew.treehelper;

import java.util.ArrayList;
import java.util.Iterator;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class CollectionItem implements TreeRowItem{
	static final String HORIZONTAL_ELLIPSIS = "\u2026";
	static final int MAX_VALUE_LENGTH = 10;
	static final int maxLength = 50;
	private String treeKey;
	private QueryResultRowArray queryResultRowArray;
	private ArrayList<TreeRowItem> singleItemsArray;

	public CollectionItem() {
	}

	public String getTreeKey() {
		return this.treeKey;
	}
	
	public void setTreeKey(String treeKey) {
		this.treeKey= treeKey;	
	}

	public int getFrequency() {
		return queryResultRowArray.getTotalFrequency();
	}

	public QueryResultRowArray getRows() {	
		return queryResultRowArray;
	}	
	public void setRows(QueryResultRowArray resultArray) {
		this.queryResultRowArray = resultArray;		
	}
	
	public void setRowsAsArray(QueryResultRowArray queryResultArray) {
		
	Iterator<QueryResultRow> resultIterator=	queryResultArray.iterator();
	ArrayList<QueryResultRow> rowList = new ArrayList<>();
	singleItemsArray = new ArrayList<TreeRowItem>();
	
	while (resultIterator.hasNext()) {
		QueryResultRow queryResultRow = (QueryResultRow) resultIterator.next();
		rowList.add(queryResultRow);
		SingleItem item = new SingleItem();
		item.setTreeKey(queryResultRow.getPhrase());	
	    singleItemsArray.add(item);	
	}
}

/*	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((singleItemsArray == null) ? 0 : singleItemsArray.hashCode());
		result = prime * result + ((treeKey == null) ? 0 : treeKey.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CollectionItem other = (CollectionItem) obj;
		if (singleItemsArray == null) {
			if (other.singleItemsArray != null)
				return false;
		} else if (!singleItemsArray.equals(other.singleItemsArray))
			return false;
		if (treeKey == null) {
			if (other.treeKey != null)
				return false;
		} else if (!treeKey.equals(other.treeKey))
			return false;
		return true;
	}*/
	
	public String getArrowIcon() {
		
		return null;
	}
	
	public String getShortenTreeKey(){
		return shorten(this.treeKey,26);
	}

	private String shorten(String toShortenValue, int maxLength) {
		if (toShortenValue.length() <= maxLength) {
			return toShortenValue;
		}	
		return toShortenValue.substring(0, maxLength/2) 
				+"["+HORIZONTAL_ELLIPSIS+"]"
				+ toShortenValue.substring(toShortenValue.length()-((maxLength/2)-2), toShortenValue.length());
	}

	@Override
	public String getPropertyName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPropertyValue() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
