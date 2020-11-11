package de.catma.ui.events;

import java.io.Serializable;
import java.util.stream.Collectors;

import de.catma.document.Range;
import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.ui.client.ui.tagger.shared.ClientComment;
import de.catma.ui.client.ui.tagger.shared.ClientCommentReply;

public class CommentMessage implements Serializable {
	
	private static final long serialVersionUID = 4432309957370280636L;
	
	private final int commentId;
	private final int commentIid;
	private final ClientComment clientComment;
	
	private Integer replyId;
	private ClientCommentReply clientCommentReply;

	private final String documentId;


	private final boolean deleted;

	private final Integer senderId;
	

	public CommentMessage(
			int commentId, 
			int commentIid,
			Integer senderId, ClientComment clientComment, 
			String sourceDocumentId, 
			boolean deleted) {
		super();
		this.commentId = commentId;
		this.commentIid = commentIid;
		this.senderId = senderId;
		this.clientComment = clientComment;
		this.documentId = sourceDocumentId;
		this.deleted = deleted;
	}
	
	public CommentMessage(
			int commentId,
			int commentIid,
			Integer senderId,
			ClientComment clientComment, 
			String sourceDocumentId, 
			boolean deleted,
			int replyId,
			ClientCommentReply clientCommentReply) {
		this(commentId, commentIid, senderId, clientComment, sourceDocumentId, deleted);
		this.replyId = replyId;
		this.clientCommentReply = clientCommentReply;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public int getCommentId() {
		return commentId;
	}

	public Integer getUserId() {
		return clientComment.getUserId();
	}

	public String getDocumentId() {
		return documentId;
	}
	
	public boolean isReplyMessage() {
		return this.clientCommentReply != null;
	}
	
	public Comment toComment() {
		Comment comment = new Comment(
			this.clientComment.getUuid(), 
			this.clientComment.getUsername(), 
			this.clientComment.getUserId(), 
			this.clientComment.getBody(), 
			this.clientComment.getRanges()
				.stream()
				.map(tr -> new Range(tr.getStartPos(), tr.getEndPos()))
				.collect(Collectors.toList()),
			this.documentId,
			this.commentId,
			this.commentIid);
		
		return comment;
	}
	
	public Reply toReply() {
		if (this.clientCommentReply == null) {
			return null;
		}
		
		Reply reply = new Reply(
				this.clientCommentReply.getUuid(), 
				this.clientCommentReply.getBody(), 
				this.clientCommentReply.getUsername(), 
				this.clientCommentReply.getUserId(), 
				this.clientCommentReply.getCommentUuid(),
				this.replyId);
		return reply;
	}

	public Integer getReplyUserId() {
		return this.clientCommentReply.getUserId();
	}
	
	public Integer getSenderId() {
		return senderId;
	}

}
