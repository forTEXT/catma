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

import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;

import java.io.IOException;

/**
 * A content handler for a {@link SourceDocument}. It is responsible for loading content from a source document and keeping it in memory.
 * <p>
 * It uses the proper encoding and {@link FileType}.
 */
public interface SourceContentHandler {
	/**
	 * Sets all the metadata for the source document with which this instance is associated. Must be called immediately after instantiation.
	 *
	 * @param sourceDocumentInfo a {@link SourceDocumentInfo} object containing the metadata
	 */
	void setSourceDocumentInfo(SourceDocumentInfo sourceDocumentInfo);

	/**
	 * Returns all the metadata for the source document with which this instance is associated.
	 *
	 * @return a {@link SourceDocumentInfo} object containing the metadata
	 */
	SourceDocumentInfo getSourceDocumentInfo();

	/**
	 * Loads the content of the source document with which this instance is associated.
	 * <p>
	 * Loading is performed from the resource identified via {@link TechInfoSet#getURI()} of the {@link SourceDocumentInfo}.
	 *
	 * @throws IOException if an error occurs while loading the content
	 */
	void load() throws IOException;

	/**
	 * Returns the content of the source document with which this instance is associated.
	 *
	 * @return the entire document text
	 * @throws IOException if an error occurs while loading the content
	 */
	String getContent() throws IOException;

	/**
	 * Discards the content of the source document with which this instance is associated.
	 */
	void unload();

	/**
	 * Whether the content of the source document with which this instance is associated has been loaded.
	 *
	 * @return <code>true</code> if the content has been loaded, <code>false</code> if the content has not yet been loaded or was unloaded via
	 *         {@link SourceContentHandler#unload()}
	 */
	boolean isLoaded();

	/**
	 * Whether the source document with which this instance is associated contains embedded annotations.
	 * <p>
	 * Can only be true for XML documents.
	 *
	 * @return <code>true</code> if embedded annotations exist, otherwise <code>false</code>
	 */
	@Deprecated
	boolean hasIntrinsicAnnotationCollection();
}
