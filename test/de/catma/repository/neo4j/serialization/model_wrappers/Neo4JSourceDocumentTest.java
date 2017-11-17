package de.catma.repository.neo4j.serialization.model_wrappers;

import de.catma.document.source.*;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.repository.neo4j.managers.Neo4JOGMSessionFactory;
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

public class Neo4JSourceDocumentTest {
	private Properties catmaProperties;

	private IDGenerator idGenerator;

	public Neo4JSourceDocumentTest() throws Exception {
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
	public void ogmInsertSourceDocument() throws Exception {
		try (Neo4JOGMSessionFactory neo4JOGMSessionFactory = new Neo4JOGMSessionFactory(this.catmaProperties)) {
			File convertedSourceDocument = new File("testdocs/rose_for_emily.txt");

			FileInputStream convertedSourceDocumentStream = new FileInputStream(convertedSourceDocument);

			IndexInfoSet indexInfoSet = new IndexInfoSet();
			indexInfoSet.setLocale(Locale.ENGLISH);

			ContentInfoSet contentInfoSet = new ContentInfoSet(
					"William Faulkner",
					"",
					"",
					"A Rose for Emily"
			);

			TechInfoSet techInfoSet = new TechInfoSet(
					FileType.TEXT,
					StandardCharsets.UTF_8,
					FileOSType.DOS,
					705211438L,
					null
			);

			SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo(
					indexInfoSet, contentInfoSet, techInfoSet
			);

			StandardContentHandler standardContentHandler = new StandardContentHandler();
			standardContentHandler.setSourceDocumentInfo(sourceDocumentInfo);
			standardContentHandler.load(convertedSourceDocumentStream);

			String sourceDocumentUuid = this.idGenerator.generate();
			SourceDocument sourceDocument = new SourceDocument(null, sourceDocumentUuid, standardContentHandler);
			sourceDocument.setRevisionHash("ABC123XYZ");

			Neo4JSourceDocument neo4JSourceDocument = new Neo4JSourceDocument(
					sourceDocumentUuid, contentInfoSet.getTitle()
			);

			neo4JSourceDocument.addSourceDocumentWorktree(sourceDocument);

			org.neo4j.ogm.session.Session session = neo4JOGMSessionFactory.getSession();

			session.save(neo4JSourceDocument);

			session = neo4JOGMSessionFactory.getSession();

			Neo4JSourceDocument loaded = session.load(
					Neo4JSourceDocument.class, sourceDocument.getUuid(), 4
			);
			SourceDocument loadedSourceDocument = loaded.getSourceDocumentWorktree(
					sourceDocument.getRevisionHash()
			);

			assertEquals(sourceDocumentUuid, loadedSourceDocument.getUuid());

			SourceDocumentInfo loadedSourceDocumentInfo = loadedSourceDocument.getSourceContentHandler()
					.getSourceDocumentInfo();

			assertEquals(
					indexInfoSet.getLocale().toLanguageTag(),
					loadedSourceDocumentInfo.getIndexInfoSet().getLocale().toLanguageTag()
			);

			assertEquals(contentInfoSet.getTitle(), loadedSourceDocumentInfo.getContentInfoSet().getTitle());
			assertEquals(contentInfoSet.getAuthor(), loadedSourceDocumentInfo.getContentInfoSet().getAuthor());

			assertEquals(techInfoSet.getFileType(), loadedSourceDocumentInfo.getTechInfoSet().getFileType());
			assertEquals(techInfoSet.getCharset(), loadedSourceDocumentInfo.getTechInfoSet().getCharset());
			assertEquals(techInfoSet.getFileOSType(), loadedSourceDocumentInfo.getTechInfoSet().getFileOSType());
			assertEquals(techInfoSet.getChecksum(), loadedSourceDocumentInfo.getTechInfoSet().getChecksum());
		}
	}
}
