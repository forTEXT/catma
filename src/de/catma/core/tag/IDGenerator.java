package de.catma.core.tag;

import java.util.UUID;

public class IDGenerator {
	public final static String ID_PREFIX = "CATMA_";
	public String generate() {
		return ID_PREFIX + UUID.randomUUID().toString();
	}
}
