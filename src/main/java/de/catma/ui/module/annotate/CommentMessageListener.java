package de.catma.ui.module.annotate;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hazelcast.topic.Message;
import com.vaadin.ui.UI;

import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.document.source.SourceDocumentReference;
import de.catma.project.Project;
import de.catma.ui.UIMessageListener;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.CommentMessage;
import de.catma.user.User;

public class CommentMessageListener extends UIMessageListener<CommentMessage> {
	
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	
	private final Supplier<SourceDocumentReference> documentSupplier;

	private final IconButton cbAutoShowComments;

	private final Project project;

	private final List<Comment> comments;

	private final Tagger tagger;


	
	public CommentMessageListener(
			UI ui, Project project, IconButton cbAutoShowComments, List<Comment> comments,
			Tagger tagger, Supplier<SourceDocumentReference> documentSupplier) {
		super(ui);
		this.project = project;
		this.cbAutoShowComments = cbAutoShowComments;
		this.comments = comments;
		this.tagger = tagger;
		this.documentSupplier = documentSupplier;
	}


	@Override
	public void uiOnMessage(Message<CommentMessage> message) {
		boolean autoShowcomments = (boolean) cbAutoShowComments.getData();
		if (!autoShowcomments) {
			return;
		}
		
		try {
			CommentMessage commentMessage = message.getMessageObject();
			final String documentId = commentMessage.getDocumentId();
			final boolean replyMessage = commentMessage.isReplyMessage();
			final long senderId = commentMessage.getSenderId();
			final boolean deleted= commentMessage.isDeleted();
			final SourceDocumentReference document = documentSupplier.get();
			if ((document != null) 
					&& document.getUuid().equals(documentId)) {
				User user = project.getCurrentUser();
				Long receiverId = user.getUserId();
				
				if (!receiverId.equals(senderId)) {
					final Comment comment = commentMessage.toComment();
					Optional<Comment> optionalExistingComment = 
						this.comments
							.stream()
							.filter(c -> c.getUuid().equals(comment.getUuid()))
							.findFirst();

					if (replyMessage) {
						if (optionalExistingComment.isPresent()) {
							Comment existingComment = optionalExistingComment.get();
							Reply reply = commentMessage.toReply();
							Reply existingReply = existingComment.getReply(reply.getUuid());

							if (existingReply != null) {
								if (deleted) {
									existingComment.removeReply(existingReply);
									tagger.removeReply(existingComment, existingReply);
								}
								else {
									existingReply.setBody(reply.getBody());
									tagger.updateReply(existingComment, existingReply);
								}
							}
							else {
								existingComment.addReply(reply);
								tagger.addReply(existingComment, reply);
							}
						}
					}
					else {
						if (deleted) {
							optionalExistingComment.ifPresent(existingComment -> {
								this.comments.remove(existingComment);
								tagger.removeComment(existingComment);
							});
						}
						else {
							if (optionalExistingComment.isPresent()) {
								Comment existingComment = optionalExistingComment.get();
								existingComment.setBody(comment.getBody());
								tagger.updateComment(existingComment);
							}
							else {
								comments.add(comment);
								tagger.addComment(comment);
							}
						}
						
					}
					
					getUi().push();
				}
			}
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Error processing an incoming comment", e);
		}
	}

}
