/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2012  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */   
package de.catma.ui.client.ui.tagger.menu;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.user.client.Timer;

import de.catma.ui.client.ui.tagger.VTagger;
import de.catma.ui.client.ui.tagger.shared.ContentElementID;


/**
 * @author marco.petris@web.de
 *
 */
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
	private TagMenuPopup lastPopup;
	private VTagger vTagger;
	
	public TagMenu(VTagger vTagger) {
		super();
		this.vTagger = vTagger;
	}

	public void loadMenu() {
		Element line = findClosestLine();
		if (line != null) {
			List<Element> taggedSpans = findTargetSpan(line);
			
			if (vTagger.hasSelection() || !taggedSpans.isEmpty()) {
				hidePopup();
				String lastSelectedColor = null;
				if (lastPopup != null) {
					lastSelectedColor = lastPopup.getLastSelectedColor();
				}
				lastPopup = new TagMenuPopup(vTagger, lastSelectedColor);
				lastPopup.setPopupPosition(lastClientX, lastClientY+5);
				
				for (Element span : taggedSpans) {
					lastPopup.addTag(vTagger.getTagInstanceID(span.getAttribute("id")));
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
