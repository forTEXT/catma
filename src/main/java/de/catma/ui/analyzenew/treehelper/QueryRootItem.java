package de.catma.ui.analyzenew.treehelper;

import java.util.ArrayList;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRowArray;

public class QueryRootItem implements TreeRowItem{




	public QueryResultRowArray queryResulutRowArray;
	public ArrayList<TreeRowItem> singleItemsArrayList;
	public String treeKey;
	static final String HORIZONTAL_ELLIPSIS = "\u2026";
	static final int MAX_VALUE_LENGTH = 10;
	static final int maxLength = 50;
	

	@Override
	public String getTreeKey() {
		return treeKey;
	}
	
	public void setTreeKey(String treeKey) {
		this.treeKey= treeKey;
	}

	@Override
	public int getFrequency() {
     return   queryResulutRowArray.getTotalFrequency();
     
	}

	@Override
	public QueryResultRowArray getRows() {
		// TODO Auto-generated method stub
		return queryResulutRowArray;
	}
	public void setRows(QueryResultRowArray groupedQueryResult) {
		this.queryResulutRowArray = groupedQueryResult;
		
	}
	
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

}
