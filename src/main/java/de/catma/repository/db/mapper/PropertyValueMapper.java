package de.catma.repository.db.mapper;

import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYVALUE;

import org.jooq.Record;
import org.jooq.RecordMapper;

public class PropertyValueMapper implements
		RecordMapper<Record, String> {

	public String map(Record record) {
		return record.getValue(PROPERTYVALUE.VALUE);
	}
}
