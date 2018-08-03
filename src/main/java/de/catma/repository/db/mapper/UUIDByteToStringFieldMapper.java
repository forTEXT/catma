package de.catma.repository.db.mapper;

import org.jooq.Record1;
import org.jooq.RecordMapper;

import de.catma.util.IDGenerator;

public class UUIDByteToStringFieldMapper implements
		RecordMapper<Record1<byte[]>, String> {

	private IDGenerator idGenerator = new IDGenerator();
	
	public String map(Record1<byte[]> record) {
		return idGenerator.uuidBytesToCatmaID(record.value1());
	}

}
