package de.catma.repository.git;

import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import static org.junit.Assert.*;

public class ProjectHandlerTest {
	private Properties catmaProperties;
	private ProjectHandler projectHandler;

	private String createdProjectId = null;

	public ProjectHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
	}

	@Before
	public void setUp() throws Exception {
		RemoteGitServerManager remoteGitServerManager = new RemoteGitServerManager(
			this.catmaProperties
		);
		remoteGitServerManager.replaceGitLabServerUrl = true;

		this.projectHandler = new ProjectHandler(
			new LocalGitRepositoryManager(this.catmaProperties),
			remoteGitServerManager
		);
	}

	@After
	public void tearDown() throws Exception {
		if (this.createdProjectId != null) {
			this.projectHandler.delete(this.createdProjectId);
		}
	}

//	@Test
//	public void getRootRepositoryHttpUrl() throws Exception {
//		createdGroupId = projectHandler.create(
//			"Test Project", "This is a test project"
//		);
//
//		String repositoryHttpUrl = projectHandler.getRootRepositoryHttpUrl(createdGroupId);
//
//		assertNotNull(repositoryHttpUrl);
//		assert repositoryHttpUrl.length() > 0;
//		assert repositoryHttpUrl.startsWith("http://");
//		assert repositoryHttpUrl.endsWith(".git");
//	}

	@Test
	public void create() throws Exception {
		this.createdProjectId = this.projectHandler.create(
			"Test CATMA Project", "This is a test CATMA project"
		);

		assertNotNull(this.createdProjectId);

		String expectedRootRepositoryName = String.format(
			this.projectHandler.projectRootRepositoryNameFormat, this.createdProjectId
		);
		String repositoryBasePath = catmaProperties.getProperty("GitBasedRepositoryBasePath");

		File expectedRootRepositoryPath = new File(repositoryBasePath, expectedRootRepositoryName);

		assert expectedRootRepositoryPath.exists();
		assert expectedRootRepositoryPath.isDirectory();

		// cleanup
		// TODO: move to tearDown?
		FileUtils.deleteDirectory(expectedRootRepositoryPath);
	}
}
