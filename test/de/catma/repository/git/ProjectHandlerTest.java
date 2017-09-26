package de.catma.repository.git;

import de.catma.repository.git.exceptions.ProjectHandlerException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.*;

public class ProjectHandlerTest {
	private Properties catmaProperties;
	private ProjectHandler projectHandler;

	private Integer createdGroupId = null;

	@Before
	public void setUp() throws IOException {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));

		this.projectHandler = new ProjectHandler(catmaProperties);
	}

	@After
	public void tearDown() throws ProjectHandlerException {
		if (createdGroupId != null) {
			projectHandler.delete(createdGroupId);
		}
	}

	@Test
	public void getRootRepositoryHttpUrl() throws ProjectHandlerException {
		createdGroupId = projectHandler.create(
			"Test Project", "This is a test project"
		);

		String repositoryHttpUrl = projectHandler.getRootRepositoryHttpUrl(createdGroupId);

		assertNotNull(repositoryHttpUrl);
		assert repositoryHttpUrl.length() > 0;
		assert repositoryHttpUrl.startsWith("http://");
		assert repositoryHttpUrl.endsWith(".git");
	}

	@Test
	public void create() throws ProjectHandlerException {
		createdGroupId = projectHandler.create(
			"Test Project", "This is a test project"
		);

		assert createdGroupId > 0;
	}
}
