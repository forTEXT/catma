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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

/**
 * A content handler for XML based {@link de.catma.document.source.SourceDocument}s.
 *
 * @author marco.petris@web.de
 *
 */
public class XMLContentHandler extends AbstractSourceContentHandler {
	protected List<String> inlineElements = new ArrayList<String>();

	public XMLContentHandler() {
		inlineElements = new ArrayList<String>();
	}
	
	
	/* (non-Javadoc)
	 * @see de.catma.document.source.contenthandler.SourceContentHandler#load(java.io.InputStream)
	 */
	public void load(InputStream is) throws IOException {
		try {
	        Builder builder = new Builder();
	        
	        Document document = builder.build(is);
	        StringBuilder contentBuilder = new StringBuilder();
	        processTextNodes(
	        		contentBuilder, 
	        		document.getRootElement());
	        setContent(contentBuilder.toString());	
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
    
    /* (non-Javadoc)
     * @see de.catma.document.source.contenthandler.SourceContentHandler#load()
     */
    public void load() throws IOException {
    	
        try {
        	InputStream is = getSourceDocumentInfo().getTechInfoSet().getURI().toURL().openStream();
        	try {
        		load(is);
        	}
        	finally {
        		is.close();
        	}
        }
        catch (Exception e) {
        	throw new IOException(e);
        }
        

    }

    /**
     * Appends text elements to the given builder otherwise descents deeper into the
     * document tree.
     * @param contentBuilder the builder is filled with text elements
     * @param element the current element to process
     * @throws URISyntaxException 
     */
    protected void processTextNodes(
    		StringBuilder contentBuilder, Element element) throws URISyntaxException {
    	
		for( int idx=0; idx<element.getChildCount(); idx++) {
            Node curChild = element.getChild(idx);
            if (curChild instanceof Text) {
            	addTextContent(contentBuilder, element, curChild.getValue());
            }
            else if (curChild instanceof Element) { //descent
                processTextNodes(
                	contentBuilder, 
                	(Element)curChild);
            
            }
        }
		
		if (element.getChildCount() == 0) { //empty elements
			addEmptyElement(contentBuilder, element);
		}
		else {
			addBreak(contentBuilder, element);
		}
    }
    
    @Deprecated
    public void addEmptyElement(StringBuilder contentBuilder, Element element) {
		contentBuilder.append(" ");
	}


	public void addTextContent(StringBuilder contentBuilder, Element element,
			String content) {
    	if (!content.trim().isEmpty()) {
    		contentBuilder.append(content);
    	}
	}
	
	public void addBreak(StringBuilder contentBuilder, Element element) {
		if (!inlineElements.contains(element.getLocalName())) {
			contentBuilder.append("\n");
		}
	}


    @Override
    public boolean hasIntrinsicMarkupCollection() {
    	return true;
    }
}
