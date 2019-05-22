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
package de.catma.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

/**
 * A corpus is a collection of {@link SourceDocument}s and a (sub-)set of the attached
 * markup.
 * 
 * @author marco.petris@web.de
 *
 */
public class Corpus {

	private String id;
	private String name;

	private List<SourceDocument> sourceDocuments;
	private List<UserMarkupCollectionReference> userMarkupCollectionRefs;
	
	/**
	 * @param id identifier (depends on the repository)
	 * @param corpusName
	 */
	public Corpus(String id, String corpusName) {
		this.id = id;
		this.name = corpusName;
		this.sourceDocuments = new ArrayList<SourceDocument>();
		this.userMarkupCollectionRefs = new ArrayList<UserMarkupCollectionReference>();
	}

	/**
	 * Corpus with a <code>null</code>-identifier for non persistent corpora.
	 * @param corpusName
	 */
	public Corpus(String corpusName) {
		this(null, corpusName);
	}

	public void addSourceDocument(SourceDocument sourceDocument) {
		sourceDocuments.add(sourceDocument);
	}

	public void addUserMarkupCollectionReference(
			UserMarkupCollectionReference userMarkupCollRef) {
		userMarkupCollectionRefs.add(userMarkupCollRef);
	}
	
	@Override
	public String toString() {
		return name;
	}

	/**
	 * @return non modifiable list
	 */
	public List<SourceDocument> getSourceDocuments() {
		return Collections.unmodifiableList(sourceDocuments);
	}

	/**
	 * @return non modifiable list
	 */
	public List<UserMarkupCollectionReference> getUserMarkupCollectionRefs() {
		return Collections.unmodifiableList(userMarkupCollectionRefs);
	}
	
	/**
	 * @return the identifier or <code>null</code> if the corpus is not persistent
	 */
	public String getId() {
		return id;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param sd
	 * @return a list of references of the user markup collections of the given
	 * source document that are contained in this corpus (may be a subset of
	 * all user markup collections of that source document).
	 */
	public List<UserMarkupCollectionReference> getUserMarkupCollectionRefs(
			SourceDocument sd) {
		List<UserMarkupCollectionReference> result = 
				new ArrayList<UserMarkupCollectionReference>();
		
		for (UserMarkupCollectionReference ref : sd.getUserMarkupCollectionRefs()) {
			if (userMarkupCollectionRefs.contains(ref)) {
				result.add(ref);
			}
		}
		return result;
	}
}
