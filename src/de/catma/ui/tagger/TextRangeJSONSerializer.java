package de.catma.ui.tagger;

import org.json.JSONException;
import org.json.JSONObject;

import de.catma.ui.client.ui.tagger.shared.ClientTagInstance.SerializationField;
import de.catma.ui.client.ui.tagger.shared.TextRange;

public class TextRangeJSONSerializer {
	
	public String toJSON(TextRange textRange) throws JSONSerializationException {
		try {
			JSONObject trJSON = new JSONObject();
			trJSON.put(SerializationField.startPos.name(), textRange.getStartPos());
			trJSON.put(SerializationField.endPos.name(), textRange.getEndPos());
			return trJSON.toString();
		}
		catch (JSONException e) {
			throw new JSONSerializationException(e);
		}
	}
	
	

}
