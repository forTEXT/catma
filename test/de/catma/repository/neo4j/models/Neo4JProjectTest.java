package de.catma.repository.neo4j.models;

import de.catma.document.source.*;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.models.Project;
import de.catma.repository.neo4j.Neo4JOGMSessionFactory;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.util.IDGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.session.Session;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

import static org.junit.Assert.*;

public class Neo4JProjectTest {
	private Properties catmaProperties;

	private IDGenerator idGenerator;

	public Neo4JProjectTest() throws Exception {
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
	public void insertProject() throws Exception {
		try (Neo4JOGMSessionFactory neo4JOGMSessionFactory = new Neo4JOGMSessionFactory(this.catmaProperties)) {
			String tagsetDefinitionUuid = this.idGenerator.generate();

			TagsetDefinition tagsetDefinition = new TagsetDefinition();
			tagsetDefinition.setUuid(tagsetDefinitionUuid);
			tagsetDefinition.setName("ALovelyName");
			tagsetDefinition.setRevisionHash("ABC123XYZ");

			String userMarkupCollectionUuid = this.idGenerator.generate();

			ContentInfoSet markupCollectionContentInfoSet = new ContentInfoSet(
					"Frank", "Test Description", "Frank", "Test Title"
			);

			// we are hoping to get rid of tag libraries altogether
			TagLibrary tagLibrary = new TagLibrary(null, null);

			UserMarkupCollection userMarkupCollection = new UserMarkupCollection(
					null, userMarkupCollectionUuid, markupCollectionContentInfoSet, tagLibrary
			);
			userMarkupCollection.setRevisionHash("ABC123XYZ");

			File convertedSourceDocument = new File("testdocs/rose_for_emily.txt");

			FileInputStream convertedSourceDocumentStream = new FileInputStream(convertedSourceDocument);

			IndexInfoSet indexInfoSet = new IndexInfoSet();
			indexInfoSet.setLocale(Locale.ENGLISH);

			String sourceDocumentUuid = this.idGenerator.generate();

			ContentInfoSet sourceDocumentContentInfoSet = new ContentInfoSet(
					"William Faulkner",
					"",
					"",
					"A Rose for Emily"
			);

			TechInfoSet techInfoSet = new TechInfoSet(
					FileType.TEXT,
					StandardCharsets.UTF_8,
					FileOSType.DOS,
					705211438L
			);

			SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo(
					indexInfoSet, sourceDocumentContentInfoSet, techInfoSet
			);

			StandardContentHandler standardContentHandler = new StandardContentHandler();
			standardContentHandler.setSourceDocumentInfo(sourceDocumentInfo);
			standardContentHandler.load(convertedSourceDocumentStream);

			SourceDocument sourceDocument = new SourceDocument(sourceDocumentUuid, standardContentHandler);
			sourceDocument.setRevisionHash("ABC123XYZ");

			String projectUuid = this.idGenerator.generate();
			String projectName = "Test Project";

			Project project = new Project(
					projectUuid, projectName, "Test Project Description", "ABC123XYZ"
			);
			project.addTagset(tagsetDefinition);
			project.addMarkupCollection(userMarkupCollection);
			project.addSourceDocument(sourceDocument);

			Neo4JProject neo4JProject = new Neo4JProject(project.getUuid(), project.getName());

			neo4JProject.setProjectRevision(project);

			org.neo4j.ogm.session.Session session = neo4JOGMSessionFactory.getSession();

			session.save(neo4JProject, 2); // limit save depth to 2 to avoid source document terms being saved

			session = neo4JOGMSessionFactory.getSession();

			Neo4JProject loaded = session.load(
					Neo4JProject.class, neo4JProject.getId(), 3
			);

			Project loadedProject = loaded.getProjectRevision(
					project.getRevisionHash()
			);

			assertEquals(projectUuid, loadedProject.getUuid());
			assertEquals(projectName, loadedProject.getName());

			assertEquals(1, loadedProject.getTagsets().size());
			assertEquals(1, loadedProject.getMarkupCollections().size());
			assertEquals(1, loadedProject.getSourceDocuments().size());

			assertEquals(tagsetDefinitionUuid, loadedProject.getTagsets().get(0).getUuid());

			assertEquals(userMarkupCollectionUuid, loadedProject.getMarkupCollections().get(0).getUuid());

			assertEquals(sourceDocumentUuid, loadedProject.getSourceDocuments().get(0).getID());
		}
	}
}
