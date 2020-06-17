package de.catma.document.comment;

import de.catma.document.Range;

public class Comment {
	
	private final String username;
	private final Integer userId;
	
	private String body;
	private final Range range;
	private final String documentId;
	
	public Comment(String username, Integer userId, String body, Range range, String documentId) {
		super();
		this.username = username;
		this.userId = userId;
		this.body = body;
		this.range = range;
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
	
	public Range getRange() {
		return range;
	}
}
