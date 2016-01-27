/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui.client.ui.tagger;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;

import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance.SerializationField;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.ui.client.ui.util.JSONSerializer;

public class ClientTagInstanceJSONSerializer extends JSONSerializer {

	public List<ClientTagInstance> fromJSONArray(String jsonArrayString) {
		JSONArray tagInstancesJSON = 
				(JSONArray)JSONParser.parseStrict(jsonArrayString);
		List<ClientTagInstance> tagInstances = new ArrayList<ClientTagInstance>();
		for (int i=0; i<tagInstancesJSON.size(); i++) {
			tagInstances.add(fromJSON((JSONObject)tagInstancesJSON.get(i)));
		}
		return tagInstances;
		
	}
	
	private ClientTagInstance fromJSON(JSONObject jsonObject) {
		String tagDefID = 
			getStringValueFromStringObject(
				jsonObject.get(SerializationField.tagDefinitionID.name()));
				
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
		
		return new ClientTagInstance(tagDefID, instanceID, color, ranges);
	}

	public String toJSONObject(ClientTagInstance tagInstance) {
		
		JSONObject tagInstanceJSON = new JSONObject();
		
		tagInstanceJSON.put(
				SerializationField.tagDefinitionID.name(),
				new JSONString(tagInstance.getTagDefinitionID()));
		
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
//	
//	public String toJSONArray(String tagInstanceIDs) {
//		int i=0;
//		JSONArray result = new JSONArray();
//		for (String tagInstanceID : tagInstanceIDs) {
//			JSONObject instanceJSON = new JSONObject();
//			instanceJSON.put(
//				SerializationField.instanceID.name(), 
//				new JSONString(tagInstanceID));
//			result.set(i, instanceJSON);
//			i++;
//		}
//		
//		return result.toString();
//	}
	
	public String toJSONArray(String tagInstanceID, String lineID) {
		
		JSONArray result = new JSONArray();
		JSONObject instanceJSON = new JSONObject();
		instanceJSON.put(
			SerializationField.instanceID.name(), 
			new JSONString(tagInstanceID));
		result.set(0, instanceJSON);
			
		instanceJSON.put(
			SerializationField.lineID.name(), 
			new JSONString(lineID));
		result.set(1, instanceJSON);
		
		return result.toString();
	}
}
