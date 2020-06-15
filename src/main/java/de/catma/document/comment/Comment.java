package de.catma.document.comment;

public class Comment {
	private String userName;
	private Integer userId;
	private String body;
	public Comment(String userName, Integer userId, String body) {
		super();
		this.userName = userName;
		this.userId = userId;
		this.body = body;
	}
	public String getUserName() {
		return userName;
	}
	public Integer getUserId() {
		return userId;
	}
	public String getBody() {
		return body;
	}
}
