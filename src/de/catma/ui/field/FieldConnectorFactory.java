package de.catma.ui.field;

import com.vaadin.ui.Field;

public interface FieldConnectorFactory<T extends Field<?>> {

	FieldConnector createFieldConnector(T target, Object itemId);
}