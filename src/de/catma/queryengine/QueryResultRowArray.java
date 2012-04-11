package de.catma.queryengine;

import java.util.ArrayList;
import java.util.List;

public class QueryResultRowArray implements QueryResult {
	private List<QueryResultRow> rows;

	public QueryResultRowArray() {
		rows = new ArrayList<QueryResultRow>();
	}
	
	public void add(QueryResultRow queryResultRow) {
		rows.add(queryResultRow);
	}
}
