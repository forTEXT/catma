package de.catma.tag;

import de.catma.util.IDGenerator;

public enum KnownTagsetDefinitionName {
	DEFAULT_INTRINSIC_XML,
	;
	
	public String asTagsetId() {
		IDGenerator idGenerator = new IDGenerator();
		
		return idGenerator.generateTagsetId(this.name());
	}
}
