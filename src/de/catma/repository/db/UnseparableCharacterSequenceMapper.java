package de.catma.repository.db;

import static de.catma.repository.db.jooq.catmarepository.Tables.UNSEPARABLE_CHARSEQUENCE;

import org.jooq.Record;
import org.jooq.RecordMapper;

public class UnseparableCharacterSequenceMapper implements
		RecordMapper<Record, String> {

	public String map(Record record) {
		return record.getValue(UNSEPARABLE_CHARSEQUENCE.CHARSEQUENCE);
	}
}
