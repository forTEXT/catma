package de.catma.ui.repository.sharing;

import java.util.Arrays;
import java.util.List;

import com.vaadin.data.Item;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;

import de.catma.document.AccessMode;
import de.catma.ui.field.FormFieldGenerator;

public class AccessModeFieldGenerator implements FormFieldGenerator {
	private List<AccessMode> accessModeList = Arrays.asList(AccessMode.values());
	
	public Field createField(Item item, Component uiContext) {
		ComboBox field = new ComboBox("Share mode", accessModeList);
		field.setValue(accessModeList.get(0));
		field.setRequired(true);
		field.setNullSelectionAllowed(false);
		field.setNewItemsAllowed(false);
		return field;
	}

}
