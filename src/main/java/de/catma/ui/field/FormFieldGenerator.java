package de.catma.ui.field;

import com.vaadin.v7.data.Item;
import com.vaadin.ui.Component;
import com.vaadin.v7.ui.Field;

public interface FormFieldGenerator {
	public Field createField(Item item, Component uiContext);
}
