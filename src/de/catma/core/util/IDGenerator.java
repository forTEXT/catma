package de.catma.core.util;

import java.util.UUID;

public class IDGenerator {
	public final static String ID_PREFIX = "CATMA_";
	public String generate() {
		return ID_PREFIX + UUID.randomUUID().toString();
	}
}
