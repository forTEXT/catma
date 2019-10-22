package de.catma.ui.module.tags;

import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;

class PossibleValueDataItem implements TagsetTreeItem {
	
	private String value;
	private boolean editable;
	
	public PossibleValueDataItem(String value, boolean editable) {
		super();
		this.value = value;
		this.editable = editable;
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
	
	@Override
	public String getRemoveIcon() {
		if (editable) {
			return VaadinIcons.TRASH.getHtml();
		}
		return VaadinIcons.LOCK.getHtml();
	}

	@Override
	public boolean isEditable() {
		return editable;
	}
	
	@Override
	public void handleRemovalRequest(TagsView tagsView) {
		if (editable) {
			tagsView.deletePossibleValueDataItem(this);
		}
	}
}
