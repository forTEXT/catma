package de.catma.queryengine;

import de.catma.core.document.Range;

public class QueryResultRow {

	private String sourceDocumentId;
	private Range range;
	private String markupDocumentId;
	private String tagDefinitionId;
	private String tagInstanceId;
	
	
	public QueryResultRow(String sourceDocumentId, Range range) {
		super();
		this.sourceDocumentId = sourceDocumentId;
		this.range = range;
	}
	
	public QueryResultRow(String sourceDocumentId, Range range,
			String markupDocumentId, String tagDefinitionId,
			String tagInstanceId) {
		super();
		this.sourceDocumentId = sourceDocumentId;
		this.range = range;
		this.markupDocumentId = markupDocumentId;
		this.tagDefinitionId = tagDefinitionId;
		this.tagInstanceId = tagInstanceId;
	}
	
	@Deprecated
	public QueryResultRow(String sourceDocumentId, Range range, String phrase,
			String markupDocumentId, String tagDefinitionId,
			String tagInstanceId) {
		super();
		this.sourceDocumentId = sourceDocumentId;
		this.range = range;
		this.markupDocumentId = markupDocumentId;
		this.tagDefinitionId = tagDefinitionId;
		this.tagInstanceId = tagInstanceId;
	}

	public String getSourceDocumentId() {
		return sourceDocumentId;
	}
	
	public Range getRange() {
		return range;
	}	

	@Override
	public String toString() {
		return "SourceDoc[#"+sourceDocumentId + "]"+range
				+ ((markupDocumentId == null)?"":("MarkupDoc[#"+markupDocumentId+"]"))
				+ ((tagDefinitionId == null)?"":("TagDef[#"+tagDefinitionId+"]")) 
				+ ((tagInstanceId == null)?"":("TagInstance[#"+tagInstanceId+"]")); 
	}
}
