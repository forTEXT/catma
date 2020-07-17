package de.catma.ui.client.ui.tagger;

import java.util.Collection;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

import de.catma.document.comment.Reply;
import de.catma.ui.client.ui.tagger.shared.ClientCommentReply.SerializationField;

public class CommentReplyJSONSerializer {

	public JSONObject toJSONObject(Reply reply) {
		
		JSONObject commentReplyJSON = new JSONObject();
		commentReplyJSON.put(
				SerializationField.uuid.name(), 
				new JSONString(reply.getUuid()));
		commentReplyJSON.put(
				SerializationField.username.name(),
				new JSONString(reply.getUsername()));
		
		commentReplyJSON.put(
				SerializationField.userId.name(), 
				new JSONNumber(reply.getUserId().doubleValue()));
		commentReplyJSON.put(
				SerializationField.body.name(), 
				new JSONString(reply.getBody()));
		
		commentReplyJSON.put(
			SerializationField.commentUuid.name(),
			new JSONString(reply.getCommentUuid()));
		
		
		return commentReplyJSON;
	}


	public String toJSONArrayString(Collection<Reply> replies) {
		JSONArray result = new JSONArray();
		int i=0;
		for (Reply reply : replies) {
			result.set(i, toJSONObject(reply));
			i++;
		}
		return result.toString();
	}

}
