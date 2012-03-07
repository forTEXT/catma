package de.catma.ui.client.ui.tag;


public class CProperty {

	private CPropertyDefinition propertyDefinition;
	private CPropertyValueList propertyValueList;
	
	public CProperty(CPropertyDefinition propertyDefinition,
			CPropertyValueList propertyValueList) {
		this.propertyDefinition = propertyDefinition;
		this.propertyValueList = propertyValueList;
	}

	public String getName() {
		return propertyDefinition.getName();
	}
}
