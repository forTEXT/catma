package de.catma.repository.git.graph.interfaces;

import java.util.List;

import de.catma.document.comment.Comment;

public interface CommentProvider {
	public List<Comment> getComments(List<String> documentIdList) throws Exception;
}
