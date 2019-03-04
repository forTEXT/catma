package de.catma.ui.analyzenew.treehelper;

import java.util.ArrayList;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;

public class CollectionItem implements TreeItem{
private String treeKey;
private GroupedQueryResult singleItems;
	


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

}
