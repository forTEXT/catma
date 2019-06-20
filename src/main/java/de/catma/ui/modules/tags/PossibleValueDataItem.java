package de.catma.ui.modules.tags;

import com.vaadin.data.provider.TreeDataProvider;

class PossibleValueDataItem implements TagsetTreeItem {
	
	private String value;
	
	public PossibleValueDataItem(String value) {
		super();
		this.value = value;
	}

	@Override
	public String getColor() {
		return null; //intended
	}

	@Override
	public String getName() {
		return null; //intended
	}

	@Override
	public String getTagsetName() {
		return null; //intended
	}

	@Override
	public String getPropertySummary() {
		return null;
	}


	@Override
	public void removePropertyDataItem(TreeDataProvider<TagsetTreeItem> dataProvider) {
		dataProvider.getTreeData().removeItem(this);
	}
	
	@Override
	public String generateStyle() {
		return "annotate-panel-possible-value-data-item";
	}
	
	@Override
	public String getPropertyValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return value;
	}
}
