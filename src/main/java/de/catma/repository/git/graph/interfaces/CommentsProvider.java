package de.catma.repository.git.graph.interfaces;

import java.util.List;

import de.catma.document.comment.Comment;

public interface CommentsProvider {
	public List<Comment> getComments(List<String> documentIds) throws Exception;
}
