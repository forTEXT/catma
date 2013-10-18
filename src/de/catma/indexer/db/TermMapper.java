package de.catma.indexer.db;

import static de.catma.repository.db.jooqgen.catmaindex.Tables.TERM;

import org.jooq.Record;
import org.jooq.RecordMapper;

import de.catma.indexer.db.model.Term;

public class TermMapper implements RecordMapper<Record, Term> {
	
	public Term map(Record record) {
		return new Term(
			record.getValue(TERM.TERMID),
			record.getValue(TERM.DOCUMENTID),
			record.getValue(TERM.FREQUENCY),
			record.getValue(TERM.TERM_));
	}

}
