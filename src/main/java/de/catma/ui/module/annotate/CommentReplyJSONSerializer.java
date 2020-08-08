package de.catma.ui.module.annotate;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.catma.document.comment.Reply;
import de.catma.ui.client.ui.tagger.shared.ClientCommentReply.SerializationField;

public class CommentReplyJSONSerializer {

	public ObjectNode toJSONObject(Reply reply) {
		
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ObjectNode result = factory.objectNode();
		result.put(SerializationField.uuid.name(), reply.getUuid());
		

		result.put(
				SerializationField.username.name(),
				reply.getUsername());
		
		result.put(
				SerializationField.userId.name(), 
				reply.getUserId().doubleValue());
		result.put(
				SerializationField.body.name(), 
				reply.getBody());
		
		result.put(
			SerializationField.commentUuid.name(),
			reply.getCommentUuid());
		
		
		return result;
	}


	public String toJSONArrayString(Collection<Reply> replies) throws IOException {
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ArrayNode collection = factory.arrayNode(replies.size());

		for (Reply reply : replies) {
			collection.add(toJSONObject(reply));
		}
		return new ObjectMapper().writeValueAsString(collection);
	}

}
