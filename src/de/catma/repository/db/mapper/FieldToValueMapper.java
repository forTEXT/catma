package de.catma.repository.db.mapper;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.TableField;


public class FieldToValueMapper<T> implements
		RecordMapper<Record, T> {
	
	private TableField<Record, T> field;

	public FieldToValueMapper(
			TableField<Record, T> field) {
		this.field = field;
	}

	public T map(Record record) {
		return record.getValue(field);
	}
}
