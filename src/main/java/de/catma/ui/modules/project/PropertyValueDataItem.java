package de.catma.ui.modules.project;

import de.catma.tag.Property;

public class PropertyValueDataItem implements PropertyTreeItem {

	private Property property;
	private String value;

	public PropertyValueDataItem(Property property, String value) {
		this.property = property;
		this.value = value;
	}

	@Override
	public String getName() {
		return null; // on purpose
	}

	@Override
	public String getValue() {
		return value;
	}

	public Property getProperty() {
		return property;
	}
}
