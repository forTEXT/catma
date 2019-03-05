package de.catma.ui.analyzenew.treehelper;

import java.util.ArrayList;

import de.catma.queryengine.result.GroupedQueryResult;

public class QueryRootItem implements TreeRowItem{




	GroupedQueryResult groupedSubResult;
	ArrayList<TreeRowItem> singleItemsArrayList;
	String treeKey;
	

	@Override
	public String getTreeKey() {
		return treeKey;
	}
	
	public void setTreeKey(String treeKey) {
		this.treeKey= treeKey;
	}

	@Override
	public int getFrequency() {
     return   groupedSubResult.getTotalFrequency();
     
	}

	@Override
	public GroupedQueryResult getRows() {
		// TODO Auto-generated method stub
		return groupedSubResult;
	}
	public void setRows(GroupedQueryResult groupedQueryResult) {
		this.groupedSubResult = groupedQueryResult;
		
	}

}
