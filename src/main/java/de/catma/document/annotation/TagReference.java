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
package de.catma.document.annotation;

import de.catma.document.Range;
import de.catma.tag.TagInstance;

import java.util.Comparator;

/**
 * A TagReference represents a {@link Range} of text referenced by a {@link TagInstance}.
 * It targets a particular source document and belongs to a particular annotation collection.
 */
public class TagReference {
	/**
	 * Compares TagReferences by their {@link Range}s.
	 */
	public static class RangeComparator implements Comparator<TagReference> {
		@Override
		public int compare(TagReference o1, TagReference o2) {
			return o1.getRange().compareTo(o2.getRange());
		}
	}

	private final String annotationCollectionId;
	private final TagInstance tagInstance;
	private String sourceDocumentId;
	private final Range range;

	public TagReference(String annotationCollectionId, TagInstance tagInstance, String sourceDocumentId, Range range) {
		this.annotationCollectionId = annotationCollectionId;
		this.tagInstance = tagInstance;

		// the 'catma' protocol prefix is deprecated and no longer supported (sourceDocumentId used to be called 'target' and was of type URI)
		if (sourceDocumentId.startsWith("catma://")) {
			sourceDocumentId = sourceDocumentId.substring(8);
		}
		this.sourceDocumentId = sourceDocumentId;

		this.range = range;
	}

	@Override
	public String toString() {
		return tagInstance + "@" + sourceDocumentId + "#" + range;
	}

	public String getAnnotationCollectionId() {
		return this.annotationCollectionId;
	}

	public TagInstance getTagInstance() {
		return tagInstance;
	}

	public String getTagInstanceId() {
		return tagInstance.getUuid();
	}

	public String getTagDefinitionId() {
		return tagInstance.getTagDefinitionId();
	}

	public String getSourceDocumentId() {
		return sourceDocumentId;
	}

	public void setSourceDocumentId(String sourceDocumentId) {
		this.sourceDocumentId = sourceDocumentId;
	}

	public Range getRange() {
		return range;
	}
}
