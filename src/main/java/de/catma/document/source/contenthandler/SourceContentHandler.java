/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009  University Of Hamburg
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
import java.io.InputStream;

import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;

/**
 * A content handler for a source document. The content handler is responsible for
 * loading content from a source document and keeping it in memory.
 * <p>
 * It uses the proper encoding and {@link FileType}.
 * <p>
 * <strong>SourceContentHandler implementations need to have a default no-arg constructor!</strong>
 */
public interface SourceContentHandler {

	/**
	 * @param sourceDocumentInfo all the metadata for the {@link SourceDocument}, has
	 * to be set right after instance creation completed
	 */
	public void setSourceDocumentInfo(
			SourceDocumentInfo sourceDocumentInfo);
	
	/**
	 * @return all the metadata for the {@link SourceDocument}
	 */
	public SourceDocumentInfo getSourceDocumentInfo();
	
	/**
	 * Loading via {@link de.catma.document.source.TechInfoSet#getURI()} of the {@link SourceDocumentInfo}.
	 * @throws IOException error accessing {@link de.catma.document.source.TechInfoSet#getURI()}.
	 */
	public void load() throws IOException;
	
	/**
	 * @return the extracted text of the {@link SourceDocument}.
	 * @throws IOException
	 */
	public String getContent() throws IOException;
	
	/**
	 * Discards the content.
	 */
	public void unload();
	
	/**
	 * @return <code>true</code> if the content has been loaded and not yet unloaded, else <code>false</code>.
	 */
	public boolean isLoaded();
	
	public boolean hasIntrinsicMarkupCollection();
	
}
