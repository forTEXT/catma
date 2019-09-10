package de.catma.ui.module.annotate.annotationpanel;

import java.util.Collections;
import java.util.List;

import com.vaadin.data.provider.TreeDataProvider;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.TagReference;

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
	public String getVisibilityIcon() {
		return null; //intended
	}

	@Override
	public String getPropertySummary() {
		return null;
	}

	@Override
	public boolean isVisible() {
		return false;
	}

	@Override
	public void setVisible(boolean visible) {
		// nooop
	}

	@Override
	public List<TagReference> getTagReferences(List<AnnotationCollection> collections) {
		return Collections.emptyList();
	}

	@Override
	public void setChildrenVisible(TreeDataProvider<TagsetTreeItem> treeDataProvider, boolean visible,
			boolean explicit) {
		// noop
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
