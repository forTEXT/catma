package de.catma.queryengine.result;

import de.catma.core.document.Range;

public class QueryResultRow {

	private String sourceDocumentId;
	private Range range;
	private String phrase;

	public QueryResultRow(String sourceDocumentId, Range range, String phrase) {
		super();
		this.sourceDocumentId = sourceDocumentId;
		this.range = range;
		this.phrase = phrase;
	}
	
	public QueryResultRow(String sourceDocumentId, Range range) {
		this(sourceDocumentId, range, null);
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

	public void setPhrase(String phrase) {
		this.phrase = phrase;
	}
	
	@Override
	public String toString() {
		return "SourceDoc[#"+sourceDocumentId + "]"+range
				+ ((phrase == null)?"phrase not set":"->"+phrase+"<- "); 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((range == null) ? 0 : range.hashCode());
		result = prime
				* result
				+ ((sourceDocumentId == null) ? 0 : sourceDocumentId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryResultRow other = (QueryResultRow) obj;
		if (range == null) {
			if (other.range != null)
				return false;
		} else if (!range.equals(other.range))
			return false;
		if (sourceDocumentId == null) {
			if (other.sourceDocumentId != null)
				return false;
		} else if (!sourceDocumentId.equals(other.sourceDocumentId))
			return false;
		return true;
	}
	
	
}
