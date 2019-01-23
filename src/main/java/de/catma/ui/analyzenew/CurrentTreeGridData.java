package de.catma.ui.analyzenew;

import com.vaadin.data.TreeData;


public class CurrentTreeGridData {
	private String query;
	private TreeData<TagRowItem> currentTreeData;
	private ViewID viewID;

	public CurrentTreeGridData(String queryAsString, TreeData<TagRowItem> currentTreeData, ViewID viewID) {
		this.currentTreeData=currentTreeData;
		this.query= queryAsString;
		this.viewID= viewID;
		
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public TreeData getCurrentTreeData() {
		return currentTreeData;
	}

	private void setCurrentTreeData(TreeData<TagRowItem> currentTreeData) {
		this.currentTreeData = currentTreeData;
	}

	public ViewID getViewID() {
		return viewID;
	}

	private void setViewID(ViewID viewID) {
		this.viewID = viewID;
	}


	

}
