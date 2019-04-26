package de.catma.ui.analyzenew.treehelper;

import java.util.ArrayList;
import com.vaadin.icons.VaadinIcons;
import de.catma.queryengine.result.QueryResultRowArray;

public class DocumentItem implements TreeRowItem {

	public QueryResultRowArray queryResultRowArray;
	public String treeKey;
	public ArrayList<TreeRowItem> singleItemsArray;
	public boolean unfold = false;
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
		return this.queryResultRowArray;
	}

	public void setRows(QueryResultRowArray queryResultRowArray) {
		this.queryResultRowArray = queryResultRowArray;
	}


	public String getArrowIcon() {
		return unfold ? VaadinIcons.CARET_DOWN.getHtml() : VaadinIcons.CARET_RIGHT.getHtml();
	}

	public String getShortenTreeKey() {
		return shorten(this.treeKey, 26);
	}

	private String shorten(String toShortenValue, int maxLength) {
		if (toShortenValue.length() <= maxLength) {
			return toShortenValue;
		}
		return toShortenValue.substring(0, maxLength / 2) + "[" + HORIZONTAL_ELLIPSIS + "]"
				+ toShortenValue.substring(toShortenValue.length() - ((maxLength / 2) - 2), toShortenValue.length());
	}

	public void setUnfold(boolean unfold) {
		this.unfold = unfold;

	}

	public boolean isUnfold() {
		return this.unfold;
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
