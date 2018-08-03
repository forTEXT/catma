package de.catma.repository.db.mapper;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERDEFINED_SEPARATINGCHARACTER;

import org.jooq.Record;
import org.jooq.RecordMapper;

public class UserDefinedSeparatingCharacterMapper implements
		RecordMapper<Record, Character> {

	public Character map(Record record) {
		return record.getValue(USERDEFINED_SEPARATINGCHARACTER.CHR).toCharArray()[0];
	}
}
