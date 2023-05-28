package de.catma.repository.git.graph.interfaces;

import de.catma.document.comment.Comment;

import java.util.List;

public interface CommentsProvider {
	List<Comment> getComments(List<String> documentIds) throws Exception;
}
