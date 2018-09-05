package de.catma.ui.repository;

import java.util.Arrays;

import com.vaadin.v7.data.Item;
import com.vaadin.v7.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.v7.ui.Field;

import de.catma.heureclea.autotagger.TagsetIdentification;
import de.catma.ui.field.FormFieldGenerator;

public class TagsetIdentificationFieldGenerator implements FormFieldGenerator {
	
	@Override
	public Field<?> createField(Item item, Component uiContext) {
		ComboBox field = 
			new ComboBox(Messages.getString("TagsetIdentificationFieldGenerator.Tagset"), Arrays.asList(TagsetIdentification.values())); //$NON-NLS-1$
		
		for (TagsetIdentification ti : TagsetIdentification.values()) {
			field.setItemCaption(ti, ti.toString());
		}
		
		field.setItemCaptionMode(ItemCaptionMode.EXPLICIT);
		return field;
	}

}
