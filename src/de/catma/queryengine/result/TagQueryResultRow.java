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
package de.catma.queryengine.result;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.catma.document.Range;

public class TagQueryResultRow extends QueryResultRow {
	
	private String markupCollectionId;
	private String tagDefinitionId;
	private String tagInstanceId;
	private SortedSet<Range> ranges;
	
	public TagQueryResultRow(String sourceDocumentId, List<Range> ranges,
			String markupCollectionId, String tagDefinitionId,
			String tagInstanceId) {
		super(sourceDocumentId, Range.getEnclosingRange(ranges));
		this.markupCollectionId = markupCollectionId;
		this.tagDefinitionId = tagDefinitionId;
		this.tagInstanceId = tagInstanceId;
		this.ranges = new TreeSet<Range>();
		this.ranges.addAll(ranges);
	}
	
	public String getTagInstanceId() {
		return tagInstanceId;
	}
	
	@Override
	public String toString() {
		return super.toString()
				+ ((markupCollectionId == null)?"":("MarkupColl[#"+markupCollectionId+"]"))
				+ ((tagDefinitionId == null)?"":("TagDef[#"+tagDefinitionId+"]")) 
				+ ((tagInstanceId == null)?"":("TagInstance[#"+tagInstanceId+"]"));
	}

	public Set<Range> getRanges() {
		return ranges;
	}
	
	public String getMarkupCollectionId() {
		return markupCollectionId;
	}
	
	public String getTagDefinitionId() {
		return tagDefinitionId;
	}
}
