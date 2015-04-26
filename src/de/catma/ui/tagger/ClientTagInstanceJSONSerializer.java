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
package de.catma.ui.tagger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance.SerializationField;
import de.catma.ui.client.ui.tagger.shared.TextRange;

public class ClientTagInstanceJSONSerializer {
	
	public List<String> fromInstanceIDJSONArray(String jsonArray) throws IOException {
		List<String> result = new ArrayList<String>();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode instanceIDArray = mapper.readValue(jsonArray, ArrayNode.class);
		
		for (int i=0; i<instanceIDArray.size(); i++) {
			JsonNode instanceIDJSON = instanceIDArray.get(i);
			result.add(
				instanceIDJSON.get(
						SerializationField.instanceID.name()).asText());
		}
		
		return result;
	}
	
	public ClientTagInstance fromJSON(String json) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode tagInstanceJSON = mapper.readValue(json, ObjectNode.class);
		String tagDefinitionID =
				tagInstanceJSON.get(SerializationField.tagDefinitionID.name()).asText();
		String instanceID = 
				tagInstanceJSON.get(SerializationField.instanceID.name()).asText();
		String color =
				tagInstanceJSON.get(SerializationField.color.name()).asText();
		List<TextRange> ranges = new ArrayList<TextRange>();
		JsonNode rangesJSON = 
				tagInstanceJSON.get(SerializationField.ranges.name());
		
		for (JsonNode trJSON : rangesJSON) {
			
			
			TextRange tr = 
					new TextRange(
							trJSON.get(SerializationField.startPos.name()).asInt(), 
							trJSON.get(SerializationField.endPos.name()).asInt());
			ranges.add(tr);
		}		
		
		return new ClientTagInstance(tagDefinitionID, instanceID, color, ranges);
	}
	
	private ObjectNode toJSONObject(ClientTagInstance tagInstance) throws IOException {
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ObjectNode tagInstanceJSON = factory.objectNode();
		tagInstanceJSON.put(
			SerializationField.tagDefinitionID.name(),
			tagInstance.getTagDefinitionID());
		tagInstanceJSON.put(
			SerializationField.instanceID.name(), 
			tagInstance.getInstanceID());
		
		tagInstanceJSON.put(
				SerializationField.color.name(), 
				tagInstance.getColor());

		ArrayNode rangesJSON = factory.arrayNode();
		tagInstanceJSON.set(
				SerializationField.ranges.name(), 
				rangesJSON);
		

		for (TextRange tr : tagInstance.getRanges()) {
			ObjectNode trJSON = factory.objectNode();
			trJSON.put(SerializationField.startPos.name(), tr.getStartPos());
			trJSON.put(SerializationField.endPos.name(), tr.getEndPos());
			rangesJSON.add(trJSON);
		}
		
		return tagInstanceJSON;
	}
	
	public String toJSON(ClientTagInstance tagInstance) throws IOException {
		return toJSONObject(tagInstance).toString();
	}
	
	public String toJSON(Collection<ClientTagInstance> tagInstances) 
			throws IOException {
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ArrayNode tagInstancesJSON = factory.arrayNode();
		for (ClientTagInstance ti : tagInstances) {
			tagInstancesJSON.add(toJSONObject(ti));
		}
	
		return tagInstancesJSON.toString();
	}

	public String join(
			String jsonArray, Collection<ClientTagInstance> tagInstances) 
					throws IOException {
		if (jsonArray == null) {
			jsonArray = "[]";
		}
		ObjectMapper mapper = new ObjectMapper();
		
		ArrayNode tagInstancesJSON = mapper.readValue(jsonArray, ArrayNode.class);
		
		for (ClientTagInstance ti : tagInstances) {
			tagInstancesJSON.add(toJSONObject(ti));
		}
		return tagInstancesJSON.toString();
	}
}
