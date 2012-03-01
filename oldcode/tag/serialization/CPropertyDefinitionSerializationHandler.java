package de.catma.ui.client.ui.tag.serialization;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONObject;

import de.catma.ui.client.ui.tag.CPropertyDefinition;
import de.catma.ui.client.ui.tag.CPropertyPossibleValueList;
import de.catma.ui.client.ui.tag.serialization.shared.PropertyDefinitionSerializationKey;

public class CPropertyDefinitionSerializationHandler {
	
	private JSONObject propertyDefinitionJS;

	public CPropertyDefinitionSerializationHandler(
			JSONObject propertyDefinitionJS) {
		super();
		this.propertyDefinitionJS = propertyDefinitionJS;
	}

	public CPropertyDefinition toPropertyDefinition() {
		
		List<String> values = new ArrayList<String>();
		
		JSONArray valuesJS = 
				(JSONArray)propertyDefinitionJS.get(
						PropertyDefinitionSerializationKey.values.name());
		
		for (int i=0; i<valuesJS.size(); i++) {
			values.add(JSONUtil.getValueFromStringObject(valuesJS.get(i)));
		}
		
		
		CPropertyPossibleValueList possibleValueList =
			new CPropertyPossibleValueList(
				values,
				((JSONBoolean)propertyDefinitionJS.get(
					PropertyDefinitionSerializationKey.singleSelect.name())).
						booleanValue());

		CPropertyDefinition propertyDefinition = 
			new CPropertyDefinition(
				JSONUtil.getValueFromStringObject(
						propertyDefinitionJS.get(
								PropertyDefinitionSerializationKey.name.name())), 
				possibleValueList);
		
		
		
		return propertyDefinition;
	}
}
