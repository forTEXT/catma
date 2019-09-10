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
package de.catma.ui.module.annotate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance.SerializationField;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.util.Pair;

public class ClientTagInstanceJSONSerializer {
	
	public Pair<String, String> fromInstanceIDLineIDJSONArray(String instanceIDLineIDJSONArray) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode instanceIDArray = mapper.readValue(instanceIDLineIDJSONArray, ArrayNode.class);
	
		JsonNode instanceIDJSON = instanceIDArray.get(0);
		String instanceID = 
			instanceIDJSON.get(
					SerializationField.instanceID.name()).asText();
		
		String lineID = 
			instanceIDJSON.get(
					SerializationField.lineID.name()).asText();
		
		return new Pair<>(instanceID, lineID);
	}
	
	public Set<String> fromInstanceIDsArray(String tagInstanceIDsJson) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode instanceIDArray = mapper.readValue(tagInstanceIDsJson, ArrayNode.class);
		HashSet<String> tagInstanceIDs = new HashSet<>();
		
		for (int i=0; i<instanceIDArray.size(); i++){
			tagInstanceIDs.add(instanceIDArray.get(i).asText());
		}
		
		return tagInstanceIDs;
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

}
