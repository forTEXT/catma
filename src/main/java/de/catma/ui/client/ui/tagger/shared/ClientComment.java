package de.catma.ui.client.ui.tagger.shared;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class ClientComment implements Serializable {
	
	private static final long serialVersionUID = 4432309957370280636L;
	
	public static enum SerializationField {
		username,
		userId,
		ranges,
		startPos,
		endPos,
		body, 
		replyCount,
		uuid,
		;
	}
	
	private final String uuid;
	private final String username;
	private final Long userId;
	private final List<TextRange> ranges;
	private int replyCount;
	
	private String body;
	private List<ClientCommentReply> replies;
	
	public ClientComment(
			String uuid, String username, Long userId, 
			String body, int replyCount, List<TextRange> ranges) {
		super();
		this.uuid = uuid;
		this.username = username;
		this.userId = userId;
		this.body = body;
		this.replyCount = replyCount;
		this.ranges = ranges;
		ranges.sort(new TextRangeComparator());
	}
	
	public String getUuid() {
		return uuid;
	}

	public String getUsername() {
		return username;
	}

	public Long getUserId() {
		return userId;
	}

	public List<TextRange> getRanges() {
		return ranges;
	}
	
	public String getBody() {
		return body;
	}
	
	public void setBody(String body) {
		this.body = body;
	}
	
	public int getReplyCount() {
		return replyCount;
	}
	
	public void setReplyCount(int replyCount) {
		this.replyCount = replyCount;
	}

	public void setReplies(List<ClientCommentReply> replies) {
		this.replies = replies;
	}
	
	public List<ClientCommentReply> getReplies() {
		return replies==null?Collections.emptyList():replies;
	}
}
