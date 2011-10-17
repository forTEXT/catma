package de.catma.ui.tagger.client.ui.menu;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

import de.catma.ui.tagger.client.ui.shared.ContentElementID;


public class TagMenu implements MouseMoveHandler {
	
	private final class MenuTimer extends Timer {

		@Override
		public void run() {
			loadMenu();
		}
		
		
	}
	
	private final static class TagMenuPopup extends DialogBox {
		
		private TreeItem root;
		
		public TagMenuPopup() {
			super(true);
			root = new TreeItem("Tags");
			Tree tree = new Tree();
			tree.addItem(root);
			setGlassEnabled(true);
			setAnimationEnabled(true);
			setWidget(new Button("hello"));
			setText("Test");
			
		}
		
		public void addTag(String tag) {
			root.addItem(tag);
		}
		
		
		
	}
	
	private int lastClientX;
	private int lastClientY;
	private MenuTimer curMenuTimer;
	private MenuItemListener menuItemListener;
	
	public TagMenu(MenuItemListener menuItemListener) {
		super();
		this.menuItemListener = menuItemListener;
	}

	public void loadMenu() {
		Element line = findClosestLine();
		if (line != null) {
			List<Element> taggedSpans = findTargetSpan(line);
			
			if (!taggedSpans.isEmpty()) {
				
				final TagMenuPopup tagMenuPopup = new TagMenuPopup();
				tagMenuPopup.setPopupPosition(lastClientX, lastClientY);
				
				for (Element span : taggedSpans) {
					tagMenuPopup.addTag(span.getAttribute("class"));
				}
				
				
				tagMenuPopup.show();
				
				
				//menuItemListener.menuItemSelected(builder.toString());
			}
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
