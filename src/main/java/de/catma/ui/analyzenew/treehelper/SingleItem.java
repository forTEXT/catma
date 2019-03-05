package de.catma.ui.analyzenew.treehelper;

import de.catma.queryengine.result.GroupedQueryResult;

public class SingleItem implements TreeRowItem{
	
	private String treeKey;
	private GroupedQueryResult groupedQueryResult;
	
	
	
	private void setTreeKey(String treeKey) {
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
	public GroupedQueryResult getRows() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Kwic getKwic() {
		
	}

}
