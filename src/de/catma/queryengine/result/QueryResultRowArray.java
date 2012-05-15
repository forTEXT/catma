package de.catma.queryengine.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class QueryResultRowArray extends ArrayList<QueryResultRow> implements QueryResult {

	public QueryResultRowArray() {
	}

	@Override
	public String toString() {
		return Arrays.toString(this.toArray());
	}
	
	public int getFrequency(String sourceDocumentID) {
		int sum = 0;
		for (QueryResultRow row : this) {
			if (row.getSourceDocumentId().equals(sourceDocumentID)) {
				sum++;
			}
		}
		return sum;
	}
	
	public Set<String> getSourceDocumentIDs() {
		Set<String> sourceDocumentIDs = new HashSet<String>();
		for (QueryResultRow row : this) {
			sourceDocumentIDs.add(
					row.getSourceDocumentId());
		}
		return sourceDocumentIDs;
	}
	
	public int getTotalFrequency() {
		return size();
	}
	
	public Set<GroupedQueryResult> asGroupedQueryResultSet() {
		return GroupedQueryResultSet.asGroupedQueryResultSet(this);
	}
}
