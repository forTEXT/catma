package de.catma.repository.neo4j;

import de.catma.repository.neo4j.managers.Neo4JGraphManager;
import de.catma.tag.*;
import de.catma.util.IDGenerator;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.v1.Session;

import java.io.FileInputStream;
import java.util.Properties;

import static org.junit.Assert.*;

public class Neo4JTagsetHandlerTest {
	private Properties catmaProperties;

	private IDGenerator idGenerator;

	public Neo4JTagsetHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));

		this.idGenerator = new IDGenerator();
	}

	@After
	public void tearDown() throws Exception {
		try (Neo4JGraphManager graphManager = new Neo4JGraphManager(this.catmaProperties)) {
			Session session = graphManager.openSession();

//			session.run("MATCH (n) DETACH DELETE n");
		}
	}

	// how to test for exceptions: https://stackoverflow.com/a/31826781
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void insertTagset() throws Exception {
		try (Neo4JGraphManager graphManager = new Neo4JGraphManager(this.catmaProperties)) {

			Neo4JTagsetHandler neo4JTagsetHandler = new Neo4JTagsetHandler(graphManager);

			String uuid = this.idGenerator.generate();

			TagsetDefinition tagsetDefinition = new TagsetDefinition();
			tagsetDefinition.setUuid(uuid);
			tagsetDefinition.setName("ALovelyName");

			neo4JTagsetHandler.insertTagset(tagsetDefinition);
		}
	}

	@Test
	public void insertTagsetWithTagDefinitions() throws Exception {
		try (Neo4JGraphManager graphManager = new Neo4JGraphManager(this.catmaProperties)) {

			Neo4JTagsetHandler neo4JTagsetHandler = new Neo4JTagsetHandler(graphManager);

			String uuid = this.idGenerator.generate();

			TagsetDefinition tagsetDefinition = new TagsetDefinition();
			tagsetDefinition.setUuid(uuid);
			tagsetDefinition.setName("ALovelyName");

			TagDefinition tagDefinition1 = new TagDefinition();
			tagDefinition1.setUuid("CATMA_TagDefinition1");
			tagDefinition1.setName("TagsetDefinition1");

			TagDefinition tagDefinition2 = new TagDefinition();
			tagDefinition2.setUuid("CATMA_TagDefinition2");
			tagDefinition2.setParentUuid("CATMA_TagDefinition1");
			tagDefinition2.setName("TagsetDefinition2");

			tagsetDefinition.addTagDefinition(tagDefinition1);
			tagsetDefinition.addTagDefinition(tagDefinition2);

			neo4JTagsetHandler.insertTagset(tagsetDefinition);
		}
	}
}
