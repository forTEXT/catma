package de.catma.ui.client.ui.tagger;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;

import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance.SerializationField;

public class TextRangeJSONSerializer extends JSONSerializer {

	public TextRange fromJSON(String jsonString) {
		JSONObject trJSON = (JSONObject)JSONParser.parseStrict(jsonString);
		TextRange tr = 
				new TextRange(
					getIntValueFromStringObject(
						trJSON.get(SerializationField.startPos.name())),
					getIntValueFromStringObject(
						trJSON.get(SerializationField.endPos.name())));
		return tr;
	}
}
