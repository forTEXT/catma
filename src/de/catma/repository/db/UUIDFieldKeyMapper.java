package de.catma.repository.db;

import org.jooq.Field;
import org.jooq.Record;

import com.google.common.base.Function;

import de.catma.util.IDGenerator;

public class UUIDFieldKeyMapper implements Function<Record, String> {
	
	private IDGenerator idGenerator = new IDGenerator();
	private Field<byte[]> uuidByteField;
	
	public UUIDFieldKeyMapper(Field<byte[]> uuidByteField) {
		this.uuidByteField = uuidByteField;
	}

	public String apply(Record r) {
		return idGenerator.uuidBytesToCatmaID(r.getValue(uuidByteField));
	}

}
