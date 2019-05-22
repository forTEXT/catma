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
package de.catma.document.source.contenthandler;

import java.io.IOException;

import de.catma.document.source.SourceDocumentInfo;

/**
 * Basic implementation that provides lazy loading.
 * 
 * @author marco.petris@web.de
 *
 */
public abstract class AbstractSourceContentHandler implements SourceContentHandler {

    private SourceDocumentInfo sourceDocumentInfo;
    private String content;
    
    /* (non-Javadoc)
     * @see de.catma.document.source.contenthandler.SourceContentHandler#setSourceDocumentInfo(de.catma.document.source.SourceDocumentInfo)
     */
    public void setSourceDocumentInfo(SourceDocumentInfo sourceDocumentInfo) {
		this.sourceDocumentInfo = sourceDocumentInfo;
	}
    
    /* (non-Javadoc)
     * @see de.catma.document.source.contenthandler.SourceContentHandler#getSourceDocumentInfo()
     */
    public SourceDocumentInfo getSourceDocumentInfo() {
		return sourceDocumentInfo;
	}

	/**
	 * Does lazy loading, first call will lead to a {@link #load()}.
	 * 
	 * @see de.catma.document.source.contenthandler.SourceContentHandler#getContent()
	 */
	public String getContent() throws IOException {
		if (content == null) {
			load();
		}
		return content;
	}

	/**
	 * @param content the content of the {@link de.catma.document.source.SourceDocument}. To be used
	 * by concrete implementations.
	 */
	protected void setContent(String content) {
		this.content = content;
	}
	
	/* (non-Javadoc)
	 * @see de.catma.document.source.contenthandler.SourceContentHandler#unload()
	 */
	public void unload() {
		content = null;
	}

    /* (non-Javadoc)
     * @see de.catma.document.source.contenthandler.SourceContentHandler#isLoaded()
     */
    public boolean isLoaded() {
    	return (content != null);
    }
    
    @Override
    public boolean hasIntrinsicMarkupCollection() {
    	return false;
    }
}
