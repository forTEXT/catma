package de.catma.indexer.db;

import static de.catma.repository.db.jooqgen.catmaindex.Tables.POSITION;
import static de.catma.repository.db.jooqgen.catmaindex.Tables.TERM;

import java.util.HashMap;

import org.jooq.Record;
import org.jooq.RecordMapper;

import de.catma.indexer.db.model.Position;
import de.catma.indexer.db.model.Term;

public class PositionMapper implements RecordMapper<Record, Position> {

	private HashMap<Integer, Term> termCache;
	private TermMapper termMapper;
	
	public PositionMapper() {
		this.termCache= new HashMap<Integer, Term>();
		this.termMapper = new TermMapper();
	}
	
	
	public Position map(Record record) {
		Integer termId = record.getValue(TERM.TERMID);
		Term term = termCache.get(termId);
		if (term == null) {
			term = termMapper.map(record);
			termCache.put(termId, term);
		}
		
		return new Position(
			record.getValue(POSITION.POSITIONID),
			term,
			record.getValue(POSITION.CHARACTERSTART),
			record.getValue(POSITION.CHARACTEREND),
			record.getValue(POSITION.TOKENOFFSET));
	}
	
}
