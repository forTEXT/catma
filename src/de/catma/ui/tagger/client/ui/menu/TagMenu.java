package de.catma.ui.tagger.client.ui.menu;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

import de.catma.ui.tagger.client.ui.shared.ContentElementID;


public class TagMenu implements MouseMoveHandler {
	
	private final class MenuTimer extends Timer {

		@Override
		public void run() {
			if ((lastPopup == null) 
					|| !( (lastClientX >= lastPopup.getAbsoluteLeft()-2) 
							&& (lastClientX <= lastPopup.getAbsoluteLeft()+lastPopup.getOffsetWidth())
						&& (lastClientY >= lastPopup.getAbsoluteTop()-2) 
							&& (lastClientY <= lastPopup.getAbsoluteTop()+lastPopup.getOffsetHeight()))) {
				loadMenu();
			}
		}
		
		
	}
	
	private final static class TagMenuPopup extends PopupPanel {
		
		private TreeItem root;
		
		public TagMenuPopup() {
			super(true);
			root = new TreeItem("Tags") {
				public void setSelected(boolean selected) {
					super.setSelected(selected);
				    setStyleName(DOM.getFirstChild(getElement()), "gwt-TreeItem-selected", false);
				}
			};
			Tree tree = new Tree();
			tree.addItem(root);
			root.setState(true);
			root.setStyleName("tagger_menu_root");
			VerticalPanel vPanel = new VerticalPanel();
			vPanel.add(tree);
			vPanel.setStylePrimaryName("tagger_menu");
			setWidget(vPanel);
		}
		
		public void addTag(String tag) {
			HorizontalPanel hPanel = new HorizontalPanel();
			hPanel.setSpacing(5);
			Label l = new Label(tag);
			l.setStyleName("menu_item_text");
			hPanel.add(l);
			Button tagRemoveButton = new Button("remove");
			hPanel.add(tagRemoveButton);
			root.addItem(hPanel);
			root.setState(true);
		}
		
		
		
	}
	
	private int lastClientX;
	private int lastClientY;
	private MenuTimer curMenuTimer;
	private MenuItemListener menuItemListener;
	private TagMenuPopup lastPopup;
	
	public TagMenu(MenuItemListener menuItemListener) {
		super();
		this.menuItemListener = menuItemListener;
	}

	public void loadMenu() {
		Element line = findClosestLine();
		if (line != null) {
			List<Element> taggedSpans = findTargetSpan(line);
			
			if (!taggedSpans.isEmpty()) {
				
				hidePopup();
				
				lastPopup = new TagMenuPopup();
				lastPopup.setPopupPosition(lastClientX, lastClientY+5);
				
				for (Element span : taggedSpans) {
					lastPopup.addTag(span.getAttribute("class"));
				}
				
				lastPopup.show();
				
				
				//menuItemListener.menuItemSelected(builder.toString());
			}
			else {
				hidePopup();
			}
		}
	}

	private void hidePopup() {
		if ((lastPopup != null) && (lastPopup.isVisible())) {
			lastPopup.hide();
		}
		
	}
	
	private List<Element> findTargetSpan(Element line) {
		ArrayList<Element> result = new ArrayList<Element>();
		
		
		if (line.getFirstChildElement() != null) {

			Element curSpan = findClosestSibling(line.getFirstChildElement());
			if (curSpan != null) {
				result.add(curSpan);
			}
			while (curSpan!= null && (curSpan.getFirstChildElement()!=null)) {
				curSpan = findClosestSibling(curSpan.getFirstChildElement());
				if (curSpan != null) {
					result.add(curSpan);
				}
			}

		}
		
		return result;
	}

	private Element findClosestLine() {
		return findClosestSibling(
				Document.get().getElementById(ContentElementID.LINE.name() + "0"));
	}
	
	private Element findClosestSibling(Element start) {
		Element curSibling = start;

		while((curSibling != null) && 
				!( (lastClientX > curSibling.getAbsoluteLeft()) 
						&& (lastClientX < curSibling.getAbsoluteRight())
					&& (lastClientY > curSibling.getAbsoluteTop()) 
						&& (lastClientY < curSibling.getAbsoluteBottom()))) {
			
			curSibling = curSibling.getNextSiblingElement();
		}
		
		return curSibling;
	}



	public void onMouseMove(MouseMoveEvent event) {
		lastClientX = event.getClientX();
		lastClientY = event.getClientY();
		if (curMenuTimer != null) {
			curMenuTimer.cancel();
		}
		
		curMenuTimer = new MenuTimer();
		curMenuTimer.schedule(400);
	}

}
