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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;
import com.vaadin.terminal.gwt.client.VConsole;

import de.catma.ui.client.ui.tagger.DebugUtil;


/**
 * @author marco.petris@web.de
 *
 */
public class SelectionHandlerImplStandard {
	
	public final static class Range {
		private JavaScriptObject javaScriptObject;
		private SelectionHandlerImplStandard impl;

		private Range(SelectionHandlerImplStandard impl, 
				JavaScriptObject javaScriptObject) {
			this.impl = impl;
			this.javaScriptObject = javaScriptObject;
		}
		
		public final Node getStartNode() {
			return impl.getStartNode(javaScriptObject);
		}
		
		public final int getStartOffset() {
			return impl.getStartOffset(javaScriptObject);
		}
		
		public final Node getEndNode() {
			return impl.getEndNode(javaScriptObject);
		}
		
		public final int getEndOffset() {
			return impl.getEndOffset(javaScriptObject);
		}
		
		@Override
		public String toString() {
			if (javaScriptObject == null) {
				return "N/A";
			}
			else {
				return impl.toJSString(javaScriptObject);
			}
		}
		
		public String toDetailedString() {
			if (javaScriptObject == null) {
				return "N/A";
			}
			else {
				return "StartNode["+getStartOffset()+"]"+DebugUtil.getNodeInfo(getStartNode()) + 
						" EndNode["+getEndOffset()+"]" + DebugUtil.getNodeInfo(getEndNode());
			}
			
		}
		
		public final boolean isEmpty() {
			return ((javaScriptObject == null) 
					|| ((getEndNode().equals(getStartNode()) 
							&& (getEndOffset()==getStartOffset()))));
		}
	}

	public final List<Range> getRangeList() {
		List<Range> result = new ArrayList<Range>();
		int rangeCount = getRangeCount();
		for (int i=0; i<rangeCount; i++) {
			JavaScriptObject jsRange = getRangeAt(i);
			VConsole.log("jsRange: " + jsRange.toString());
			if (jsRange != null) { 
				Range range = new Range(this,jsRange);
				VConsole.log(range.toDetailedString());
				result.add(range);
			}
		}
		return result;
	}
	
	protected native JavaScriptObject getRangeAt(int idx) /*-{
		if ($wnd.getSelection().rangeCount > idx) {
			return $wnd.getSelection().getRangeAt(idx);
		}
		else {
			return null;
		}
	}-*/;

	protected native int getRangeCount() /*-{
		return $wnd.getSelection().rangeCount;
	}-*/;
	
	protected native Node getStartNode(JavaScriptObject range) /*-{
		return range.startContainer; 
	}-*/;
	
	protected native int getStartOffset(JavaScriptObject range) /*-{
		return range.startOffset; 
	}-*/;
	
	protected native Node getEndNode(JavaScriptObject range) /*-{
		return range.endContainer; 
	}-*/;
	
	protected native int getEndOffset(JavaScriptObject range) /*-{
		return range.endOffset; 
	}-*/;
	
	protected native String toJSString(JavaScriptObject obj) /*-{
		return obj.toString(); 
	}-*/;
}
