package de.catma.repository.neo;

import de.catma.repository.neo.exceptions.NeoTagsetHandlerException;
import de.catma.repository.neo.managers.Neo4JGraphManager;
import de.catma.repository.neo.managers.Neo4JOGMSessionFactory;
import de.catma.repository.neo.serialization.models.Neo4JTagsetDefinition;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyPossibleValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;
import de.catma.util.IDGenerator;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.v1.Session;

import java.io.FileInputStream;
import java.util.Arrays;
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

	public PropertyDefinition getFakePropertyDefinition(String name, boolean singleSelect, String... values){
		String uuid = this.idGenerator.generate();

		PropertyPossibleValueList userPropertyPossibleValues = new PropertyPossibleValueList(
				Arrays.asList(values), singleSelect
		);

		PropertyDefinition propertyDefinition = new PropertyDefinition(
				null, uuid, name, userPropertyPossibleValues
		);

		return propertyDefinition;
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
			tagDefinition1.setName("TagDefinition1");

			tagDefinition1.addUserDefinedPropertyDefinition(
					getFakePropertyDefinition(
							"TagDefinition1PropDef1",
							true,
							"Verb", "Noun"
					));
			tagDefinition1.addUserDefinedPropertyDefinition(
					getFakePropertyDefinition(
							"TagDefinition1PropDef2",
							true,
							"Monday", "Friday"
					));
			tagDefinition1.addSystemPropertyDefinition(
					getFakePropertyDefinition(
							PropertyDefinition.SystemPropertyName.catma_markupauthor.toString(),
							true,
							"Frank", "Joe"
					));

			TagDefinition tagDefinition2 = new TagDefinition();
			tagDefinition2.setUuid("CATMA_TagDefinition2");
			tagDefinition2.setParentUuid("CATMA_TagDefinition1");
			tagDefinition2.setName("TagDefinition2");

			TagDefinition tagDefinition3 = new TagDefinition();
			tagDefinition3.setUuid("CATMA_TagDefinition3");
			tagDefinition3.setParentUuid("CATMA_TagDefinition2");
			tagDefinition3.setName("TagDefinition3");

			tagsetDefinition.addTagDefinition(tagDefinition1);
			tagsetDefinition.addTagDefinition(tagDefinition2);
			tagsetDefinition.addTagDefinition(tagDefinition3);

			Neo4JTagsetDefinition neo4JTagsetDefinition = new Neo4JTagsetDefinition(tagsetDefinition);

			org.neo4j.ogm.session.Session session = neo4JOGMSessionFactory.getSession();

			session.save(neo4JTagsetDefinition);
			session.clear();

			session = neo4JOGMSessionFactory.getSession();

			Neo4JTagsetDefinition loaded = session.load(Neo4JTagsetDefinition.class, tagsetDefinition.getUuid(), 2);
			TagsetDefinition loadedTagsetDefinition = loaded.getTagsetDefinition();

			assertEquals(uuid, loadedTagsetDefinition.getUuid());
			assertEquals("ALovelyName", loadedTagsetDefinition.getName());

			assertTrue(loadedTagsetDefinition.hasTagDefinition(tagDefinition1.getUuid()));
			assertTrue(loadedTagsetDefinition.hasTagDefinition(tagDefinition2.getUuid()));
			assertTrue(loadedTagsetDefinition.hasTagDefinition(tagDefinition3.getUuid()));

			TagDefinition loadedTagDefinition1 = loadedTagsetDefinition.getTagDefinition(tagDefinition1.getUuid());

			assertEquals(0, loadedTagDefinition1.getSystemPropertyDefinitions().size());
			assertEquals(2, loadedTagDefinition1.getUserDefinedPropertyDefinitions().size());
		}
	}
}
