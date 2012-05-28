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
