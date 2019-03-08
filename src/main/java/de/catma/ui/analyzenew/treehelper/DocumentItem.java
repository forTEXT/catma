package de.catma.ui.analyzenew.treehelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vaadin.icons.VaadinIcons;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class DocumentItem implements TreeRowItem{
	
	
	public QueryResultRowArray queryResultRowArray;
	public String treeKey;
	public ArrayList<TreeRowItem>singleItemsArray;
	public boolean unfold= true;
	static final String HORIZONTAL_ELLIPSIS = "\u2026";
	static final int MAX_VALUE_LENGTH = 10;
	static final int maxLength = 50;
	
	
	public String getTreeKey() {	
		return treeKey;
	}
	public void setTreeKey(String documentID) {
		this.treeKey = documentID;
	}

	public int getFrequency() {
	   return queryResultRowArray.size();
	     
		}


	public QueryResultRowArray getRows() {
		return   this.queryResultRowArray;
	}
	
	
	public void setRows(QueryResultRowArray queryResultRowArray) {	
		this.queryResultRowArray=  queryResultRowArray;	
	}
	
	
/*	public void setRowsAsArray(QueryResultRowArray queryResultArray) {
		
	Iterator<QueryResultRow> resultIterator=	queryResultArray.iterator();
	ArrayList<QueryResultRow> rowList = new ArrayList<>();
	singleItemsArray = new ArrayList<TreeRowItem>();
	
	while (resultIterator.hasNext()) {
		QueryResultRow queryResultRow = (QueryResultRow) resultIterator.next();
		rowList.add(queryResultRow);
		SingleItem item = new SingleItem();
		item.setTreeKey(queryResultRow.getPhrase());	
	    singleItemsArray.add(item);	
	}*/

	
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
		DocumentItem other = (DocumentItem) obj;
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
	
	public  String getArrowIcon() {
		   return unfold? VaadinIcons.CARET_RIGHT.getHtml():VaadinIcons.CARET_DOWN.getHtml();
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
	
	public void setUnfold(boolean unfold) {
		this.unfold= unfold;
		
	}
	public boolean isUnfold() {
		return this.unfold;
	}

}
