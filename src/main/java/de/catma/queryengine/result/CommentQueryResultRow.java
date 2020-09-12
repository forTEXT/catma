package de.catma.queryengine.result;

import de.catma.document.Range;
import de.catma.document.comment.Comment;
import de.catma.queryengine.QueryId;

public class CommentQueryResultRow extends QueryResultRow {

	private Comment comment;

	public CommentQueryResultRow(QueryId queryId, Comment comment) {
		super(queryId, comment.getDocumentId(), Range.getEnclosingRange(comment.getRanges()));
		this.comment = comment;
	}
	
	public Comment getComment() {
		return comment;
	}

}
