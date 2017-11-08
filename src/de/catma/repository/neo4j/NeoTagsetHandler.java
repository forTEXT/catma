package de.catma.repository.neo4j;

import de.catma.repository.neo4j.exceptions.NeoTagsetHandlerException;
import de.catma.repository.neo4j.managers.Neo4JGraphManager;
import de.catma.tag.TagsetDefinition;
import de.catma.util.IDGenerator;
import org.neo4j.driver.v1.Session;

import static org.neo4j.driver.v1.Values.parameters;

public class NeoTagsetHandler {
	private final Neo4JGraphManager graphManager;
	private final IDGenerator idGenerator;

	public NeoTagsetHandler(Neo4JGraphManager graphManager){
		this.graphManager = graphManager;
		this.idGenerator = new IDGenerator();
	}

	public void insertTagset(TagsetDefinition tagsetDefinition) throws NeoTagsetHandlerException{
		try(Session session = this.graphManager.openSession()){
			session.run(
					"CREATE (a:TagsetDefinition {uuid: {uuid}, name: {name}})",
					parameters("uuid", tagsetDefinition.getUuid(), "name", tagsetDefinition.getName())
					);
		}
	}
}
