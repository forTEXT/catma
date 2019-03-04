package de.catma.ui.analyzenew.treehelper;

import java.util.ArrayList;
import java.util.List;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResultRow;

public class DocumentItem implements TreeItem{
	
	
	public GroupedQueryResult singleResultRowsList;
	public String treeKey;
	
	
	public String getTreeKey() {	
		return treeKey;
	}
	public void setTreeKey(String documentID) {
		this.treeKey = documentID;
	}

	public int getFrequency() {
	return singleResultRowsList.getTotalFrequency();
	}


	public GroupedQueryResult getRows() {
		return   this.singleResultRowsList;
	}
	
	
	public void setRows(GroupedQueryResult groupedQueryResult) {	
           singleResultRowsList= groupedQueryResult;	
	}
	


}
