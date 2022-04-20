package de.catma.ui.module.tags;

import java.util.stream.Collectors;

import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;

import de.catma.tag.PropertyDefinition;
import de.catma.ui.util.Cleaner;

class PropertyDataItem implements TagsetTreeItem {
	
	private PropertyDefinition propertyDefinition;
	private boolean valuesExpanded;
	private boolean editable;
	
	public PropertyDataItem(PropertyDefinition propertyDefinition, boolean editable) {
		super();
		this.propertyDefinition = propertyDefinition;
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
		StringBuilder propertySummary = new StringBuilder();
		if (!propertyDefinition.getPossibleValueList().isEmpty()) {
			propertySummary.append(
				(valuesExpanded?VaadinIcons.CARET_DOWN.getHtml():VaadinIcons.CARET_RIGHT.getHtml()));
		}
			
		propertySummary.append("<div class=\"annotation-panel-property-summary\">");
		propertySummary.append(Cleaner.clean(propertyDefinition.getName())); 

		if (!valuesExpanded) {
			if (!propertyDefinition.getPossibleValueList().isEmpty()) { 
				propertySummary.append(" - ");
			}
			propertySummary.append(propertyDefinition.getPossibleValueList().stream()
			.limit(2)
			.map(pValue -> Cleaner.clean(pValue))
			.collect(Collectors.joining(",")));
			propertySummary.append(
				((propertyDefinition.getPossibleValueList().size() > 2)?"...":""));
		}
		propertySummary.append("</div>");
		return propertySummary.toString();		
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
	
	@Override
	public String toString() {
		return propertyDefinition.getName();
	}
	
	@Override
	public String getRemoveIcon() {
		if (editable) {
			return VaadinIcons.TRASH.getHtml();
		}
		return null;
	}

	@Override
	public boolean isEditable() {
		return editable;
	}
	
	@Override
	public void handleRemovalRequest(TagsView tagsView) {
		if (editable) {
			tagsView.deletePropertyDataItem(this);
		}
	}

}
