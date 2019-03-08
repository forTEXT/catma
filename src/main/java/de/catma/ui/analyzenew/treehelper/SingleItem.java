package de.catma.ui.analyzenew.treehelper;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRowArray;

public class SingleItem implements TreeRowItem{
	
	private String treeKey;
	private QueryResultRowArray queryResultRowArray;
	static final String HORIZONTAL_ELLIPSIS = "\u2026";
	static final int MAX_VALUE_LENGTH = 10;
	static final int maxLength = 50;

	public void setTreeKey(String treeKey) {
		this.treeKey= treeKey;
	}
	@Override
	public String getTreeKey() {
		// TODO Auto-generated method stub
		return treeKey;
	}

	@Override
	public int getFrequency() {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public QueryResultRowArray getRows() {
		// TODO Auto-generated method stub
		return queryResultRowArray;
	}
	
	public void setRows(QueryResultRowArray queryResultRowArray) {
		this.queryResultRowArray = queryResultRowArray;
		
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
	
/*	public Kwic getKwic() {	
	}*/
	
	

}
