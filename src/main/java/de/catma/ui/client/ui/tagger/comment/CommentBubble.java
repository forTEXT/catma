package de.catma.ui.client.ui.tagger.comment;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;

import de.catma.ui.client.ui.tagger.shared.ClientComment;

public class CommentBubble extends FlowPanel {
	
	public static interface CommentBubbleListener {
		public void selected(ClientComment comment);
		public void edit(ClientComment comment, int x, int y);
		public void remove(ClientComment comment);
		public void replyTo(ClientComment comment, int x, int y);
	}
	
	private ClientComment comment;
	private EditCommentButton btEdit;
	private RemoveCommentButton btRemove;
	private ReplyCommentButton btReply;
	private FlowPanel buttonPanel;
	private HTML content;
	private CommentBubbleListener listener;

	public CommentBubble(ClientComment comment, CommentBubbleListener listener) {
		
		this.comment = comment;
		this.listener = listener;
		
		initComponents();
		initActions();
	}

	private void initActions() {
//		addDomHandler(new MouseOverHandler() {
//			
//			@Override
//			public void onMouseOver(MouseOverEvent event) {
//				buttonPanel.setVisible(true);
//				listener.selected(comment);
//			}
//		}, MouseOverEvent.getType());
		
//		addDomHandler(new MouseOutHandler() {
//			
//			@Override
//			public void onMouseOut(MouseOutEvent event) {
//				buttonPanel.setVisible(false);
//				listener.selected();
//			}
//		}, MouseOutEvent.getType());
		
		
		addDomHandler(new MouseUpHandler() {
			
			@Override
			public void onMouseUp(MouseUpEvent event) {
				buttonPanel.setVisible(true);
				content.setHTML(comment.getBody());

				listener.selected(comment);
			}
		}, MouseUpEvent.getType());
		
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

	private void initComponents() {
		addStyleName("comment");
		this.content = new HTML(getShortBody());
		this.content.addStyleName("comment-content");
		add(content);
		
		buttonPanel = new FlowPanel();
		buttonPanel.addStyleName("comment-button-panel");
		buttonPanel.setVisible(false);

		btEdit = new EditCommentButton();
		buttonPanel.add(btEdit);
		
		btRemove = new RemoveCommentButton();
		buttonPanel.add(btRemove);

		btReply = new ReplyCommentButton();
		buttonPanel.add(btReply);
		
		add(buttonPanel);
		
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
		
		//TODO: update comments
	}

}
