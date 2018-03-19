package de.catma.ui.field;

import com.vaadin.v7.data.Container;
import com.vaadin.ui.Component;
import com.vaadin.v7.ui.Field;

public interface TableFieldGenerator {
	public Field createField(Container container, Object itemId, Component uiContext);
}
