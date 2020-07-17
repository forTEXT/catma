package de.catma.ui.client.ui.tagger.comment;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import de.catma.ui.client.ui.tagger.comment.CommentBubble.CommentBubbleListener;
import de.catma.ui.client.ui.tagger.editor.Line;
import de.catma.ui.client.ui.tagger.shared.ClientComment;

public class CommentLinePanel extends FlowPanel {
	public static interface CommentLinePanelListener {
		public void selected(ClientComment comment, Line line);
		public void edit(ClientComment comment, int x, int y);
		public void remove(ClientComment comment);
		public void replyTo(ClientComment comment, int x, int y);
	}
	
	private Line line;
	private CommentLinePanelListener listener;

	public CommentLinePanel(Line line, CommentLinePanelListener listener) {
		super();
		this.line = line;
		this.listener = listener;
		initComponents();
	}

	private void initComponents() {
		addStyleName("comment-line-panel");
		
	}

	public void addComment(ClientComment comment) {

		insert(new CommentBubble(comment, new CommentBubbleListener() {
			@Override
			public void selected(ClientComment comment) {
				
				for (int i=0; i<getWidgetCount(); i++) {
					Widget w = getWidget(i);
					if (w instanceof CommentBubble) {
						CommentBubble commentBubble= (CommentBubble)w;
						if (!commentBubble.getComment().equals(comment)) {
							commentBubble.deselect();
						}
					}
				}
				
				listener.selected(comment, line);
			}
			
			@Override
			public void edit(ClientComment comment, int x, int y) {
				listener.edit(comment, x, y);
			}
			
			@Override
			public void remove(ClientComment comment) {
				listener.remove(comment);
			}
			
			@Override
			public void replyTo(ClientComment comment, int x, int y) {
				listener.replyTo(comment, x, y);
			}
		}), 0);
	}
	
	public Line getLine() {
		return line;
	}

	public void deselect() {
		for (int i=0; i<getWidgetCount(); i++) {
			Widget w = getWidget(i);
			if (w instanceof CommentBubble) {
				CommentBubble commentBubble= (CommentBubble)w;
				commentBubble.deselect();
			}
		}	
	}

	public void refreshComment(String uuid) {
		
		for (int i=0; i<getWidgetCount(); i++) {
			Widget w = getWidget(i);
			if (w instanceof CommentBubble) {
				CommentBubble commentBubble= (CommentBubble)w;
				if (commentBubble.getComment().getUuid().equals(uuid)) {
					commentBubble.refresh();
				}
			}
		}	
		
		
	}

	public void removeComment(String uuid) {
		for (int i=0; i<getWidgetCount(); i++) {
			Widget w = getWidget(i);
			if (w instanceof CommentBubble) {
				CommentBubble commentBubble= (CommentBubble)w;
				if (commentBubble.getComment().getUuid().equals(uuid)) {
					commentBubble.removeFromParent();
					break;
				}
			}
		}			
	}

}
