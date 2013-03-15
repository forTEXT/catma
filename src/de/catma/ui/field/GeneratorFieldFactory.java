package de.catma.ui.field;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;

public class GeneratorFieldFactory extends DefaultFieldFactory {
	
	protected Map<Object, TableFieldGenerator> tableFieldGenerators = 
			new HashMap<Object, TableFieldGenerator>();
	
	protected Map<Object, FormFieldGenerator> formFieldGenerators = 
			new HashMap<Object, FormFieldGenerator>();
	
	@Override
	public Field createField(Container container, Object itemId,
			Object propertyId, Component uiContext) {
		TableFieldGenerator generator = tableFieldGenerators.get(propertyId);
		if (generator != null) {
			return generator.createField(container, itemId, uiContext);
		}
		return super.createField(container, itemId, propertyId, uiContext);
	}
	
	@Override
	public Field createField(Item item, Object propertyId, Component uiContext) {
		FormFieldGenerator generator = formFieldGenerators.get(propertyId);
		if (generator != null) {
			return generator.createField(item, uiContext);
		}
		return super.createField(item, propertyId, uiContext);
	}
	
}
