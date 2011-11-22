package de.catma.ui.client.ui.tagger.menu;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.user.client.Timer;

import de.catma.ui.client.ui.tagger.shared.ContentElementID;


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
	
	private int lastClientX;
	private int lastClientY;
	private MenuTimer curMenuTimer;
	private TagActionListener tagActionListener;
	private TagMenuPopup lastPopup;
	
	public TagMenu(TagActionListener tagActionListener) {
		super();
		this.tagActionListener = tagActionListener;
	}

	public void loadMenu() {
		Element line = findClosestLine();
		if (line != null) {
			List<Element> taggedSpans = findTargetSpan(line);
			
			if (!taggedSpans.isEmpty()) {
				
				hidePopup();
				
				lastPopup = new TagMenuPopup(tagActionListener);
				lastPopup.setPopupPosition(lastClientX, lastClientY+5);
				
				for (Element span : taggedSpans) {
					lastPopup.addTag(span.getAttribute("class"));
				}
				
				lastPopup.show();
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
