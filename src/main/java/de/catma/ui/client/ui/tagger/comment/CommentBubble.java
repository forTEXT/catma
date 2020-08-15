package de.catma.ui.client.ui.tagger.comment;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

import de.catma.ui.client.ui.tagger.comment.ReplyPanel.ReplyPanelListener;
import de.catma.ui.client.ui.tagger.shared.ClientComment;
import de.catma.ui.client.ui.tagger.shared.ClientCommentReply;

public class CommentBubble extends FlowPanel {
	
	public static interface CommentBubbleListener {
		public void selected(ClientComment comment);
		public void edit(ClientComment comment, int x, int y);
		public void remove(ClientComment comment);
		public void replyTo(ClientComment comment, int x, int y);
		public void edit(ClientComment comment, ClientCommentReply reply, int x, int y);
		public void remove(ClientComment comment, ClientCommentReply reply);
	}
	
	private ClientComment comment;
	private EditCommentButton btEdit;
	private RemoveCommentButton btRemove;
	private ReplyCommentButton btReply;
	private FlowPanel commentLayoutPanel;
	private FlowPanel buttonPanel;
	private HTML content;
	private CommentBubbleListener listener;
	private FlowPanel replyLayoutPanel;
	private HandlerRegistration mouseUpReg;

	public CommentBubble(ClientComment comment, CommentBubbleListener listener) {
		
		this.comment = comment;
		this.listener = listener;
		
		initComponents();
		initActions();
	}
	
	private void addMouseUpHandler() {
		this.mouseUpReg = content.addDomHandler(new MouseUpHandler() {
			
			@Override
			public void onMouseUp(MouseUpEvent event) {
				buttonPanel.setVisible(true);
				content.setHTML(comment.getBody());
				addReplies();
				listener.selected(comment);
				mouseUpReg.removeHandler();
			}
		}, MouseUpEvent.getType());
	}
	
	private void initActions() {
		addMouseUpHandler();
		
		btEdit.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				listener.edit(comment, event.getClientX(), event.getClientY());
			}
		});
		
		btRemove.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				listener.remove(comment);
			}
		});
		
		btReply.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				listener.replyTo(comment, event.getClientX(), event.getClientY());
			}
		});
	}

	private void addReplies() {
		replyLayoutPanel.clear();
		for (ClientCommentReply reply : comment.getReplies()) {
			replyLayoutPanel.add(new ReplyPanel(reply, new ReplyPanelListener() {
				
				@Override
				public void remove(ClientCommentReply reply) {
					listener.remove(comment, reply);
				}
				
				@Override
				public void edit(ClientCommentReply reply, int x, int y) {
					listener.edit(comment, reply, x, y);
				}
			}));
		}
	}

	private void initComponents() {
		addStyleName("comment-bubble-container");
		
		Label usernameLabel = new Label(comment.getUsername() + ":");
		add(usernameLabel);
		
		commentLayoutPanel = new FlowPanel();
		commentLayoutPanel.addStyleName("comment");
		
		this.content = new HTML(getShortBody());
		this.content.addStyleName("comment-content");
		commentLayoutPanel.add(content);
		
		buttonPanel = new FlowPanel();
		buttonPanel.addStyleName("comment-button-panel");
		buttonPanel.setVisible(false);

		btEdit = new EditCommentButton();
		buttonPanel.add(btEdit);
		
		btRemove = new RemoveCommentButton();
		buttonPanel.add(btRemove);

		btReply = new ReplyCommentButton();
		buttonPanel.add(btReply);
		
		commentLayoutPanel.add(buttonPanel);
		
		add(commentLayoutPanel);
		
		replyLayoutPanel = new FlowPanel();
		replyLayoutPanel.addStyleName("reply-container");
		
		add(replyLayoutPanel);
	}
	
	private String getShortBody() {
		boolean exceededLength = comment.getBody().length() > 80;
		if (exceededLength) {
			return comment.getBody().substring(0, 80) + "...";
		}
		else {
			return comment.getBody();
		}
	}

	public void deselect() {
		buttonPanel.setVisible(false);
		this.content.setHTML(getShortBody());
		this.replyLayoutPanel.clear();
		addMouseUpHandler();
	}

	public ClientComment getComment() {
		return comment;
	}

	public void refresh() {
		if (buttonPanel.isVisible()) {
			this.content.setHTML(comment.getBody());
			
			
		}
		else {
			this.content.setHTML(getShortBody());
		}
		
		addReplies();
	}

}
