package de.catma.indexer.db;

import static de.catma.repository.db.jooqgen.catmaindex.Tables.POSITION;
import static de.catma.repository.db.jooqgen.catmaindex.Tables.TERM;

import org.jooq.Record;
import org.jooq.RecordMapper;

import de.catma.document.Range;
import de.catma.queryengine.QueryId;
import de.catma.queryengine.result.QueryResultRow;

public class QueryResultRowMapper implements
		RecordMapper<Record, QueryResultRow> {

	private String documentId;
	private QueryId queryId;

	public QueryResultRowMapper(QueryId queryId, String documentId) {
		this.queryId = queryId;
		this.documentId = documentId;
	}

	public QueryResultRow map(Record record) {
		return new QueryResultRow(
				queryId,
				documentId, 
				new Range(
					record.getValue(POSITION.CHARACTERSTART), 
					record.getValue(POSITION.CHARACTEREND)), 
				record.getValue(TERM.TERM_));
	}
	
}
