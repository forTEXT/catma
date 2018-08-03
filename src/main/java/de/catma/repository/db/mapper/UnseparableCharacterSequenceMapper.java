package de.catma.repository.db.mapper;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.UNSEPARABLE_CHARSEQUENCE;

import org.jooq.Record;
import org.jooq.RecordMapper;

public class UnseparableCharacterSequenceMapper implements
		RecordMapper<Record, String> {

	public String map(Record record) {
		return record.getValue(UNSEPARABLE_CHARSEQUENCE.CHARSEQUENCE);
	}
}
