package de.catma.ui.client.ui.tagger.shared;

import java.util.List;

public class ClientComment {
	
	public static enum SerializationField {
		username,
		userId,
		ranges,
		startPos,
		endPos,
		body, 
		uuid,
		;
	}
	
	private final String uuid;
	private final String username;
	private final Integer userId;
	private final List<TextRange> ranges;
	
	private String body;
	
	public ClientComment(String uuid, String username, Integer userId, String body, List<TextRange> ranges) {
		super();
		this.uuid = uuid;
		this.username = username;
		this.userId = userId;
		this.body = body;
		this.ranges = ranges;
		ranges.sort(new TextRangeComparator());
	}
	
	public String getUuid() {
		return uuid;
	}

	public String getUsername() {
		return username;
	}

	public Integer getUserId() {
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
	
}
