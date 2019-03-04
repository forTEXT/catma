package de.catma.ui.analyzenew.treehelper;

import java.util.ArrayList;
import java.util.List;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;

public class RootItem implements TreeItem {
	GroupedQueryResult groupedSubResult;
	ArrayList<RowItem> singleItemsArrayList;
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
