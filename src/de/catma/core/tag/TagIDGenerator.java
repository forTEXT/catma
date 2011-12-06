package de.catma.core.tag;

import java.util.UUID;

public class TagIDGenerator {
	public String generate() {
		return "CATMA_" + UUID.randomUUID().toString();
	}
}
