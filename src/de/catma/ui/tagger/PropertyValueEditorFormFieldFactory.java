package de.catma.ui.tagger;

import java.util.Set;

import com.vaadin.data.Item;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormFieldFactory;
import com.vaadin.ui.ListSelect;

public class PropertyValueEditorFormFieldFactory implements FormFieldFactory {
	
	private ListSelect valueBox;
	
	public PropertyValueEditorFormFieldFactory(
			Set<String> initialValues) {
		valueBox = new ListSelect(
				"Values", 
				initialValues);
		valueBox.setNewItemsAllowed(true);
		valueBox.setMultiSelect(true);
	}

	public Field createField(Item item, Object propertyId, Component uiContext) {
		return valueBox;
	}
}
