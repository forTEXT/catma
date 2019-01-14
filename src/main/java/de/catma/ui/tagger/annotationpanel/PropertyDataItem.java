package de.catma.ui.tagger.annotationpanel;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.data.provider.HierarchicalDataProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;

import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.tag.PropertyDefinition;

public class PropertyDataItem implements TagsetTreeItem {
	
	private PropertyDefinition propertyDefinition;
	private boolean valuesExpanded;
	
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
		StringBuilder propertySummary = new StringBuilder();
		propertySummary.append(
			(valuesExpanded?VaadinIcons.CARET_DOWN.getHtml():VaadinIcons.CARET_RIGHT.getHtml()));
			
		propertySummary.append("<div class=\"annotation-panel-property-summary\">");
		propertySummary.append(propertyDefinition.getName()); 

		if (!valuesExpanded) {
			propertySummary.append(" - ");
			propertySummary.append(propertyDefinition.getPossibleValueList().stream()
			.limit(2)
			.collect(Collectors.joining(",")));
			propertySummary.append(
				((propertyDefinition.getPossibleValueList().size() > 2)?"...":""));
		}
		propertySummary.append("</div>");
		return propertySummary.toString();		
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
	public void removePropertyDataItem(TreeDataProvider<TagsetTreeItem> dataProvider) {
		dataProvider.getTreeData().removeItem(this);
	}
	
	@Override
	public String generateStyle() {
		return "annotate-panel-property-data-item";
	}
	
	public PropertyDefinition getPropertyDefinition() {
		return propertyDefinition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((propertyDefinition == null) ? 0 : propertyDefinition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PropertyDataItem other = (PropertyDataItem) obj;
		if (propertyDefinition == null) {
			if (other.propertyDefinition != null)
				return false;
		} else if (!propertyDefinition.equals(other.propertyDefinition))
			return false;
		return true;
	}
	
	public boolean isValuesExpanded() {
		return valuesExpanded;
	}
	
	public void setValuesExpanded(boolean valuesExpanded) {
		this.valuesExpanded = valuesExpanded;
	}
	
}
