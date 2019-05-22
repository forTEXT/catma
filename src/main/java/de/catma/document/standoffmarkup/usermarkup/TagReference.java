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
package de.catma.document.standoffmarkup.usermarkup;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;

import de.catma.document.Range;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.PropertyDefinition.SystemPropertyName;

/**
 * A {@link Range} of text referenced by a {@link TagInstance}.
 * 
 * @author marco.petris@web.de
 *
 */
public class TagReference {

	/**
	 * Compares TagReferences by their {@link Range}s.
	 *
	 */
	public static class RangeComparator implements Comparator<TagReference> {
		/**
		 * uses {@link Range}s for comparison
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(TagReference o1, TagReference o2) {
			return o1.getRange().compareTo(o2.getRange());
		}
	}
	
	private TagInstance tagInstance;
	private Range range;
	private URI target;
	private String userMarkupCollectionUuid;

	/**
	 * @param tagInstance the referencing instance
	 * @param uri a reference to the source document
	 * @param range the referenced range of text
	 * @param userMarkupCollectionUuid the ID of the containing collection
	 * @throws URISyntaxException
	 */
	public TagReference(TagInstance tagInstance, String uri, Range range, String userMarkupCollectionUuid)
			throws URISyntaxException {
		this.tagInstance = tagInstance;
		this.target = new URI(uri);
		this.range = range;
		this.userMarkupCollectionUuid = userMarkupCollectionUuid;
	}
	
	@Override
	public String toString() {
		return tagInstance + "@" + target + "#" + range;
	}

	/**
	 * @return definition of the {@link TagInstance}'s tag 
	 */
	public String getTagDefinitionId() {
		return tagInstance.getTagDefinitionId();
	}
	
	/**
	 * @return uuid of the {@link TagInstance}.
	 */
	public String getTagInstanceID() {
		return tagInstance.getUuid();
	}
	
	/**
	 * @return referenced text
	 */
	public Range getRange() {
		return range;
	}

	/**
	 * @return the referencing instance
	 */
	public TagInstance getTagInstance() {
		return tagInstance;
	}
	
	/**
	 * @return link to source document
	 */
	public URI getTarget() {
		return target;
	}

	public String getUserMarkupCollectionUuid() {
		return this.userMarkupCollectionUuid;
	}

	public void setUserMarkupCollectionUuid(String userMarkupCollectionUuid) {
		this.userMarkupCollectionUuid = userMarkupCollectionUuid;
	}
}
