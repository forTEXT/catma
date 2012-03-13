package de.catma.ui.dialog;

import com.vaadin.data.util.PropertysetItem;

public class PropertyCollection extends PropertysetItem {
	
	
	public PropertyCollection(String... stringProperties) {
		for (String stringProperty : stringProperties) {
			addItemProperty(stringProperty, new StringProperty());
		}
	}
	
	

}
