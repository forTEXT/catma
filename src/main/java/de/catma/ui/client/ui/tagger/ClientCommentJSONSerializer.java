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
import java.util.Collection;
import java.util.List;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;

import de.catma.ui.client.ui.tagger.shared.ClientComment;
import de.catma.ui.client.ui.tagger.shared.ClientComment.SerializationField;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import de.catma.ui.client.ui.util.JSONSerializer;

public class ClientCommentJSONSerializer extends JSONSerializer {

	public List<ClientComment> fromJSONArray(String jsonArrayString) {
		JSONArray commentsJSON = 
				(JSONArray)JSONParser.parseStrict(jsonArrayString);
		List<ClientComment> comments = new ArrayList<ClientComment>();
		for (int i=0; i<commentsJSON.size(); i++) {
			comments.add(fromJSON((JSONObject)commentsJSON.get(i)));
		}
		return comments;
		
	}
	
	public ClientComment fromJSONString(String jsonObject) {
		return fromJSON((JSONObject) JSONParser.parseStrict(jsonObject));
	}
	
	public ClientComment fromJSON(JSONObject jsonObject) {
		String username = 
			getStringValueFromStringObject(
				jsonObject.get(SerializationField.username.name()));
				
		Integer userId = 
			getIntValueFromStringObject(
				jsonObject.get(SerializationField.userId.name()));
		String body = 
			getStringValueFromStringObject(
				jsonObject.get(SerializationField.body.name()));
		
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
		
		return new ClientComment(username, userId, body, ranges);
	}

	public String toJSONObjectString(ClientComment comment) {
		return toJSONObject(comment).toString();
	}
	
	public JSONObject toJSONObject(ClientComment comment) {
		
		JSONObject commentJSON = new JSONObject();
		
		commentJSON.put(
				SerializationField.username.name(),
				new JSONString(comment.getUsername()));
		
		commentJSON.put(
				SerializationField.userId.name(), 
				new JSONNumber(comment.getUserId().doubleValue()));
		commentJSON.put(
				SerializationField.body.name(), 
				new JSONString(comment.getBody()));		
		
		TextRangeJSONSerializer textRangeJSONSerializer = new TextRangeJSONSerializer();
		JSONArray rangesJSON = textRangeJSONSerializer.toJSONArray(comment.getRanges());
		
		commentJSON.put(
				SerializationField.ranges.name(), rangesJSON);
		
		return commentJSON;
	}


	public String toJSONArrayString(Collection<ClientComment> comments) {
		JSONArray result = new JSONArray();
		int i=0;
		for (ClientComment comment : comments) {
			result.set(i, toJSONObject(comment));
			i++;
		}
		return result.toString();
	}
}
