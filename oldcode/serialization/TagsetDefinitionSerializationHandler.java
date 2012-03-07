package de.catma.ui.serialization;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagsetDefinition;
import de.catma.ui.client.ui.tag.serialization.shared.TagsetDefinitionSerializationKey;

public class TagsetDefinitionSerializationHandler {

	private TagsetDefinition tagsetDefinition;

	public TagsetDefinitionSerializationHandler(TagsetDefinition tagsetDefinition) {
		super();
		this.tagsetDefinition = tagsetDefinition;
	}
	

	public JSONObject toJSONObject() throws JSONException {
		
		JSONObject tagsetDefinitionJS = new JSONObject();
		
		tagsetDefinitionJS.put(
				TagsetDefinitionSerializationKey.id.name(), 
				tagsetDefinition.getID());
		
		tagsetDefinitionJS.put(
			TagsetDefinitionSerializationKey.name.name(), 
			tagsetDefinition.getName());
		
		JSONArray tagDefinitionsJS = new JSONArray();
		
		for (TagDefinition td : tagsetDefinition) {
			tagDefinitionsJS.put(
					new TagDefinitionSerializationHandler(td).toJSONObject());
		}
	
		tagsetDefinitionJS.put(
				TagsetDefinitionSerializationKey.tagDefinitions.name(), 
				tagDefinitionsJS);
		
		return tagsetDefinitionJS;

	}
}
