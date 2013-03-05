/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
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
package de.catma.ui.client.ui.tagger.editor;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;

public class HighlightedSpanFactory extends SpanFactory {
	
	private String highlightColor;
	
	public HighlightedSpanFactory(String highlightColor) {
		this.highlightColor = highlightColor;
	}

	@Override
	public Element createTaggedSpan(String innerHtml) {
		Element highlightedSpan = DOM.createSpan();
		String style = 
				"display:inline-block; color:#F0F0F0; background:"
						+highlightColor+";";
		
		highlightedSpan.setAttribute("style", style);
		highlightedSpan.setId(getInstanceID() + "_" + instanceReferenceCounter++);
		highlightedSpan.setInnerHTML(innerHtml);
		return highlightedSpan;
	}

}
