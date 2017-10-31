package de.catma.repository.neo;

import de.catma.repository.neo.exceptions.NeoTagsetHandlerException;
import de.catma.repository.neo.managers.Neo4JGraphManager;
import de.catma.tag.TagsetDefinition;
import de.catma.util.IDGenerator;

public class NeoTagsetHandler {
	private final Neo4JGraphManager graphManager;
	private final IDGenerator idGenerator;

	public NeoTagsetHandler(Neo4JGraphManager graphManager){
		this.graphManager = graphManager;
		this.idGenerator = new IDGenerator();
	}

	public void insertTagset(TagsetDefinition tagsetDefinition) throws NeoTagsetHandlerException{
		throw new NeoTagsetHandlerException("Not implemented");
	}
}
