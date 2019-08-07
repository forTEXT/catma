package de.catma.ui.analyzenew.treegridhelper;

import java.util.ArrayList;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class RootItem implements TreeRowItem {
	private QueryResultRowArray queryResultRowArray;
	private ArrayList<TreeRowItem> singleItemsArrayList;
    private String treeKey;
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
	return	queryResultRowArray.size();
	   
   }
     
	@Override
	public QueryResultRowArray getRows() {
		// TODO Auto-generated method stub
		return queryResultRowArray;
	}
	public void setRows(QueryResultRowArray queryResultRowArray) {
		this.queryResultRowArray =  queryResultRowArray;
		
	}
	
	@Override
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

	@Override
	public String getForward() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getBackward() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPosition() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContextDiv() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPhrase() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCollectionName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDocumentName() {
		// TODO Auto-generated method stub
		return null;
	}


}
