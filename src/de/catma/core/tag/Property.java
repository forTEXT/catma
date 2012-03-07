package de.catma.core.tag;


public class Property {

	private PropertyDefinition propertyDefinition;
	private PropertyValueList propertyValueList;
	
	public Property(PropertyDefinition propertyDefinition,
			PropertyValueList propertyValueList) {
		this.propertyDefinition = propertyDefinition;
		this.propertyValueList = propertyValueList;
	}

	public String getName() {
		return propertyDefinition.getName();
	}
	
	public PropertyValueList getPropertyValueList() {
		return propertyValueList;
	}
}
