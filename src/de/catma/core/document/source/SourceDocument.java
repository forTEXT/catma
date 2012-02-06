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

package de.catma.core.document.source;

import java.util.ArrayList;
import java.util.List;

import de.catma.core.document.Range;
import de.catma.core.document.source.contenthandler.SourceContentHandler;
import de.catma.core.document.standoffmarkup.structure.StructureMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.user.UserMarkupCollectionReference;

/**
 * The representation of a Source Document.
 *
 * @author Marco Petris
 *
 */
public class SourceDocument {
	
	private String title;
	private SourceContentHandler sourceContentHandler;
	private List<StructureMarkupCollectionReference> structureMarkupCollectionRefs;
	private List<UserMarkupCollectionReference> userMarkupCollectionRefs;
	
	public SourceDocument(String title, SourceContentHandler handler) {
		super();
		this.title = title;
		this.sourceContentHandler = handler;
		this.structureMarkupCollectionRefs = new ArrayList<StructureMarkupCollectionReference>();
		this.userMarkupCollectionRefs = new ArrayList<UserMarkupCollectionReference>();
	}

	/**
	 * Displays the title of the document.
	 */
	@Override
	public String toString() {
		return title;
	}

	/**
	 * @param range the range of the content
	 * @return the content between the startpoint and the endpoint of the range
	 */
	public String getContent( Range range ) {
		int length = getContent().length();
		return getContent().substring(
				Math.min(range.getStartPoint(), length), 
				Math.min(range.getEndPoint(), length));
	}
	
	public String getContent() {
		return sourceContentHandler.getContent();
	}

	public void addStructureMarkupCollectionReference(
			StructureMarkupCollectionReference structureMarkupCollRef) {
		structureMarkupCollectionRefs.add(structureMarkupCollRef);
	}

	public void addUserMarkupCollectionReference(
			UserMarkupCollectionReference userMarkupCollRef) {
		userMarkupCollectionRefs.add(userMarkupCollRef);
	}

	public String getID() {
		return sourceContentHandler.getSourceDocumentInfo().getTechInfoSet().getURI().toString();
	}
	
	public List<StructureMarkupCollectionReference> getStructureMarkupCollectionRefs() {
		return structureMarkupCollectionRefs;
	}
	
	public List<UserMarkupCollectionReference> getUserMarkupCollectionRefs() {
		return userMarkupCollectionRefs;
	}
	
	public SourceContentHandler getSourceContentHandler() {
		return sourceContentHandler;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
}
