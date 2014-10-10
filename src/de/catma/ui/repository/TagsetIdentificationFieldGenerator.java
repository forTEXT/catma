package de.catma.ui.repository;

import java.util.Arrays;

import com.vaadin.data.Item;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;

import de.catma.heureclea.autotagger.TagsetIdentification;
import de.catma.ui.field.FormFieldGenerator;

public class TagsetIdentificationFieldGenerator implements FormFieldGenerator {

	@Override
	public Field<?> createField(Item item, Component uiContext) {
		ComboBox field = 
			new ComboBox("Tagset", Arrays.asList(TagsetIdentification.values()));
		return field;
	}

}
