package de.catma.project.event;

import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;

public class ReplyChangeEvent {
	
	private final ChangeType changeType;
	private final Comment comment;
	private final Reply reply;

	public ReplyChangeEvent(ChangeType changeType, Comment comment, Reply reply) {
		super();
		this.changeType = changeType;
		this.comment = comment;
		this.reply = reply;
	}
	public ChangeType getChangeType() {
		return changeType;
	}
	public Reply getReply() {
		return reply;
	}
	
	public Comment getComment() {
		return comment;
	}

}
