package de.catma.ui.serialization;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.catma.core.tag.PropertyDefinition;
import de.catma.ui.client.ui.tag.serialization.shared.PropertyDefinitionSerializationKey;

public class PropertyDefinitionSerializationHandler {
	
	private PropertyDefinition propertyDefinition;

	public PropertyDefinitionSerializationHandler(
			PropertyDefinition propertyDefinition) {
		super();
		this.propertyDefinition = propertyDefinition;
	}

	public JSONObject toJSONObject() throws JSONException {
		
		JSONObject propertyDefinitionJS = new JSONObject();
		
		propertyDefinitionJS.put(
			PropertyDefinitionSerializationKey.name.name(), 
			propertyDefinition.getName());
		
		propertyDefinitionJS.put(
			PropertyDefinitionSerializationKey.singleSelect.name(),
			propertyDefinition.getPossibleValueList().isSingleSelect());
		
		JSONArray valuesJS= new JSONArray();
		
		for (String value :
			propertyDefinition.getPossibleValueList().getPropertyValueList().getValues()) {
			
			valuesJS.put(value);
		}
		
		propertyDefinitionJS.put(
				PropertyDefinitionSerializationKey.values.name(), 
				valuesJS);
		
		return propertyDefinitionJS;
	}
}
