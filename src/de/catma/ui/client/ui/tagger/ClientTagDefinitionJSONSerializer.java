package de.catma.ui.client.ui.tagger;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;

import de.catma.ui.client.ui.tagger.shared.ClientTagDefinition;

public class ClientTagDefinitionJSONSerializer extends JSONSerializer {

	public ClientTagDefinition fromJSON(String tagDefJSONString) {
		JSONObject tagDefJSON =
				(JSONObject)JSONParser.parseStrict(tagDefJSONString);
		
		String id = getStringValueFromStringObject(
			tagDefJSON.get(
				ClientTagDefinition.SerializationField.tagDefinitionID.name()));
		String hexColorString = getStringValueFromStringObject(
			tagDefJSON.get(
				ClientTagDefinition.SerializationField.colorHexValue.name()));
		return new ClientTagDefinition(id, hexColorString);
	}
}
