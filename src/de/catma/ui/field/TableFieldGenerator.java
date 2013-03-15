package de.catma.ui.field;

import com.vaadin.data.Container;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;

public interface TableFieldGenerator {
	public Field createField(Container container, Object itemId, Component uiContext);
}
