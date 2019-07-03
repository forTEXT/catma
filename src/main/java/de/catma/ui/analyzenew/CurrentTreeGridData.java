package de.catma.ui.analyzenew;

import com.vaadin.data.TreeData;

import de.catma.ui.analyzenew.treegridhelper.TreeRowItem;


public class CurrentTreeGridData {
	
	private String query;
	private TreeData<TreeRowItem> currentTreeData;
	private ViewID viewID;

	public CurrentTreeGridData(String queryString, TreeData<TreeRowItem> currentTreeGridData, ViewID currentView) {
		this.currentTreeData=currentTreeGridData;
		this.query= queryString;
		this.viewID= currentView;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public TreeData<TreeRowItem> getCurrentTreeData() {
		return currentTreeData;
	}

	private void setCurrentTreeData(TreeData<TreeRowItem> currentTreeData) {
		this.currentTreeData = currentTreeData;
	}

	public ViewID getViewID() {
		return viewID;
	}

	private void setViewID(ViewID viewID) {
		this.viewID = viewID;
	}


	

}
