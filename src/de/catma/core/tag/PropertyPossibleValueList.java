package de.catma.core.tag;

import java.util.List;

public class PropertyPossibleValueList {

	private boolean singleSelect;
	private PropertyValueList propertyValueList;
	
	public PropertyPossibleValueList(List<String> values, boolean singleSelect) {
		super();
		this.propertyValueList = new PropertyValueList(values);
		this.singleSelect = singleSelect;
	}
	
	
	@Override
	public String toString() {
		return propertyValueList.toString();
	}


	public String getFirstValue() {
		return propertyValueList.getFirstValue();
	}
	
	
	public boolean isSingleSelect() {
		return singleSelect;
	}
	
	public PropertyValueList getPropertyValueList() {
		return propertyValueList;
	}
	
	
	
	
}
