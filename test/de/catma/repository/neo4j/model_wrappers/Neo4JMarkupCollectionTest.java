package de.catma.repository.neo4j.model_wrappers;

import de.catma.document.AccessMode;
import de.catma.document.Range;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.repository.neo4j.Neo4JOGMSessionFactory;
import de.catma.tag.*;
import de.catma.util.IDGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.session.Session;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.*;

public class Neo4JMarkupCollectionTest {
	private Properties catmaProperties;

	private IDGenerator idGenerator;

	public Neo4JMarkupCollectionTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));

		this.idGenerator = new IDGenerator();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		try (Neo4JOGMSessionFactory neo4JOGMSessionFactory = new Neo4JOGMSessionFactory(this.catmaProperties)) {
			Session session = neo4JOGMSessionFactory.getSession();

			// TODO: delete exactly the graph created by the test so that it's safe to run this even against the
			// production DB
//			session.query("MATCH (n) DETACH DELETE n", new HashMap<>());
		}
	}

	@Test
	public void insertMarkupCollection() throws Exception {
		try (Neo4JOGMSessionFactory neo4JOGMSessionFactory = new Neo4JOGMSessionFactory(this.catmaProperties)) {

			TagDefinition tagDefinition = new TagDefinition();
			tagDefinition.setUuid("CATMA_TagDefinition");
			tagDefinition.setName("TagDefinition");

			PropertyDefinition userDefinedPropertyDefinition = Neo4JTagsetTest.getFakePropertyDefinition(
					"TagDefinitionPropDef",
					true,
					"Verb", "Noun"
			);
			tagDefinition.addUserDefinedPropertyDefinition(userDefinedPropertyDefinition);

			PropertyDefinition systemPropertyDefinition = Neo4JTagsetTest.getFakePropertyDefinition(
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

			Neo4JMarkupCollection neo4JMarkupCollection = new Neo4JMarkupCollection(userMarkupCollection);

			org.neo4j.ogm.session.Session session = neo4JOGMSessionFactory.getSession();

			session.save(neo4JMarkupCollection);

			session = neo4JOGMSessionFactory.getSession();

			Neo4JMarkupCollection loaded = session.load(
					Neo4JMarkupCollection.class, neo4JMarkupCollection.getId(), 4
			);
			UserMarkupCollection loadedUserMarkupCollection = loaded.getUserMarkupCollection();

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
