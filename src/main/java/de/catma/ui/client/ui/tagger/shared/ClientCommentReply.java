package de.catma.ui.client.ui.tagger.shared;

public class ClientCommentReply {

	public static enum SerializationField {
		username,
		userId,
		body, 
		uuid,
		commentUuid,
		;
	}
	
	private final String uuid;
	private String body;
	private String username;
	private Integer userId;
	private String commentUuid;
	
	public ClientCommentReply(String uuid, String body, String username, Integer userId, String commentUuid) {
		super();
		this.uuid = uuid;
		this.body = body;
		this.username = username;
		this.userId = userId;
		this.commentUuid = commentUuid;
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

	
	
}
