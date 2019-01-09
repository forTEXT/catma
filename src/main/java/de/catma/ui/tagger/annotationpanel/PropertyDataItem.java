package de.catma.ui.tagger.annotationpanel;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.data.provider.HierarchicalDataProvider;
import com.vaadin.data.provider.TreeDataProvider;

import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.tag.PropertyDefinition;

public class PropertyDataItem implements TagsetTreeItem {
	
	private PropertyDefinition propertyDefinition;
	
	public PropertyDataItem(PropertyDefinition propertyDefinition) {
		super();
		this.propertyDefinition = propertyDefinition;
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
		return propertyDefinition.getName() 
			+ " - " 
		+ propertyDefinition.getPossibleValueList().stream().collect(Collectors.joining(","));
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
	public List<TagReference> getTagReferences(List<UserMarkupCollection> collections) {
		return Collections.emptyList();
	}

	@Override
	public void setChildrenVisible(TreeDataProvider<TagsetTreeItem> treeDataProvider, boolean visible,
			boolean explicit) {
		// noop
	}

	@Override
	public boolean isPropertiesExpanded() {
		return false;
	}

	@Override
	public void setPropertiesExpanded(boolean propertiesExpanded) {
		// noop

	}

	@Override
	public void removePropertyDataItem(TreeDataProvider<TagsetTreeItem> dataProvider) {
		dataProvider.getTreeData().removeItem(this);
	}
	
	@Override
	public String generateStyle() {
		return "annotate-panel-property-data-item";
	}
}
