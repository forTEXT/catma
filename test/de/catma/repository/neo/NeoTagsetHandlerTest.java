package de.catma.repository.neo;

import de.catma.repository.neo.exceptions.NeoTagsetHandlerException;
import de.catma.repository.neo.managers.Neo4JGraphManager;
import de.catma.repository.neo.managers.Neo4JOGMSessionFactory;
import de.catma.repository.neo.serialization.models.Neo4JTagsetDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.util.IDGenerator;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.v1.Session;

import java.io.FileInputStream;
import java.util.Properties;

import static org.junit.Assert.*;

public class NeoTagsetHandlerTest {
	private Properties catmaProperties;

	private IDGenerator idGenerator;

	public NeoTagsetHandlerTest() throws Exception {
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

			NeoTagsetHandler neoTagsetHandler = new NeoTagsetHandler(graphManager);

			String uuid = this.idGenerator.generate();

			TagsetDefinition tagsetDefinition = new TagsetDefinition();
			tagsetDefinition.setUuid(uuid);
			tagsetDefinition.setName("ALovelyName");

			neoTagsetHandler.insertTagset(tagsetDefinition);
		}
	}

	@Test
	public void insertTagsetWithTagDefinitions() throws Exception {
		try (Neo4JGraphManager graphManager = new Neo4JGraphManager(this.catmaProperties)) {

			NeoTagsetHandler neoTagsetHandler = new NeoTagsetHandler(graphManager);

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

			neoTagsetHandler.insertTagset(tagsetDefinition);
		}
	}

	@Test
	public void ogmInsertTagsetWithTagDefinitions() throws Exception {
		try (Neo4JOGMSessionFactory neo4JOGMSessionFactory = new Neo4JOGMSessionFactory(this.catmaProperties)) {

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

//			neoTagsetHandler.insertTagset(tagsetDefinition);

			Neo4JTagsetDefinition neo4JTagsetDefinition = new Neo4JTagsetDefinition(tagsetDefinition);

			org.neo4j.ogm.session.Session session = neo4JOGMSessionFactory.getSession();

			session.save(neo4JTagsetDefinition);
		}
	}

}
