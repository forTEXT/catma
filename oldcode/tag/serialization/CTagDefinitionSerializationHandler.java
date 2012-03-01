package de.catma.ui.client.ui.tag.serialization;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

import de.catma.ui.client.ui.tag.CTagDefinition;
import de.catma.ui.client.ui.tag.CVersion;
import de.catma.ui.client.ui.tag.serialization.shared.TagDefinitionSerializationKey;

public class CTagDefinitionSerializationHandler {

	private JSONObject tagDefinitionJS;

	public CTagDefinitionSerializationHandler(JSONObject tagDefinitionJS) {
		this.tagDefinitionJS = tagDefinitionJS;
	}

	public CTagDefinition toCTagDefinition() {
		
		CTagDefinition tagDefinition =
			new CTagDefinition(
				JSONUtil.getValueFromStringObject(tagDefinitionJS.get(
						TagDefinitionSerializationKey.id.name())),
				JSONUtil.getValueFromStringObject(tagDefinitionJS.get(
						TagDefinitionSerializationKey.type.name())),
				new CVersion(),
				JSONUtil.getValueFromStringObject(tagDefinitionJS.get(
						TagDefinitionSerializationKey.baseID.name())));
		
		JSONArray systemPropertyDefinitionsJS = 
			(JSONArray)tagDefinitionJS.get(
				TagDefinitionSerializationKey.systemPropertyDefinitions.name());
		
		for (int i=0; i<systemPropertyDefinitionsJS.size(); i++) {
			tagDefinition.addSystemPropertyDefinition(
				new CPropertyDefinitionSerializationHandler(
					(JSONObject)systemPropertyDefinitionsJS.get(i)).toPropertyDefinition());
		}
		
		JSONArray userDefinedPropertyDefinitionsJS = 
				(JSONArray)tagDefinitionJS.get(
					TagDefinitionSerializationKey.userDefinedPropertyDefinitions.name());
			
		for (int i=0; i<userDefinedPropertyDefinitionsJS.size(); i++) {
			tagDefinition.addUserDefinedPropertyDefinition(
				new CPropertyDefinitionSerializationHandler(
					(JSONObject)userDefinedPropertyDefinitionsJS.get(i)).toPropertyDefinition());
		}		
		
		return tagDefinition;
	}

}
