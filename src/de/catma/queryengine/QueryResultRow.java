package de.catma.queryengine;

import de.catma.core.document.Range;

public class QueryResultRow {

	private String sourceDocumentId;
	private Range range;
	private String phrase;
	private String markupDocumentId;
	private String tagDefinitionId;
	private String tagInstanceId;
	
	
	public QueryResultRow(String sourceDocumentId, Range range, String phrase) {
		super();
		this.sourceDocumentId = sourceDocumentId;
		this.range = range;
		this.phrase = phrase;
	}
	
	
	public QueryResultRow(String sourceDocumentId, Range range, String phrase,
			String markupDocumentId, String tagDefinitionId,
			String tagInstanceId) {
		super();
		this.sourceDocumentId = sourceDocumentId;
		this.range = range;
		this.phrase = phrase;
		this.markupDocumentId = markupDocumentId;
		this.tagDefinitionId = tagDefinitionId;
		this.tagInstanceId = tagInstanceId;
	}


	@Override
	public String toString() {
		return "SourceDoc[#"+sourceDocumentId + "]"+range + "Phrase[" + phrase + "]"
				+ ((markupDocumentId == null)?"":("MarkupDoc[#"+markupDocumentId+"]"))
				+ ((tagDefinitionId == null)?"":("TagDef[#"+tagDefinitionId+"]")) 
				+ ((tagInstanceId == null)?"":("TagInstance[#"+tagInstanceId+"]")); 
	}
}
