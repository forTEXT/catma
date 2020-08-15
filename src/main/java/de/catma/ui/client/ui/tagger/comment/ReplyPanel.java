package de.catma.ui.client.ui.tagger.comment;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

import de.catma.ui.client.ui.tagger.shared.ClientCommentReply;

public class ReplyPanel extends FlowPanel {
	
	public static interface ReplyPanelListener {
		public void edit(ClientCommentReply reply, int x, int y);
		public void remove(ClientCommentReply reply);
	}

	private ClientCommentReply reply;
	private HTML content;
	private FlowPanel buttonPanel;
	private EditCommentButton btEdit;
	private RemoveCommentButton btRemove;

	public ReplyPanel(ClientCommentReply reply, ReplyPanelListener listener) {
		this.reply = reply;
		initComponents();
		initActions(listener);
	}

	private void initActions(ReplyPanelListener listener) {
		btEdit.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				listener.edit(reply, event.getClientX(), event.getClientY());
			}
		});
		
		btRemove.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				listener.remove(reply);
			}
		});
	}

	private void initComponents() {
		addStyleName("comment-reply-panel");
		
		Label usernameLabel = new Label(reply.getUsername() + ":");
		add(usernameLabel);

		FlowPanel commentReplyMainSection = new FlowPanel();
		commentReplyMainSection.addStyleName("comment-reply-main-section");
		
		add(commentReplyMainSection);
		
		this.content = new HTML(reply.getBody());
		this.content.addStyleName("comment-reply-content");
		commentReplyMainSection.add(content);
		
		buttonPanel = new FlowPanel();
		buttonPanel.addStyleName("comment-button-panel");

		btEdit = new EditCommentButton();
		buttonPanel.add(btEdit);
		
		btRemove = new RemoveCommentButton();
		buttonPanel.add(btRemove);
		
		commentReplyMainSection.add(buttonPanel);

	}
}
