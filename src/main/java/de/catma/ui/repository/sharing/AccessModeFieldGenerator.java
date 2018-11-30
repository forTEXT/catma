package de.catma.ui.repository.sharing;

import java.util.Arrays;
import java.util.List;

import com.vaadin.ui.Component;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.Field;

import de.catma.document.AccessMode;
import de.catma.ui.field.FormFieldGenerator;

public class AccessModeFieldGenerator implements FormFieldGenerator {
	private List<AccessMode> accessModeList = Arrays.asList(AccessMode.values());
	
	public Field createField(Item item, Component uiContext) {
		ComboBox field = new ComboBox(Messages.getString("AccessModeFieldGenerator.shareMode"), accessModeList); //$NON-NLS-1$
		field.setValue(accessModeList.get(0));
		field.setRequired(true);
		field.setNullSelectionAllowed(false);
		field.setNewItemsAllowed(false);
		return field;
	}

}
