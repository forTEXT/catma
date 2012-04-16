package de.catma.ui.tagger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.catma.ui.client.ui.tagger.shared.TagInstance;
import de.catma.ui.client.ui.tagger.shared.TagInstance.SerializationField;
import de.catma.ui.client.ui.tagger.shared.TextRange;

public class TagInstanceJSONSerializer {
	
	// somehow the 
	public static class JSONSerializationException extends Exception {

		public JSONSerializationException(Throwable cause) {
			super(cause);
		}
		
	}

	public TagInstance fromJSON(String json) throws JSONSerializationException {
		try {
			JSONObject tagInstanceJSON = new JSONObject(json);
			String instanceID = 
					tagInstanceJSON.getString(SerializationField.instanceID.name());
			String color =
					tagInstanceJSON.getString(SerializationField.color.name());
			List<TextRange> ranges = new ArrayList<TextRange>();
			JSONArray rangesJSON = 
					tagInstanceJSON.getJSONArray(SerializationField.ranges.name());
			
			for (int i=0; i<rangesJSON.length(); i++) {
				JSONObject trJSON = (JSONObject)rangesJSON.get(i);
				
				TextRange tr = 
						new TextRange(
								trJSON.getInt(SerializationField.startPos.name()), 
								trJSON.getInt(SerializationField.endPos.name()));
				ranges.add(tr);
			}		
			
			return new TagInstance(instanceID, color, ranges);
		}
		catch(JSONException e) {
			throw new JSONSerializationException(e);
		}
	}
	
	private JSONObject toJSONObject(TagInstance tagInstance) throws JSONSerializationException {
		try {
			JSONObject tagInstanceJSON = new JSONObject();
			tagInstanceJSON.put(
				SerializationField.instanceID.name(), 
				tagInstance.getInstanceID());
			
			tagInstanceJSON.put(
					SerializationField.color.name(), 
					tagInstance.getColor());
	
			JSONArray rangesJSON = new JSONArray();
			tagInstanceJSON.put(
					SerializationField.ranges.name(), 
					rangesJSON);
			
	
			for (TextRange tr : tagInstance.getRanges()) {
				JSONObject trJSON = new JSONObject();
				trJSON.put(SerializationField.startPos.name(), tr.getStartPos());
				trJSON.put(SerializationField.endPos.name(), tr.getEndPos());
				rangesJSON.put(trJSON);
			}
			
			return tagInstanceJSON;
		}
		catch (JSONException e) {
			throw new JSONSerializationException(e);
		}
	}
	
	public String toJSON(TagInstance tagInstance) throws JSONSerializationException {
		return toJSONObject(tagInstance).toString();
	}
	
	public String toJSON(Collection<TagInstance> tagInstances) 
			throws JSONSerializationException {
		JSONArray tagInstancesJSON = new JSONArray();
		for (TagInstance ti : tagInstances) {
			tagInstancesJSON.put(toJSONObject(ti));
		}
	
		return tagInstancesJSON.toString();
	}

}
