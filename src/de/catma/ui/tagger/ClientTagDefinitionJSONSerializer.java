package de.catma.ui.tagger;

import org.json.JSONException;
import org.json.JSONObject;

import de.catma.ui.client.ui.tagger.shared.ClientTagDefinition;

public class ClientTagDefinitionJSONSerializer {

	public String toJSON(ClientTagDefinition clientTagDefinition) 
			throws JSONSerializationException {
		return toJSONObject(clientTagDefinition).toString();
	}

	private JSONObject toJSONObject(ClientTagDefinition clientTagDefinition) 
			throws JSONSerializationException {
		try {
			JSONObject result = new JSONObject();
			result.put(
				ClientTagDefinition.SerializationField.tagDefinitionID.name(), 
				clientTagDefinition.getId());
			result.put(
				ClientTagDefinition.SerializationField.colorHexValue.name(),
				clientTagDefinition.getColor());
			return result;
		}
		catch (JSONException e) {
			throw new JSONSerializationException(e);
		}
	}
}
