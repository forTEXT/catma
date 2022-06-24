package de.catma.document.comment;

public class Reply {

	private final String uuid;
	private String body;
	
	private transient String username;
	private transient Long userId;
	private transient String commentUuid;
	private transient Long id;
	
	public Reply(String uuid, String body, String username, Long userId, String commentUuid) {
		this(uuid, body, username, userId, commentUuid, null);
	}
	
	public Reply(String uuid, String body, String username, Long userId, String commentUuid, Long id) {
		super();
		this.uuid = uuid;
		this.body = body;
		this.username = username;
		this.userId = userId;
		this.commentUuid = commentUuid;
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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

	public Long getUserId() {
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

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public void setCommentUuid(String commentUuid) {
		this.commentUuid = commentUuid;
	}
	
	@Override
	public String toString() {
		return "Reply #" + id + " " + uuid + " by " + username + " for #" + commentUuid + " " + body;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Reply))
			return false;
		Reply other = (Reply) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
	
	
	
}
