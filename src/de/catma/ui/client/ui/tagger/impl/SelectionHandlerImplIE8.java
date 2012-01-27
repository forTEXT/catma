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
package de.catma.ui.client.ui.tagger.impl;

import com.google.gwt.core.client.JavaScriptObject;



/**
 * @author marco.petris@web.de
 *
 */
public class SelectionHandlerImplIE8 extends SelectionHandlerImplStandard {

	//TODO: needs more work... or skip IE8 support at all...
	@Override
	protected native JavaScriptObject getRangeAt(int idx) /*-{
		var origRange = $doc.selection.createRange();
		var rangeResult = new Object();
		var moveLength = $doc.documentElement.innerHTML.length * (-1);
		var rangeCopy = origRange.duplicate();
		var start = 
			Math.abs(rangeCopy.duplicate().moveStart('character', moveLength));

		var rangeWindow = rangeCopy.duplicate();
		rangeCopy.collapse(true);
		var parentElement = rangeWindow.parentElement();
		var children = parentElement.getElementsByTagName('*');
		for ( var i = children.length - 1; i >= 0; i--) {
			rangeWindow.moveToElementText(children[i]);
			if (rangeWindow.inRange(rangeCopy)) {
				parentElement = children[i];
				break;
			}
		}
		rangeWindow.moveToElementText(parentElement);
		rangeResult.startOffset = 
			start - Math.abs(rangeWindow.moveStart('character', moveLength));
		rangeResult.startContainer = rangeCopy.parentElement().firstChild;

		rangeCopy = origRange.duplicate();
		var end = 
			Math.abs(origRange.duplicate().moveEnd('character', moveLength));
		rangeWindow = rangeCopy.duplicate();
		rangeCopy.collapse(false);
		parentElement = rangeWindow.parentElement();
		children = parentElement.getElementsByTagName('*');
		for ( var i = children.length - 1; i >= 0; i--) {
			rangeWindow.moveToElementText(children[i]);
			if (rangeWindow.inRange(rangeCopy)) {
				parentElement = children[i];
				break;
			}
		}
		
		rangeWindow.moveToElementText(parentElement);
		rangeResult.endOffset = 
			end - Math.abs(rangeWindow.moveStart('character', moveLength));
		rangeResult.endContainer = rangeCopy.parentElement().firstChild;
		return rangeResult;
	}-*/;

	
	@Override
	protected int getRangeCount() {
		return 1;
	}

}
