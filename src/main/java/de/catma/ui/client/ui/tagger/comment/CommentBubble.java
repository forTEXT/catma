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
	}
	
	private ClientComment comment;
	private EditCommentButton btEdit;
	private RemoveCommentButton btRemove;
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
				listener.selected(comment);
			}
		}, MouseUpEvent.getType());
		
		btEdit.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				GWT.log("edit");
			}
		});
		
		btRemove.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				GWT.log("remove");
			}
		});
	}

	private void initComponents() {
		addStyleName("comment");
		this.content = new HTML(comment.getBody());
		this.content.addStyleName("comment-content");
		add(content);
		
		buttonPanel = new FlowPanel();
		buttonPanel.addStyleName("comment-button-panel");
		buttonPanel.setVisible(false);

		btEdit = new EditCommentButton();
		buttonPanel.add(btEdit);
		
		btRemove = new RemoveCommentButton();
		buttonPanel.add(btRemove);

		add(buttonPanel);
		
	}

	public void deselect() {
		buttonPanel.setVisible(false);
	}

	public ClientComment getComment() {
		return comment;
	}

}
