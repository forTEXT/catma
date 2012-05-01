package de.catma.queryengine.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class QueryResultRowArray implements QueryResult, Iterable<QueryResultRow> {
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
}
