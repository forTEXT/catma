package de.catma.queryengine.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class QueryResultRowArray implements QueryResult {
	private List<QueryResultRow> rows;

	public QueryResultRowArray() {
		rows = new ArrayList<QueryResultRow>();
	}
	
	public void add(QueryResultRow queryResultRow) {
		rows.add(queryResultRow);
	}
	
	@Override
	public String toString() {
		return Arrays.toString(rows.toArray());
	}
	
	public Iterator<QueryResultRow> iterator() {
		return rows.iterator();
	}

	public void addAll(QueryResultRowArray queryResult) {
		rows.addAll(queryResult.rows);
	}
	
	public int getFrequency(String sourceDocumentID) {
		int sum = 0;
		for (QueryResultRow row : rows) {
			if (row.getSourceDocumentId().equals(sourceDocumentID)) {
				sum++;
			}
		}
		return sum;
	}
	
	public Set<String> getSourceDocumentIDs() {
		Set<String> sourceDocumentIDs = new HashSet<String>();
		for (QueryResultRow row : rows) {
			sourceDocumentIDs.add(
					row.getSourceDocumentId());
		}
		return sourceDocumentIDs;
	}
	
	public int getTotalFrequency() {
		return rows.size();
	}
	
	public Set<GroupedQueryResult> asGroupedQueryResultSet() {
		HashMap<String, PhraseResult> phraseResultMapping = 
				new HashMap<String, PhraseResult>();
		
		for (QueryResultRow row : rows) {
			if (!phraseResultMapping.containsKey(row.getPhrase())) {
				phraseResultMapping.put(
					row.getPhrase(), new PhraseResult(row.getPhrase()));
			}
			
			phraseResultMapping.get(row.getPhrase()).addQueryResultRow(row);
		}
		
		Set<GroupedQueryResult> groupedQueryResults = 
				new HashSet<GroupedQueryResult>();
		
		groupedQueryResults.addAll(phraseResultMapping.values());
		
		return groupedQueryResults;
	}
}
