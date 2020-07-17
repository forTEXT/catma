package de.catma.document.comment;

public class Reply {

	private final String uuid;
	private String body;
	
	private transient String username;
	private transient Integer userId;
	private transient String commentUuid;
	private transient Integer id;
	
	public Reply(String uuid, String body, String username, Integer userId, String commentUuid) {
		this(uuid, body, username, userId, commentUuid, null);
	}
	
	public Reply(String uuid, String body, String username, Integer userId, String commentUuid, Integer id) {
		super();
		this.uuid = uuid;
		this.body = body;
		this.username = username;
		this.userId = userId;
		this.commentUuid = commentUuid;
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUuid() {
		return uuid;
	}

	public String getBody() {
		return body;
	}

	public String getUsername() {
		return username;
	}

	public Integer getUserId() {
		return userId;
	}

	public String getCommentUuid() {
		return commentUuid;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public void setCommentUuid(String commentUuid) {
		this.commentUuid = commentUuid;
	}
	
	@Override
	public String toString() {
		return "Reply #" + id + " " + uuid + " by " + username + " for #" + commentUuid + " " + body;
	}
	
}
