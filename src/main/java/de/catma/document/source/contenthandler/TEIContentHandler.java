/*
 * CATMA Computer Aided Text Markup and Analysis
 *
 *    Copyright (C) 2008-2010  University Of Hamburg
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.catma.document.source.contenthandler;

import java.util.Scanner;

import nu.xom.Element;


/**
 * A content handler HTML based {@link de.catma.document.source.SourceDocument}s.
 *
 * @author marco.petris@web.de
 *
 */
public class TEIContentHandler extends XMLContentHandler {
	
	public TEIContentHandler() {
		inlineElements.add("abbr");
		inlineElements.add("add");
		inlineElements.add("expan");
		inlineElements.add("corr");
		inlineElements.add("date");
		inlineElements.add("del");
		inlineElements.add("distinct");
		inlineElements.add("emph");
		inlineElements.add("foreign");
		inlineElements.add("gap");
		inlineElements.add("gloss");
		inlineElements.add("hi");
		inlineElements.add("index");
		inlineElements.add("measure");
		inlineElements.add("mentioned");
		inlineElements.add("milestone");
		inlineElements.add("name");
		inlineElements.add("num");
		inlineElements.add("orig");
		inlineElements.add("q");
		inlineElements.add("quote");
		inlineElements.add("ref");
		inlineElements.add("reg");
		inlineElements.add("rs");
		inlineElements.add("said");
		inlineElements.add("sic");
		inlineElements.add("soCalled");
		inlineElements.add("sp");
		inlineElements.add("street");
		inlineElements.add("term");
		inlineElements.add("time");
		inlineElements.add("unclear");
	}
	
	@Override
	public void addTextContent(StringBuilder contentBuilder, Element element,
			String content) {

		boolean inline = inlineElements.contains(element.getLocalName());
		// make things look good...
		
    	if (!content.trim().isEmpty()) {
    		if (inline) {
    			contentBuilder.append(" ");
    		}
    		try (Scanner lineScanner = new Scanner(content.trim())) { 
    			String conc = "";
	    		while (lineScanner.hasNextLine()) {
	    			contentBuilder.append(conc);
	    			contentBuilder.append(lineScanner.nextLine().trim());
	    			conc = " ";
	    		}
    		}
    		if (inline) {
    			contentBuilder.append(" ");
    		}
    	}
	}
		
	@Override
	public void addEmptyElement(StringBuilder contentBuilder, Element element) {
		// show linebreaks as actual linebreaks
		if (element.getLocalName().equals("lb")) {
			contentBuilder.append("\n");
		}
		else {
			super.addEmptyElement(contentBuilder, element);
		}
	}
	
}
