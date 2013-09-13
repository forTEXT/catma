package de.catma.repository.db;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.TableField;


public class IDFieldToIntegerMapper implements
		RecordMapper<Record, Integer> {
	
	private TableField<Record, Integer> field;

	public IDFieldToIntegerMapper(
			TableField<Record, Integer> field) {
		this.field = field;
	}

	public Integer map(Record record) {
		return record.getValue(field);
	}
}
