package de.catma.ui.serialization;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.catma.core.tag.PropertyDefinition;
import de.catma.core.tag.TagDefinition;
import de.catma.ui.client.ui.tag.serialization.shared.TagDefinitionSerializationKey;

public class TagDefinitionSerializationHandler {

	private TagDefinition tagDefinition;

	public TagDefinitionSerializationHandler(TagDefinition tagDefinition) {
		super();
		this.tagDefinition = tagDefinition;
	}
	
	public JSONObject toJSONObject() throws JSONException {
		JSONObject tagDefinitionJS = new JSONObject();
		
		tagDefinitionJS.put(
			TagDefinitionSerializationKey.id.name(), tagDefinition.getID());
		tagDefinitionJS.put(
			TagDefinitionSerializationKey.type.name(), tagDefinition.getType());
		tagDefinitionJS.put(
			TagDefinitionSerializationKey.baseID.name(), tagDefinition.getBaseID());
		
		JSONArray userDefinedPropertiesJS = new JSONArray();
		
		for (PropertyDefinition pd :
			tagDefinition.getUserDefinedPropertyDefinitions()) {
			userDefinedPropertiesJS.put(
				new PropertyDefinitionSerializationHandler(pd).toJSONObject());
		}

		tagDefinitionJS.put(
			TagDefinitionSerializationKey.userDefinedPropertyDefinitions.name(), 
			userDefinedPropertiesJS);
		
		JSONArray systemPropertiesJS = new JSONArray();
		
		for (PropertyDefinition pd :
			tagDefinition.getSystemPropertyDefinitions()) {
			systemPropertiesJS.put(
				new PropertyDefinitionSerializationHandler(pd).toJSONObject());
		}

		tagDefinitionJS.put(
			TagDefinitionSerializationKey.systemPropertyDefinitions.name(), 
			systemPropertiesJS);
	
		return tagDefinitionJS;
	}
}
