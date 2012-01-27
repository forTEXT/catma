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
package de.catma.ui.client.ui.tagger;

import java.util.Date;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;

/**
 * @author marco.petris@web.de
 *
 */
public class TaggedSpanFactory {

	private String instanceID;
	private int instanceReferenceCounter = 1;
	private String color;
	
	public TaggedSpanFactory(String color) {
		this(String.valueOf(new Date().getTime()), color);
	}
	
	public TaggedSpanFactory(String instanceID, String color) {
		super();
		this.instanceID = instanceID;
		this.color = color;
	}

	public Element createTaggedSpan(String innerHtml) {
		Element taggedSpan = DOM.createSpan();
		String style = 
				"display:inline-block; border-bottom:5px; border-bottom-color:#" 
				+ color
				+ ";border-bottom-style:solid;";
		
		taggedSpan.setAttribute("style", style);
		taggedSpan.setId(instanceID + "_" + instanceReferenceCounter++);
		taggedSpan.setInnerHTML(innerHtml);
		return taggedSpan;
	}

	public String getInstanceID() {
		return instanceID;
	}

	public String getColor() {
		return color;
	}
	
}
