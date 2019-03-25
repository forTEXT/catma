package de.catma.ui.analyzenew.treehelper;

import java.io.Serializable;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;

public class SingleItem implements TreeRowItem, Serializable {

	private String forward;
	private String backward;
	private int position;
	private String treeKey;
	private QueryResultRowArray queryResultRowArray;
	static final String HORIZONTAL_ELLIPSIS = "\u2026";
	static final int MAX_VALUE_LENGTH = 10;
	static final int maxLength = 50;

	public void setTreeKey(String treeKey) {
		this.treeKey = treeKey;
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

	public String getShortenTreeKey() {
		return shorten(this.treeKey, 18);
	}

	private String shorten(String toShortenValue, int maxLength) {
		if (toShortenValue.length() <= maxLength) {
			return toShortenValue;
		}
		return toShortenValue.substring(0, maxLength / 2) + "[" + HORIZONTAL_ELLIPSIS + "]"
				+ toShortenValue.substring(toShortenValue.length() - ((maxLength / 2) - 2), toShortenValue.length());
	}

	public String getPhrase() {
		return queryResultRowArray.get(0).getPhrase();
	}

	public String getPropertyName() {
		TagQueryResultRow tRow = (TagQueryResultRow) queryResultRowArray.get(0);
		return tRow.getPropertyName();

	}

	public String getPropertyValue() {
		TagQueryResultRow tRow = (TagQueryResultRow) queryResultRowArray.get(0);
		return tRow.getPropertyValue();

	}

	public String getBackward() {
		return backward;
	}

	public void setBackward(String backward) {
		this.backward = backward;
	}

	public String getForward() {
		return forward;
	}

	public void setForward(String forward) {
		this.forward = forward;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public QueryResultRowArray getQueryResultRowArray() {
		return queryResultRowArray;
	}

	public void setQueryResultRowArray(QueryResultRowArray queryResultRowArray) {
		this.queryResultRowArray = queryResultRowArray;
	}

	@Override
	public String getContext() {
		return 	getBackward()+getTreeKey()+getForward();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((backward == null) ? 0 : backward.hashCode());
		result = prime * result + ((forward == null) ? 0 : forward.hashCode());
		result = prime * result + position;
		result = prime * result + ((queryResultRowArray == null) ? 0 : queryResultRowArray.hashCode());
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
		SingleItem other = (SingleItem) obj;
		if (backward == null) {
			if (other.backward != null)
				return false;
		} else if (!backward.equals(other.backward))
			return false;
		if (forward == null) {
			if (other.forward != null)
				return false;
		} else if (!forward.equals(other.forward))
			return false;
		if (position != other.position)
			return false;
		if (queryResultRowArray == null) {
			if (other.queryResultRowArray != null)
				return false;
		} else if (!queryResultRowArray.equals(other.queryResultRowArray))
			return false;
		if (treeKey == null) {
			if (other.treeKey != null)
				return false;
		} else if (!treeKey.equals(other.treeKey))
			return false;
		return true;
	}
	
	

}
