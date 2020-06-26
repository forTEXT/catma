package de.catma.ui.client.ui.tagger.comment;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;

import de.catma.ui.client.ui.tagger.comment.CommentLinePanel.CommentLinePanelListener;
import de.catma.ui.client.ui.tagger.editor.Line;
import de.catma.ui.client.ui.tagger.shared.ClientComment;

public class CommentPanel extends FlowPanel {
	
	public static interface CommentPanelListener {
		public void addComment(int x, int y);
		public void edit(ClientComment comment, int x, int y);
		public void remove(ClientComment comment);
	}
	
	private static final int PANEL_OFFSET = 33; 
	
	private AddCommentButton btAddComment;
	private ArrayList<CommentLinePanel> panels = new ArrayList<CommentLinePanel>();

	private CommentPanelListener commentPanelListener;
	
	public CommentPanel(CommentPanelListener commentPanelListener) {
		this.commentPanelListener = commentPanelListener;
		initComponents();
		initActions();
	}

	private void initActions() {
		this.btAddComment.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				commentPanelListener.addComment(event.getClientX(), event.getClientY());
			}
		});
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

	public void addComment(ClientComment comment, Line line) {
		CommentLinePanel panel = panels.get(line.getLineId());
		panel.addComment(comment);
		alignCommentLinePanels(panel);
	}

	private void alignCommentLinePanels(CommentLinePanel panel) {
		panel.getElement().getStyle().setTop(panel.getLine().getLineElement().getOffsetTop()-PANEL_OFFSET, Unit.PX);
		int focusIdx = panels.indexOf(panel);
		int currentMaxTop = panel.getLine().getLineElement().getOffsetTop()-PANEL_OFFSET;
		int currentHeightDown = panel.getElement().getOffsetHeight();
		
		if (focusIdx != 0) {
			for (int idx=focusIdx-1; idx>=0; idx--) {
				CommentLinePanel curPanel = panels.get(idx);
				if (curPanel.getWidgetCount() > 0) {
					int currentTop = curPanel.getLine().getLineElement().getOffsetTop()-PANEL_OFFSET;
					if (currentTop > (currentMaxTop-curPanel.getElement().getOffsetHeight())) {
						currentTop = currentMaxTop-curPanel.getElement().getOffsetHeight();
					}
	
					curPanel.getElement().getStyle().setTop(currentTop, Unit.PX);
					
					currentMaxTop = currentTop;
				}
			}
		}
		
		if (focusIdx != panels.size()-1) {
			for (int idx=focusIdx+1; idx<panels.size(); idx++) {
				CommentLinePanel curPanel = panels.get(idx);
				if (curPanel.getWidgetCount() > 0) {
					int currentTop = curPanel.getLine().getLineElement().getOffsetTop()-PANEL_OFFSET;
					if (currentTop < (currentMaxTop+currentHeightDown)) {
						currentTop = currentMaxTop+currentHeightDown;
					}
	
					curPanel.getElement().getStyle().setTop(currentTop, Unit.PX);
					
					currentMaxTop = currentTop;
					currentHeightDown = curPanel.getElement().getOffsetHeight(); 
				}
				
				currentHeightDown = 0;
				
			}
		}
	}

	public void setLines(List<Line> lines) {
		panels = new ArrayList<CommentLinePanel>(lines.size());
		for (Line line : lines) {
			CommentLinePanel panel = new CommentLinePanel(line, new CommentLinePanelListener() {
				@Override
				public void commentBubbleSelected(Line line) {
					CommentLinePanel selectedPanel = panels.get(line.getLineId());
					for (CommentLinePanel panel : panels) {
						if (!panel.equals(selectedPanel)) {
							panel.deselect();
						}
					}
					
					alignCommentLinePanels(selectedPanel);
				}
				
				@Override
				public void edit(ClientComment comment, int x, int y) {
					commentPanelListener.edit(comment, x, y);
				}
				
				@Override
				public void remove(ClientComment comment) {
					commentPanelListener.remove(comment);
				}
			}); 
			panels.add(panel);
			add(panel);
			
			panel.getElement().getStyle().setTop(line.getLineElement().getOffsetTop()-PANEL_OFFSET, Unit.PX);

		}
		
	}

}
