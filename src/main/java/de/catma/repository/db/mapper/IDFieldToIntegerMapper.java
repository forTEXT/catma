package de.catma.repository.db.mapper;

import org.jooq.Record;
import org.jooq.TableField;


public class IDFieldToIntegerMapper extends FieldToValueMapper<Integer> {
	public IDFieldToIntegerMapper(
			TableField<Record, Integer> field) {
		super(field);
	}
}
