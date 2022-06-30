package de.catma.ui.client.ui.tagger.shared;

import java.io.Serializable;

public class ClientCommentReply implements Serializable {
	
	private static final long serialVersionUID = 4432309957370280636L;

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
	private Long userId;
	private String commentUuid;
	
	public ClientCommentReply(String uuid, String body, String username, Long userId, String commentUuid) {
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

	public Long getUserId() {
		return userId;
	}

	public String getCommentUuid() {
		return commentUuid;
	}
	
}
