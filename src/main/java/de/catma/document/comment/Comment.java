package de.catma.document.comment;

import java.util.List;

import de.catma.document.Range;

public class Comment {
	
	private final String username;
	private final Integer userId;
	
	private String body;
	private final List<Range> ranges;
	private final String documentId;
	
	public Comment(String username, Integer userId, String body, List<Range> ranges, String documentId) {
		super();
		this.username = username;
		this.userId = userId;
		this.body = body;
		this.ranges = ranges;
		this.documentId = documentId;
	}
	
	public String getUsername() {
		return username;
	}
	
	public Integer getUserId() {
		return userId;
	}
	
	public String getBody() {
		return body;
	}
	
	public String getDocumentId() {
		return documentId;
	}
	
	public List<Range> getRanges() {
		return ranges;
	}
}
