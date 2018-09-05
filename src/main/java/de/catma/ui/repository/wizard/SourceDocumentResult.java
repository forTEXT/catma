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
package de.catma.ui.repository.wizard;

import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;

public class SourceDocumentResult {
	private SourceDocumentInfo sourceDocumentInfo;
	private SourceDocument sourceDocument;
	private String sourceDocumentID = null;
	
	public SourceDocumentResult() {
		super();
		this.sourceDocumentInfo = new SourceDocumentInfo();
	}
	
	public SourceDocumentInfo getSourceDocumentInfo() {
		return sourceDocumentInfo;
	}
	
	public void setSourceDocumentInfo(SourceDocumentInfo sourceDocumentInfo) {
		this.sourceDocumentInfo = sourceDocumentInfo;
	}
	
	public SourceDocument getSourceDocument() {
		return sourceDocument;
	}
	
	public void setSourceDocument(SourceDocument sourceDocument) {
		this.sourceDocument = sourceDocument;
	}
	
	public String getSourceDocumentID() {
		return sourceDocumentID;
	}
	
	public void setSourceDocumentID(String sourceDocumentID) {
		this.sourceDocumentID = sourceDocumentID;
	}
}