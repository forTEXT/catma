package de.catma.ui.client.ui.tagger;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;

import de.catma.ui.client.ui.tagger.shared.ClientCommentReply;
import de.catma.ui.client.ui.tagger.shared.ClientCommentReply.SerializationField;
import de.catma.ui.client.ui.util.JSONSerializer;

public class ClientCommentReplyJSONSerializer extends JSONSerializer {

	public List<ClientCommentReply> fromJSONArray(String jsonArrayString) {
		List<ClientCommentReply> replies = new ArrayList<ClientCommentReply>();
		if (jsonArrayString != null && !jsonArrayString.isEmpty()) {
			JSONArray repliesJSON = 
					(JSONArray)JSONParser.parseStrict(jsonArrayString);
			for (int i=0; i<repliesJSON.size(); i++) {
				replies.add(fromJSON((JSONObject)repliesJSON.get(i)));
			}
		}
		return replies;
		
	}
	
	public ClientCommentReply fromJSON(JSONObject jsonObject) {
		String uuid = 
			getStringValueFromStringObject(
				jsonObject.get(SerializationField.uuid.name()));
				
		String username = 
			getStringValueFromStringObject(
				jsonObject.get(SerializationField.username.name()));
				
		Long userId =
			getLongValueFromNumberObject(
				jsonObject.get(SerializationField.userId.name()));
		String body = 
			getStringValueFromStringObject(
				jsonObject.get(SerializationField.body.name()));

		String commentUuid = 
				getStringValueFromStringObject(
					jsonObject.get(SerializationField.commentUuid.name()));

		
		return new ClientCommentReply(uuid, body, username, userId, commentUuid);
	}

}
