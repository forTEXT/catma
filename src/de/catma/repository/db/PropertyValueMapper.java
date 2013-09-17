package de.catma.repository.db;

import static de.catma.repository.db.jooq.catmarepository.Tables.PROPERTYVALUE;

import org.jooq.Record;
import org.jooq.RecordMapper;

public class PropertyValueMapper implements
		RecordMapper<Record, String> {

	public String map(Record record) {
		return record.getValue(PROPERTYVALUE.VALUE);
	}
}
