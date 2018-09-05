package de.catma.ui.field;

import java.util.Map;
import java.util.WeakHashMap;

import com.vaadin.v7.ui.Field;
import com.vaadin.v7.ui.TableFieldFactory;

public class FieldMapper {
	// mapping: column->row->Field (itemId->propertyId->Field)
	private Map<Object, Map<Object, Field<?>>> 
		propertyFields = new WeakHashMap<Object, Map<Object,Field<?>>>();
	
	public void registerField(Object itemId, Object propertyId, Field<?> field) {
		
		Map<Object,Field<?>> propertyFieldMapping = propertyFields.get(itemId);
		
		if (propertyFieldMapping == null) {
			propertyFieldMapping = new WeakHashMap<Object, Field<?>>();
			propertyFields.put(itemId, propertyFieldMapping);
		}
		else if (propertyFieldMapping.containsKey(propertyId)) {
			propertyFieldMapping.clear();
		}
		
		propertyFieldMapping.put(propertyId, field);
	}
	
	public Field<?> getField(Object itemId, Object propertyId) {
		Map<Object,Field<?>> propertyFieldMapping = propertyFields.get(itemId);
		
		if (propertyFieldMapping != null) {
			return propertyFieldMapping.get(propertyId);
		}
		
		return null;
	}
	
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void connectFields(Object itemId,
			Object propertyIdSource, Object propertyIdTarget, 
			FieldConnectorFactory fieldConnectorFactory) {

		Field source = getField(
				itemId, 
				propertyIdSource);

		Field target = getField(
			itemId, 
			propertyIdTarget);
		
		if ((source != null) && (target != null)) {
			source.addValueChangeListener(fieldConnectorFactory.createFieldConnector(
					target, itemId));
		}
	}


}
