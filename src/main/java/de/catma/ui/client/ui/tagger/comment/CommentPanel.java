package de.catma.ui.client.ui.tagger.comment;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;

import de.catma.ui.client.ui.tagger.editor.Line;
import de.catma.ui.client.ui.tagger.shared.ClientComment;

public class CommentPanel extends FlowPanel {
	
	private static final int PANEL_OFFSET = 33; 
	
	private AddCommentButton btAddComment;

	public CommentPanel() {
		initComponents();
		initActions();
	}

	private void initActions() {
	}

	private void initComponents() {
		addStyleName("comment-panel");
		this.btAddComment = new AddCommentButton();
		add(btAddComment);
		this.btAddComment.setVisible(false);
	}
	
	public void setAddCommentButtonVisible(boolean visible, Line line) {
		int topOffset = 0;
		
		if (line != null) {
			topOffset = line.getLineElement().getOffsetTop();
		}
		
		this.btAddComment.setVisible(visible);
		this.btAddComment.getElement().getStyle().setTop(topOffset-PANEL_OFFSET, Unit.PX);
	}
	
	public HandlerRegistration addAddCommentClickHandler(ClickHandler clickHandler) {
		return this.btAddComment.addClickHandler(clickHandler);
	}

	public void addComment(ClientComment comment, Line line) {
		Element commentDiv = DOM.createDiv();
		commentDiv.setAttribute("class", "comment");
		this.getElement().appendChild(commentDiv);
		commentDiv.setInnerText(comment.getBody());
		
		commentDiv.getStyle().setTop(line.getLineElement().getOffsetTop()-PANEL_OFFSET, Unit.PX);
	}

}
