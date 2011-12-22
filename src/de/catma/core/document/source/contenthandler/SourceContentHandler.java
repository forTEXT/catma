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

package de.catma.core.document.source.contenthandler;


import java.io.IOException;

import de.catma.core.document.source.FileType;
import de.catma.core.document.source.SourceDocumentInfo;


/**
 * A content handler for a Source Document. The content handler is responsible for 
 * loading content from a Source Document. It uses the proper encoding and 
 * {@link FileType}. 
 * <br><br>
 * <b> SourceContentHandler need to have a default no arg constructor!</b>
 *
 * @author Marco Petris
 *
 */
public interface SourceContentHandler {

	public void setSourceDocumentInfo(
			SourceDocumentInfo sourceDocumentInfo );
	
	public SourceDocumentInfo getSourceDocumentInfo();
	
	public void load() throws IOException;
	
	/**
	 * Returns the content between the startPoint and the endPoint. A point is a mark
	 * between two characters. Counting starts with 0.
	 * @param startPoint the point before the first character of the content
	 * @param endPoint the point after the last character of the content
	 * @return the content
	 */
	public String getContent( long startPoint, long endPoint );
	
	/**
	 * Returns the content from the character after the startPoint. A point is a mark
	 * between two characters. Counting starts with 0.
	 * @param startPoint the point before teh first character of the content.
	 * @return the content
	 */
	public String getContent( long startPoint );
	
	/**
	 * @return the size of the content
	 */
	public long getSize();
}