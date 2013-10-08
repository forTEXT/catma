package de.catma.repository.db.mapper;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.TableField;

import de.catma.util.Pair;


public class IDFieldsToIntegerPairMapper implements
		RecordMapper<Record, Pair<Integer,Integer>> {
	
	private TableField<Record, Integer> field1;
	private TableField<Record, Integer> field2;
	
	public IDFieldsToIntegerPairMapper(
			TableField<Record, Integer> field1,
			TableField<Record, Integer> field2) {
		this.field1 = field1;
		this.field2 = field2;
	}

	public Pair<Integer,Integer> map(Record record) {
		return new Pair<Integer,Integer>(
			record.getValue(field1),
			record.getValue(field2));
	}
}
