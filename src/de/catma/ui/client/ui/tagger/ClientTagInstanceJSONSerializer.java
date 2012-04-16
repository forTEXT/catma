package de.catma.ui.client.ui.tagger;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

import de.catma.ui.client.ui.tagger.shared.TagInstance;
import de.catma.ui.client.ui.tagger.shared.TagInstance.SerializationField;
import de.catma.ui.client.ui.tagger.shared.TextRange;

public class TagInstanceJSONSerializer {

	public List<TagInstance> fromJSONArray(String jsonArrayString) {
		JSONArray tagInstancesJSON = 
				(JSONArray)JSONParser.parseStrict(jsonArrayString);
		List<TagInstance> tagInstances = new ArrayList<TagInstance>();
		for (int i=0; i<tagInstancesJSON.size(); i++) {
			tagInstances.add(fromJSON((JSONObject)tagInstancesJSON.get(i)));
		}
		return tagInstances;
		
	}
	
	private TagInstance fromJSON(JSONObject jsonObject) {
		String instanceID = 
			getStringValueFromStringObject(
				jsonObject.get(SerializationField.instanceID.name()));
		String color = 
			getStringValueFromStringObject(
				jsonObject.get(SerializationField.color.name()));
		JSONArray rangesJSON = 
			(JSONArray)jsonObject.get(SerializationField.ranges.name());
		
		List<TextRange> ranges = new ArrayList<TextRange>();
		
		for (int i=0; i<rangesJSON.size(); i++) {
			JSONObject trJSON = (JSONObject)rangesJSON.get(i);
			TextRange tr = 
					new TextRange(
						getIntValueFromStringObject(
							trJSON.get(SerializationField.startPos.name())),
						getIntValueFromStringObject(
							trJSON.get(SerializationField.endPos.name())));
			ranges.add(tr);
		}
		
		return new TagInstance(instanceID, color, ranges);
	}

	public String toJSONObject(TagInstance tagInstance) {
		
		JSONObject tagInstanceJSON = new JSONObject();
		
		tagInstanceJSON.put(
				SerializationField.instanceID.name(), 
				new JSONString(tagInstance.getInstanceID()));
		tagInstanceJSON.put(
				SerializationField.color.name(), 
				new JSONString(tagInstance.getColor()));		
		
		JSONArray rangesJSON = new JSONArray();
		int i=0;
		for (TextRange tr : tagInstance.getRanges()) {
			JSONObject trJSON = new JSONObject();
			trJSON.put(
				SerializationField.startPos.name(), 
				new JSONNumber(tr.getStartPos()));
			trJSON.put(
				SerializationField.endPos.name(), 
				new JSONNumber(tr.getEndPos()));
			rangesJSON.set(i, trJSON);
			i++;
		}
		
		tagInstanceJSON.put(
				SerializationField.ranges.name(), rangesJSON);
		
		return tagInstanceJSON.toString();
	}
	
	private int getIntValueFromStringObject(JSONValue jsonValue) {
		if (jsonValue != null) {
			double result = ((JSONNumber)jsonValue).doubleValue();
			return Double.valueOf(result).intValue();
		}
		else {
			throw new IllegalArgumentException("jsonValue cannot be null");
		}
	}
	
	private String getStringValueFromStringObject(JSONValue jsonValue) {
		if (jsonValue != null) {
			String result = ((JSONString)jsonValue).stringValue();
			return result;
		}
		else {
			return null;
		}
	}
	
}
