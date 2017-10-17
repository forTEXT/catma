package de.catma.repository.git;

import de.catma.repository.db.DBUser;
import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManagerTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

public class TagsetHandlerTest {
	private Properties catmaProperties;
	private RemoteGitServerManager remoteGitServerManager;
	private LocalGitRepositoryManager localGitRepositoryManager;
	private TagsetHandler tagsetHandler;

	private ArrayList<String> tagsetReposToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<String> projectsToDeleteOnTearDown = new ArrayList<>();

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
		de.catma.user.User catmaUser =new DBUser(
			1, String.format("catma-testuser-%s", RandomStringUtils.randomAlphanumeric(3)),
			false, false, false
		);

		this.remoteGitServerManager = new RemoteGitServerManager(this.catmaProperties, catmaUser);
		this.remoteGitServerManager.replaceGitLabServerUrl = true;

		this.localGitRepositoryManager = new LocalGitRepositoryManager(this.catmaProperties);

		this.tagsetHandler = new TagsetHandler(
			this.localGitRepositoryManager, this.remoteGitServerManager
		);
	}

	@After
	public void tearDown() throws Exception {

		if (this.tagsetReposToDeleteOnTearDown.size() > 0) {
			for (String tagsetId : this.tagsetReposToDeleteOnTearDown) {
				List<Project> projects = this.remoteGitServerManager.getAdminGitLabApi().getProjectApi().getProjects(
						tagsetId
				); // this getProjects overload does a search
				for (Project project : projects) {
					this.remoteGitServerManager.deleteRepository(project.getId());
				}
				await().until(
						() -> this.remoteGitServerManager.getAdminGitLabApi().getProjectApi().getProjects().isEmpty()
				);
			}
			this.tagsetReposToDeleteOnTearDown.clear();
		}

		if (this.projectsToDeleteOnTearDown.size() > 0) {
			try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
				ProjectHandler projectHandler = new ProjectHandler(localGitRepoManager, this.remoteGitServerManager);

				for (String projectId : this.projectsToDeleteOnTearDown) {
					projectHandler.delete(projectId);
				}
				this.projectsToDeleteOnTearDown.clear();
			}
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
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {

			ProjectHandler projectHandler = new ProjectHandler(
					localGitRepoManager, this.remoteGitServerManager
			);

			String projectId = projectHandler.create(
					"Test CATMA Project for Tagset", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);


			String tagsetId = tagsetHandler.create(
					"InterestingTagset",
					"Pretty interesting stuff",
					projectId);

			assertNotNull(tagsetId);
			assert tagsetId.startsWith("CATMA_");

			// Why don't we need to do this?
			// what is clearing the git folder?
			// this.tagsetReposToDeleteOnTearDown.add(tagsetId);

			File expectedRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), tagsetId);
			assert expectedRepoPath.exists();
			assert expectedRepoPath.isDirectory();

			assert Arrays.asList(expectedRepoPath.list()).contains("header.json");

			String expectedSerializedHeader = "Serialized properties";

			assertEquals(
					expectedSerializedHeader.replaceAll("[\n\t]", ""),
					FileUtils.readFileToString(new File(expectedRepoPath, "header.json"), StandardCharsets.UTF_8)
			);
		}
	}

	@Test
	public void delete() throws Exception {
	}

}
