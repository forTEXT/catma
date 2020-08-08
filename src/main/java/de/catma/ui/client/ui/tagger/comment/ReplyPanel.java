package de.catma.ui.client.ui.tagger.comment;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;

import de.catma.ui.client.ui.tagger.shared.ClientCommentReply;

public class ReplyPanel extends FlowPanel {

	private ClientCommentReply reply;
	private HTML content;
	private FlowPanel buttonPanel;
	private EditCommentButton btEdit;
	private RemoveCommentButton btRemove;

	public ReplyPanel(ClientCommentReply reply) {
		this.reply = reply;
		initComponents();
	}

	private void initComponents() {
		addStyleName("comment-reply-panel");
		
		this.content = new HTML(reply.getBody());
		this.content.addStyleName("comment-reply-content");
		add(content);
		
		buttonPanel = new FlowPanel();
		buttonPanel.addStyleName("comment-button-panel");

		btEdit = new EditCommentButton();
		buttonPanel.add(btEdit);
		
		btRemove = new RemoveCommentButton();
		buttonPanel.add(btRemove);
		
		add(buttonPanel);

	}
}
