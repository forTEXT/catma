package de.catma.project.event;

import de.catma.document.comment.Reply;

public class ReplyChangeEvent {
	
	private final ChangeType changeType;
	private final Reply reply;
	
	public ReplyChangeEvent(ChangeType changeType, Reply reply) {
		super();
		this.changeType = changeType;
		this.reply = reply;
	}
	public ChangeType getChangeType() {
		return changeType;
	}
	public Reply getReply() {
		return reply;
	}
	
	

}
