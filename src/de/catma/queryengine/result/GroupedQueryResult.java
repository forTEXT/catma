package de.catma.queryengine.result;

import java.util.Set;

public interface GroupedQueryResult extends Iterable<QueryResultRow> {
	public Object getGroup();
	
	public int getTotalFrequency();
	
	public int getFrequency(String sourceDocumentID);
	
	public Set<String> getSourceDocumentIDs();

	public GroupedQueryResult getSubResult(String... sourceDocumentID);
}
