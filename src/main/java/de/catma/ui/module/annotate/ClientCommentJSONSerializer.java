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
import java.util.Collection;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.catma.document.comment.Comment;
import de.catma.ui.client.ui.tagger.shared.ClientComment;
import de.catma.ui.client.ui.tagger.shared.ClientComment.SerializationField;
import de.catma.ui.client.ui.tagger.shared.TextRange;

public class ClientCommentJSONSerializer {
	
	public String toJSON(ClientComment comment) 
			throws IOException {
		return new ObjectMapper().writeValueAsString(comment);
	}

	private ObjectNode toJSONObject(ClientComment comment) 
			throws IOException {
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ObjectNode result = factory.objectNode();
		result.put(SerializationField.uuid.name(), comment.getUuid());
		
		result.put(
			SerializationField.username.name(), 
			comment.getUsername());
		result.put(
			SerializationField.userId.name(),
			comment.getUserId());
		result.put(
				SerializationField.body.name(), 
				comment.getBody());
		result.put(
				SerializationField.replyCount.name(), 
				comment.getReplyCount());

		ArrayNode ranges = factory.arrayNode();
		
		for (TextRange range : comment.getRanges()) {
			ObjectNode rangeNode = factory.objectNode();
			rangeNode.put(SerializationField.startPos.name(), range.getStartPos());
			rangeNode.put(SerializationField.endPos.name(), range.getEndPos());
			ranges.add(rangeNode);
		}
		
		result.set(SerializationField.ranges.name(), ranges);
		
		return result;
	}
	
	
	public String toJSON(Collection<Comment> comments) throws IOException {
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ArrayNode collection = factory.arrayNode(comments.size());
		
		for (Comment comment : comments) {
			collection.add(
				toJSONObject(
					new ClientComment(
						comment.getUuid(), 
						comment.getUsername(), 
						comment.getUserId(), 
						comment.getBody(), 
						comment.getReplyCount(),
						comment.getRanges()
							.stream()
							.map(range -> new TextRange(range.getStartPoint(), range.getEndPoint()))
							.collect(Collectors.toList()))));
		}
		
		return new ObjectMapper().writeValueAsString(collection);
		
	}

}
