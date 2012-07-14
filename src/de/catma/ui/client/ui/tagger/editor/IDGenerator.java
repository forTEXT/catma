package de.catma.ui.client.ui.tagger.editor;

import de.catma.ui.client.ui.util.UUID;

public class IDGenerator {

	public static String generate() {
		return "CATMA_" + UUID.uuid().toUpperCase();
	}
	
}
