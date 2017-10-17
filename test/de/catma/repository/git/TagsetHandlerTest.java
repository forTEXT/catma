package de.catma.repository.git;

import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManagerTest;
import helpers.Randomizer;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import static org.junit.Assert.*;

public class TagsetHandlerTest {
	private Properties catmaProperties;
	private RemoteGitServerManager remoteGitServerManager;
	private LocalGitRepositoryManager localGitRepositoryManager;
	private TagsetHandler tagsetHandler;

	private File createdRepositoryPath = null;

	public TagsetHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
	}

	@Before
	public void setUp() throws Exception {
		// create a fake CATMA user which we'll use to instantiate the RemoteGitServerManager
		de.catma.user.User catmaUser = Randomizer.getDbUser();

		this.remoteGitServerManager = new RemoteGitServerManager(this.catmaProperties, catmaUser);
		this.remoteGitServerManager.replaceGitLabServerUrl = true;

		this.localGitRepositoryManager = new LocalGitRepositoryManager(this.catmaProperties);

		this.tagsetHandler = new TagsetHandler(
			this.localGitRepositoryManager, this.remoteGitServerManager
		);
	}

	@After
	public void tearDown() throws Exception {

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
	}

	@Test
	public void delete() throws Exception {
	}

}
