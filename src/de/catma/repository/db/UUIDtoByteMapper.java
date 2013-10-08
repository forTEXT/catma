package de.catma.repository.db;

import com.google.common.base.Function;

import de.catma.util.IDGenerator;

public class UUIDtoByteMapper implements Function<String, byte[]> {

	private IDGenerator idGenerator = new IDGenerator();
	
	public byte[] apply(String uuid) {
		return idGenerator.catmaIDToUUIDBytes(uuid);
	}

}
