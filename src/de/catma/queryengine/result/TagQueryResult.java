package de.catma.queryengine.result;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author marco.petris@web.de
 *
 */
public class TagQueryResult implements GroupedQueryResult, QueryResult {
	
	private String group;
	private QueryResultRowArray rows;
	
	public TagQueryResult(String group) {
		this.group = group;
		this.rows = new QueryResultRowArray();
	}
	
	public int getFrequency(String sourceDocumentID) {
		return this.rows.getFrequency(sourceDocumentID);
	}
	
	public Object getGroup() {
		return group;
	}
	
	public Set<String> getSourceDocumentIDs() {
		return rows.getSourceDocumentIDs();
	}

	public int getTotalFrequency() {
		return rows.getTotalFrequency();
	}
	
	public Iterator<QueryResultRow> iterator() {
		return rows.iterator();
	}
	
	public Set<GroupedQueryResult> asGroupedSet() {
		return rows.asGroupedSet();
	}
	
	public QueryResultRowArray asQueryResultRowArray() {
		return rows;
	}
	
	public void add(TagQueryResultRow tagQueryResultRow) {
		rows.add(tagQueryResultRow);
	}

	public GroupedQueryResult getSubResult(String... sourceDocumentID) {
		Set<String> filterSourceDocumentIds = new HashSet<String>(); 
		filterSourceDocumentIds.addAll(Arrays.asList(sourceDocumentID));
		
		PhraseResult subResult = new PhraseResult(group);
		for (QueryResultRow row : this) {
			if (filterSourceDocumentIds.contains(row.getSourceDocumentId())) {
				subResult.addQueryResultRow(row);
			}
		}
		return subResult;
	}

}
