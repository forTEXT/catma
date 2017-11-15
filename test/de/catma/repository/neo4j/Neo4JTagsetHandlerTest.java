package de.catma.repository.neo4j;

import de.catma.document.AccessMode;
import de.catma.document.Range;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.repository.neo4j.managers.Neo4JGraphManager;
import de.catma.repository.neo4j.managers.Neo4JOGMSessionFactory;
import de.catma.repository.neo4j.serialization.model_wrappers.Neo4JTagsetDefinition;
import de.catma.repository.neo4j.serialization.model_wrappers.Neo4JUserMarkupCollection;
import de.catma.tag.*;
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
			tagsetDefinition.setRevisionHash("ABC123XYZ");

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

			Neo4JTagsetDefinition neo4JTagsetDefinition = new Neo4JTagsetDefinition(
					tagsetDefinition.getUuid(), tagsetDefinition.getName()
			);

			neo4JTagsetDefinition.addTagsetWorktree(tagsetDefinition);

			org.neo4j.ogm.session.Session session = neo4JOGMSessionFactory.getSession();

			session.save(neo4JTagsetDefinition);
			session.clear();

			session = neo4JOGMSessionFactory.getSession();

			Neo4JTagsetDefinition loaded = session.load(
					Neo4JTagsetDefinition.class, tagsetDefinition.getUuid(), 3
			);
			TagsetDefinition loadedTagsetDefinition = loaded.getTagsetWorktree(tagsetDefinition.getRevisionHash());

			assertEquals(uuid, loadedTagsetDefinition.getUuid());
			assertEquals("ALovelyName", loadedTagsetDefinition.getName());

			assertTrue(loadedTagsetDefinition.hasTagDefinition(tagDefinition1.getUuid()));
			assertTrue(loadedTagsetDefinition.hasTagDefinition(tagDefinition2.getUuid()));
			assertTrue(loadedTagsetDefinition.hasTagDefinition(tagDefinition3.getUuid()));

			TagDefinition loadedTagDefinition1 = loadedTagsetDefinition.getTagDefinition(tagDefinition1.getUuid());

			assertEquals(1, loadedTagDefinition1.getSystemPropertyDefinitions().size());
			assertEquals(2, loadedTagDefinition1.getUserDefinedPropertyDefinitions().size());
		}
	}

	@Test
	public void ogmInsertMarkupCollection() throws Exception {
		try (Neo4JOGMSessionFactory neo4JOGMSessionFactory = new Neo4JOGMSessionFactory(this.catmaProperties)) {

			TagDefinition tagDefinition = new TagDefinition();
			tagDefinition.setUuid("CATMA_TagDefinition");
			tagDefinition.setName("TagDefinition");

			PropertyDefinition userDefinedPropertyDefinition = getFakePropertyDefinition(
					"TagDefinitionPropDef",
					true,
					"Verb", "Noun"
			);
			tagDefinition.addUserDefinedPropertyDefinition(userDefinedPropertyDefinition);

			PropertyDefinition systemPropertyDefinition = getFakePropertyDefinition(
					PropertyDefinition.SystemPropertyName.catma_markupauthor.toString(),
					true,
					"Frank", "Joe"
			);
			tagDefinition.addSystemPropertyDefinition(systemPropertyDefinition);

			TagInstance tagInstance = new TagInstance("CATMA_TagInstance", tagDefinition);
			// the TagInstance constructor sets default values for system properties, so we need to clear them
			for (Property property : tagInstance.getSystemProperties()) {
				property.setPropertyValueList(new PropertyValueList());
			}

			Property userDefinedProperty = new Property(userDefinedPropertyDefinition, new PropertyValueList("Verb"));
			Property systemProperty = new Property(systemPropertyDefinition, new PropertyValueList("Frank"));

			tagInstance.addUserDefinedProperty(userDefinedProperty);
			tagInstance.addSystemProperty(systemProperty);

			String fakeSourceDocumentUri = "http://catma.de/gitlab/fakeProjectId_corpus/documents/fakeSourceDocumentId";

			Range range1 = new Range(1, 6);
			Range range2 = new Range(7, 12);

			String userMarkupCollectionUuid = this.idGenerator.generate();
			String userMarkupCollectionName = "Frank's Markup Collection";

			TagReference tagReference1 = new TagReference(
					tagInstance, fakeSourceDocumentUri, range1, userMarkupCollectionUuid
			);

			TagReference tagReference2 = new TagReference(
					tagInstance, fakeSourceDocumentUri, range2, userMarkupCollectionUuid
			);

			ContentInfoSet contentInfoSet = new ContentInfoSet(
					"Frank", "Test Description", "Frank", userMarkupCollectionName
			);

			// we are hoping to get rid of tag libraries altogether
			TagLibrary tagLibrary = new TagLibrary(null, null);

			UserMarkupCollection userMarkupCollection = new UserMarkupCollection(
					null, userMarkupCollectionUuid,
					contentInfoSet,
					tagLibrary,
					Arrays.asList(tagReference1, tagReference2),
					AccessMode.WRITE
			);
			userMarkupCollection.setRevisionHash("ABC123XYZ");

			Neo4JUserMarkupCollection neo4JUserMarkupCollection = new Neo4JUserMarkupCollection(
					userMarkupCollectionUuid, userMarkupCollectionName
			);

			neo4JUserMarkupCollection.addMarkupCollectionWorktree(userMarkupCollection);

			org.neo4j.ogm.session.Session session = neo4JOGMSessionFactory.getSession();

			session.save(neo4JUserMarkupCollection);
			session.clear();

			session = neo4JOGMSessionFactory.getSession();

			Neo4JUserMarkupCollection loaded = session.load(
					Neo4JUserMarkupCollection.class, userMarkupCollection.getUuid(), 4
			);
			UserMarkupCollection loadedUserMarkupCollection = loaded.getMarkupCollectionWorktree(
					userMarkupCollection.getRevisionHash()
			);

			assertEquals(userMarkupCollectionUuid, loadedUserMarkupCollection.getUuid());
			assertEquals(userMarkupCollectionName, loadedUserMarkupCollection.getContentInfoSet().getTitle());

			assertEquals(2, loadedUserMarkupCollection.getTagReferences().size());

			assertEquals(
					tagInstance.getUuid(),
					loadedUserMarkupCollection.getTagReferences().get(0).getTagInstance().getUuid()
			);
			assertEquals(
					tagInstance.getUuid(),
					loadedUserMarkupCollection.getTagReferences().get(1).getTagInstance().getUuid()
			);

			assertEquals(
					tagDefinition.getUuid(),
					loadedUserMarkupCollection.getTagReferences().get(0).getTagInstance().getTagDefinition().getUuid()
			);
			assertEquals(
					tagDefinition.getUuid(),
					loadedUserMarkupCollection.getTagReferences().get(1).getTagInstance().getTagDefinition().getUuid()
			);
		}
	}
}
