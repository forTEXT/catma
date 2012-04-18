package de.catma.queryengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
}
