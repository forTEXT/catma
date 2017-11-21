package de.catma.repository.neo4j.model_wrappers;

import de.catma.models.Project;
import de.catma.repository.neo4j.Neo4JOGMSessionFactory;
import de.catma.util.IDGenerator;
import helpers.Randomizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.session.Session;

import java.io.FileInputStream;
import java.util.Properties;

import static org.junit.Assert.*;

public class Neo4JUserTest {
	private Properties catmaProperties;

	private IDGenerator idGenerator;

	public Neo4JUserTest() throws Exception {
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
	public void insertUser() throws Exception {
		try (Neo4JOGMSessionFactory neo4JOGMSessionFactory = new Neo4JOGMSessionFactory(this.catmaProperties)) {
			de.catma.user.User user = Randomizer.getDbUser();
			String userIdentifier = user.getIdentifier();

			Neo4JUser neo4JUser = new Neo4JUser(user);

			String projectUuid = this.idGenerator.generate();
			String projectName = "Test Project";
			String projectRevisionHash = "ABC123XYZ";

			Project project = new Project(
					projectUuid, projectName, "Test Project Description", projectRevisionHash
			);

			neo4JUser.setProject(project);

			org.neo4j.ogm.session.Session session = neo4JOGMSessionFactory.getSession();

			session.save(neo4JUser);

			session = neo4JOGMSessionFactory.getSession();

			Neo4JUser loaded = session.load(
					Neo4JUser.class, neo4JUser.getId(), 2
			);

			assertEquals(userIdentifier, loaded.getIdentifier());

			Project loadedProject = loaded.getProject(projectUuid, projectRevisionHash);

			assertEquals(projectName, loadedProject.getName());
		}
	}
}
