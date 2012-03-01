package de.catma.ui.client.ui.tag;

import java.util.List;

public class CPropertyPossibleValueList {

	private boolean singleSelect;
	private CPropertyValueList propertyValueList;
	
	public CPropertyPossibleValueList(List<String> values, boolean singleSelect) {
		super();
		this.propertyValueList = new CPropertyValueList(values);
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
	
	public CPropertyValueList getPropertyValueList() {
		return propertyValueList;
	}
	
	
	
	
}
