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
package de.catma.document.corpus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocumentReference;

/**
 * A corpus is a collection of {@link SourceDocument}s and a (sub-)set of the attached
 * markup.
 * 
 * @author marco.petris@web.de
 *
 */
public class Corpus {

	private List<SourceDocumentReference> sourceDocuments;
	private List<AnnotationCollectionReference> userMarkupCollectionRefs;
	
	public Corpus() {
		this.sourceDocuments = new ArrayList<SourceDocumentReference>();
		this.userMarkupCollectionRefs = new ArrayList<AnnotationCollectionReference>();
	}

	public void addSourceDocument(SourceDocumentReference sourceDocument) {
		sourceDocuments.add(sourceDocument);
	}

	public void addUserMarkupCollectionReference(
			AnnotationCollectionReference userMarkupCollRef) {
		userMarkupCollectionRefs.add(userMarkupCollRef);
	}

	/**
	 * @return non modifiable list
	 */
	public List<SourceDocumentReference> getSourceDocuments() {
		return Collections.unmodifiableList(sourceDocuments);
	}

	/**
	 * @return non modifiable list
	 */
	public List<AnnotationCollectionReference> getUserMarkupCollectionRefs() {
		return Collections.unmodifiableList(userMarkupCollectionRefs);
	}

	/**
	 * @param sd
	 * @return a list of references of the user markup collections of the given
	 * source document that are contained in this corpus (may be a subset of
	 * all user markup collections of that source document).
	 */
	public List<AnnotationCollectionReference> getUserMarkupCollectionRefs(
			SourceDocumentReference sd) {
		List<AnnotationCollectionReference> result = 
				new ArrayList<AnnotationCollectionReference>();
		
		for (AnnotationCollectionReference ref : sd.getUserMarkupCollectionRefs()) {
			if (userMarkupCollectionRefs.contains(ref)) {
				result.add(ref);
			}
		}
		return result;
	}
	
	public boolean isEmpty() {
		return getSourceDocuments().isEmpty();
	}

	public List<String> getDocumentIds() {
		return sourceDocuments.stream()
				.map(SourceDocumentReference::getUuid)
				.collect(Collectors.toList());
	}

	public List<String> getCollectionIds() {
		return userMarkupCollectionRefs.stream()
				.map(AnnotationCollectionReference::getId)
				.collect(Collectors.toList());
	}
}
