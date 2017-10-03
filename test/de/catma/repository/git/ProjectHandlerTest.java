package de.catma.repository.git;

import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import static org.junit.Assert.*;

public class ProjectHandlerTest {
	private Properties catmaProperties;
	private LocalGitRepositoryManager localGitRepositoryManager;
	private ProjectHandler projectHandler;

	private String createdProjectId = null;

	public ProjectHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
	}

//	@Before
//	public void setUp() throws Exception {
//		RemoteGitServerManager remoteGitServerManager = new RemoteGitServerManager(
//			this.catmaProperties
//		);
//		remoteGitServerManager.replaceGitLabServerUrl = true;
//
//		this.localGitRepositoryManager = new LocalGitRepositoryManager(this.catmaProperties);
//
//		this.projectHandler = new ProjectHandler(
//			this.localGitRepositoryManager,
//			remoteGitServerManager
//		);
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		if (this.createdProjectId != null) {
//			this.projectHandler.delete(this.createdProjectId);
//			this.createdProjectId = null;
//		}
//
//		if (this.localGitRepositoryManager != null) {
//			this.localGitRepositoryManager.close();
//			this.localGitRepositoryManager = null;
//		}
//	}
//
//	@Test
//	public void create() throws Exception {
//		this.createdProjectId = this.projectHandler.create(
//			"Test CATMA Project", "This is a test CATMA project"
//		);
//
//		assertNotNull(this.createdProjectId);
//		assert this.createdProjectId.startsWith("CATMA_");
//
//		String expectedRootRepositoryName = String.format(
//			ProjectHandler.PROJECT_ROOT_REPOSITORY_NAME_FORMAT, this.createdProjectId
//		);
//		String repositoryBasePath = catmaProperties.getProperty("GitBasedRepositoryBasePath");
//
//		File expectedRootRepositoryPath = new File(repositoryBasePath, expectedRootRepositoryName);
//
//		assert expectedRootRepositoryPath.exists();
//		assert expectedRootRepositoryPath.isDirectory();
//	}
//
//	@Test
//	public void delete() throws Exception {
//		this.createdProjectId = this.projectHandler.create(
//			"Test CATMA Project", "This is a test CATMA project"
//		);
//
//		assertNotNull(this.createdProjectId);
//		assert this.createdProjectId.startsWith("CATMA_");
//
//		String expectedRootRepositoryName = String.format(
//			ProjectHandler.PROJECT_ROOT_REPOSITORY_NAME_FORMAT, this.createdProjectId
//		);
//		String repositoryBasePath = catmaProperties.getProperty("GitBasedRepositoryBasePath");
//
//		File expectedRootRepositoryPath = new File(repositoryBasePath, expectedRootRepositoryName);
//
//		assert expectedRootRepositoryPath.exists();
//		assert expectedRootRepositoryPath.isDirectory();
//
//		this.projectHandler.delete(this.createdProjectId);
//
//		assertFalse(expectedRootRepositoryPath.exists());
//
//		// prevent tearDown from also attempting to delete the project
//		this.createdProjectId = null;
//	}
}
