package de.catma.repository.neo;

import de.catma.repository.neo.exceptions.NeoTagsetHandlerException;
import de.catma.tag.TagsetDefinition;
import de.catma.util.IDGenerator;

public class NeoTagsetHandler {
	private final IDGenerator idGenerator;

	public NeoTagsetHandler(){
		this.idGenerator = new IDGenerator();
	}

	public void insertTagset(TagsetDefinition tagsetDefinition) throws NeoTagsetHandlerException{
		throw new NeoTagsetHandlerException("Not implemented");
	}
}
