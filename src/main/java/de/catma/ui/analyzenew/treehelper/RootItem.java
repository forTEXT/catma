package de.catma.ui.analyzenew.treehelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.hazelcast.map.impl.querycache.publisher.QueryCacheListenerRegistry;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.PhraseResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class RootItem implements TreeRowItem {
	public QueryResultRowArray queryResultRowArray;
	public ArrayList<TreeRowItem> singleItemsArrayList;
    public 	String treeKey;
	static final String HORIZONTAL_ELLIPSIS = "\u2026";
	static final int MAX_VALUE_LENGTH = 10;
	static final int maxLength = 50;
	private Integer frequency;
	

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
	public void setOneRow(QueryResultRow queryResultRow) {
		
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


}
