package de.catma.repository.git;

import de.catma.repository.db.DBUser;
import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManagerTest;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import static org.junit.Assert.*;

public class ProjectHandlerTest {
	private Properties catmaProperties;
	private RemoteGitServerManager remoteGitServerManager;
	private LocalGitRepositoryManager localGitRepositoryManager;
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
		// create a fake CATMA user which we'll use to instantiate the RemoteGitServerManager
		de.catma.user.User catmaUser = new DBUser(
			1, "catma-testuser", false, false, false
		);

		this.remoteGitServerManager = new RemoteGitServerManager(
			this.catmaProperties, catmaUser
		);
		this.remoteGitServerManager.replaceGitLabServerUrl = true;

		this.localGitRepositoryManager = new LocalGitRepositoryManager(this.catmaProperties);

		this.projectHandler = new ProjectHandler(
			this.localGitRepositoryManager,
			this.remoteGitServerManager
		);
	}

	@After
	public void tearDown() throws Exception {
		if (this.createdProjectId != null) {
			this.projectHandler.delete(this.createdProjectId);
			this.createdProjectId = null;
		}

		if (this.localGitRepositoryManager != null) {
			this.localGitRepositoryManager.close();
			this.localGitRepositoryManager = null;
		}

		// delete the GitLab user that the RemoteGitServerManager constructor in setUp would have
		// created - see RemoteGitServerManagerTest tearDown() for more info
		User user = this.remoteGitServerManager.getGitLabUser();
		this.remoteGitServerManager.getAdminGitLabApi().getUserApi().deleteUser(user.getId());
		RemoteGitServerManagerTest.awaitUserDeleted(
			this.remoteGitServerManager.getAdminGitLabApi().getUserApi(), user.getId()
		);
	}

	@Test
	public void create() throws Exception {
		this.createdProjectId = this.projectHandler.create(
			"Test CATMA Project", "This is a test CATMA project"
		);

		assertNotNull(this.createdProjectId);
		assert this.createdProjectId.startsWith("CATMA_");

		String expectedRootRepositoryName = String.format(
			ProjectHandler.PROJECT_ROOT_REPOSITORY_NAME_FORMAT, this.createdProjectId
		);
		String repositoryBasePath = catmaProperties.getProperty("GitBasedRepositoryBasePath");

		File expectedRootRepositoryPath = new File(repositoryBasePath, expectedRootRepositoryName);

		assert expectedRootRepositoryPath.exists();
		assert expectedRootRepositoryPath.isDirectory();
	}

	@Test
	public void delete() throws Exception {
		this.createdProjectId = this.projectHandler.create(
			"Test CATMA Project", "This is a test CATMA project"
		);

		assertNotNull(this.createdProjectId);
		assert this.createdProjectId.startsWith("CATMA_");

		String expectedRootRepositoryName = String.format(
			ProjectHandler.PROJECT_ROOT_REPOSITORY_NAME_FORMAT, this.createdProjectId
		);
		String repositoryBasePath = catmaProperties.getProperty("GitBasedRepositoryBasePath");

		File expectedRootRepositoryPath = new File(repositoryBasePath, expectedRootRepositoryName);

		assert expectedRootRepositoryPath.exists();
		assert expectedRootRepositoryPath.isDirectory();

		this.projectHandler.delete(this.createdProjectId);

		assertFalse(expectedRootRepositoryPath.exists());

		// prevent tearDown from also attempting to delete the project
		this.createdProjectId = null;
	}
}
