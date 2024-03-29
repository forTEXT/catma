package de.catma.document.comment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.catma.document.Range;

public class Comment {
	
	private final String uuid;
	private String body;
	private final List<Range> ranges;
	private final String documentId;
	
	private transient String username;
	private transient Long userId;
	private transient Long id;
	private transient Long iid;
	private transient int replyCount;
	private transient List<Reply> replies = new ArrayList<Reply>();

	public Comment(String uuid, String username, Long userId, String body, List<Range> ranges, String documentId) {
		this(uuid, username, userId, body, ranges, documentId, null, null);
	}

	public Comment(String uuid, String username, Long userId, String body, List<Range> ranges, String documentId, Long id, Long iid) {
		super();
		this.uuid = uuid;
		this.username = username;
		this.userId = userId;
		this.body = body;
		this.ranges = ranges;
		this.documentId = documentId;
		this.id = id;
		this.iid = iid;
	}
	
	public Comment(Comment comment, int base) {

		this(comment.uuid, comment.username, comment.userId, comment.body, 
			comment.getRanges()
				.stream()
				.map(range -> new Range(range.getStartPoint()+base, range.getEndPoint()+base))
				.collect(Collectors.toList()),
			comment.documentId,
			comment.id,
			comment.iid);
		this.replyCount = comment.replyCount;
	}

	public String getUsername() {
		return username;
	}
	
	public Long getUserId() {
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
	
	public Long getId() {
		return id;
	}
	
	public Long getIid() {
		return iid;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setBody(String body) {
		this.body = body;
	}
	
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public void setIid(Long iid) {
		this.iid = iid;
	}
	
	@Override
	public String toString() {
		return "Comment " + uuid + " #" + id + " by " + username + " for " + documentId + " " + body;
	}

	public void setReplyCount(int replyCount) {
		this.replyCount = replyCount;
	}
	
	public int getReplyCount() {
		return replyCount;
	}

	public int getStartPos() {
		return ranges.stream().map(Range::getStartPoint).sorted().findFirst().orElse(-1);
	}
	
	public List<Reply> getReplies() {
		return replies;
	}
	
	public void removeReply(Reply reply) {
		this.replies.remove(reply);
		this.replyCount--;
	}
	
	public void setReplies(List<Reply> replies) {
		this.replies = replies;
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
		if (!(obj instanceof Comment))
			return false;
		Comment other = (Comment) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

	public Reply getReply(String replyUuid) {
		for (Reply reply : replies) {
			if (reply.getUuid().equals(replyUuid)) {
				return reply;
			}
		}
		return null;
	}

	public void addReply(Reply reply) {
		replies.add(reply);
		this.replyCount++;
	}
	
	
}
