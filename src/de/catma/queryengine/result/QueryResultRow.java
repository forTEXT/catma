package de.catma.queryengine.result;

import de.catma.core.document.Range;

public class QueryResultRow {

	private String sourceDocumentId;
	private Range range;
	private String phrase;
	private String markupCollectionId;
	private String tagDefinitionId;
	private String tagInstanceId;

	public QueryResultRow(String sourceDocumentId, Range range, String phrase) {
		super();
		this.sourceDocumentId = sourceDocumentId;
		this.range = range;
		this.phrase = phrase;
	}
	
	public QueryResultRow(String sourceDocumentId, Range range) {
		super();
		this.sourceDocumentId = sourceDocumentId;
		this.range = range;
	}
	
	public QueryResultRow(String sourceDocumentId, Range range,
			String markupCollectionId, String tagDefinitionId,
			String tagInstanceId) {
		super();
		this.sourceDocumentId = sourceDocumentId;
		this.range = range;
		this.markupCollectionId = markupCollectionId;
		this.tagDefinitionId = tagDefinitionId;
		this.tagInstanceId = tagInstanceId;
	}
	
	public QueryResultRow(String sourceDocumentId, Range range, String phrase,
			String markupDocumentId, String tagDefinitionId,
			String tagInstanceId) {
		super();
		this.sourceDocumentId = sourceDocumentId;
		this.range = range;
		this.phrase = phrase;
		this.markupCollectionId = markupDocumentId;
		this.tagDefinitionId = tagDefinitionId;
		this.tagInstanceId = tagInstanceId;
	}

	public String getSourceDocumentId() {
		return sourceDocumentId;
	}
	
	public Range getRange() {
		return range;
	}	

	public String getPhrase() {
		return phrase;
	}
	
	public String getTagInstanceId() {
		return tagInstanceId;
	}
	
	@Override
	public String toString() {
		return "SourceDoc[#"+sourceDocumentId + "]"+range
				+ ((phrase == null)?"":phrase)
				+ ((markupCollectionId == null)?"":("MarkupColl[#"+markupCollectionId+"]"))
				+ ((tagDefinitionId == null)?"":("TagDef[#"+tagDefinitionId+"]")) 
				+ ((tagInstanceId == null)?"":("TagInstance[#"+tagInstanceId+"]")); 
	}
}
