package de.catma.ui.field;

import com.vaadin.data.Item;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;

public interface FormFieldGenerator {
	public Field createField(Item item, Component uiContext);
}
