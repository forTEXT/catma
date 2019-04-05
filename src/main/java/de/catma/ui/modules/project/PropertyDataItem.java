package de.catma.ui.modules.project;

import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;

public class PropertyDataItem implements PropertyTreeItem {
	
	private PropertyDefinition propertyDef;
	private Property property;

	public PropertyDataItem(PropertyDefinition propertyDef, Property property) {
		this.propertyDef = propertyDef;
		this.property = property;
	}

	@Override
	public String getName() {
		return propertyDef.getName();
	}

	@Override
	public String getValue() {
		return null; // value column is empty
	}

	public Property getProperty() {
		return property;
	}
	
	public PropertyDefinition getPropertyDef() {
		return propertyDef;
	}
}
