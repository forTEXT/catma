package de.catma.project.event;

import de.catma.document.comment.Comment;

public class CommentChangeEvent {
	
	private final ChangeType changeType;
	private final Comment comment;
	
	public CommentChangeEvent(ChangeType changeType, Comment comment) {
		super();
		this.changeType = changeType;
		this.comment = comment;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

	public Comment getComment() {
		return comment;
	}
	
	

}
