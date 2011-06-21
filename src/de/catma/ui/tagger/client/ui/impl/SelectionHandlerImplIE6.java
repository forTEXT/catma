package de.catma.ui.tagger.client.ui.impl;

import com.google.gwt.core.client.JavaScriptObject;



public class SelectionHandlerImplIE6 extends SelectionHandlerImplStandard {

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
