package de.catma.queryengine.result;

import java.util.HashSet;
import java.util.Set;

public class QueryResultRowSet extends HashSet<QueryResultRow> implements QueryResult {
	
	public QueryResultRowSet() {
	}
	
	public QueryResultRowSet(Iterable<QueryResultRow> source) {
		for (QueryResultRow row : source) {
			add(row);
		}
	}
	
	public Set<GroupedQueryResult> asGroupedQueryResultSet() {
		return GroupedQueryResultSet.asGroupedQueryResultSet(this);
	}
}
