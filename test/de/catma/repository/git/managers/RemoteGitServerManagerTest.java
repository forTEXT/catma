package de.catma.repository.git.managers;

import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import static org.awaitility.Awaitility.*;
import static org.junit.Assert.*;

public class RemoteGitServerManagerTest {
	private Properties catmaProperties;
	private RemoteGitServerManager serverManager;

	private IRemoteGitServerManager.CreateRepositoryResponse createRepositoryResponse = null;
	private String createdGroupPath = null;

	public RemoteGitServerManagerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
	}

	@Before
	public void setUp() throws Exception {
		this.serverManager = new RemoteGitServerManager(this.catmaProperties);
	}

	@After
	public void tearDown() throws Exception {
		if (this.createRepositoryResponse != null) {
			this.serverManager.deleteRepository(this.createRepositoryResponse.repositoryId);
			await().until(
				() -> this.serverManager.getGitLabApi().getProjectApi().getProjects().isEmpty()
			);

			if (this.createRepositoryResponse.groupPath != null) {
				this.serverManager.deleteGroup(this.createRepositoryResponse.groupPath);
				await().until(
					() -> this.serverManager.getGitLabApi().getGroupApi().getGroups().isEmpty()
				);
			}

			this.createRepositoryResponse = null;
		}

		if (this.createdGroupPath != null) {
			this.serverManager.deleteGroup(this.createdGroupPath);
			await().until(
				() -> this.serverManager.getGitLabApi().getGroupApi().getGroups().isEmpty()
			);
			this.createdGroupPath = null;
		}
	}

	@Test
	public void createRepository() throws Exception {
		this.createRepositoryResponse = this.serverManager.createRepository(
			"test-repo", null
		);

		assertNotNull(this.createRepositoryResponse);
		assert this.createRepositoryResponse.repositoryId > 0;
		assertNotNull(this.createRepositoryResponse.repositoryHttpUrl);
		assertNull(this.createRepositoryResponse.groupPath);

		Project project = this.serverManager.getGitLabApi().getProjectApi().getProject(
			this.createRepositoryResponse.repositoryId
		);

		assertNotNull(project);
		assertEquals("test-repo", project.getName());
	}

	@Test
	public void createRepositoryInGroup() throws Exception {
		this.createdGroupPath = this.serverManager.createGroup(
			"test-group", "test-group", null
		);

		assertNotNull(this.createdGroupPath);

		Group group = this.serverManager.getGitLabApi().getGroupApi().getGroup(
			this.createdGroupPath
		);

		assertNotNull(group);
		assertEquals("test-group", group.getName());
		assertEquals("test-group", group.getPath());

		this.createRepositoryResponse = this.serverManager.createRepository(
			"test-repo", null, this.createdGroupPath
		);

		assertNotNull(this.createRepositoryResponse);
		assert this.createRepositoryResponse.repositoryId > 0;

		Project project = this.serverManager.getGitLabApi().getProjectApi().getProject(
			this.createRepositoryResponse.repositoryId
		);

		assertNotNull(project);
		assertEquals("test-repo", project.getName());

		List<Project> repositoriesInGroup = this.serverManager.getGitLabApi().getGroupApi()
				.getProjects(group.getId());

		assertEquals(1, repositoriesInGroup.size());
		assertEquals(
			this.createRepositoryResponse.repositoryId, (long)repositoriesInGroup.get(0).getId()
		);

		// prevent tearDown from attempting to delete the group twice
		this.createdGroupPath = null;
	}

	@Test
	public void deleteRepository() throws Exception {
		this.createRepositoryResponse = this.serverManager.createRepository(
			"test-repo", null
		);

		assertNotNull(this.createRepositoryResponse);
		assert this.createRepositoryResponse.repositoryId > 0;

		this.serverManager.deleteRepository(this.createRepositoryResponse.repositoryId);

		await().until(
			() -> this.serverManager.getGitLabApi().getProjectApi().getProjects().isEmpty()
		);

		// prevent tearDown from also attempting to delete the repository
		this.createRepositoryResponse = null;
	}

	@Test
	public void createGroup() throws Exception {
		this.createdGroupPath = this.serverManager.createGroup(
			"test-group", "test-group", null
		);

		assertNotNull(this.createdGroupPath);

		Group group = this.serverManager.getGitLabApi().getGroupApi().getGroup(
			this.createdGroupPath
		);
		assertNotNull(group);
		assertEquals("test-group", group.getName());
	}

	@Test
	public void deleteGroup() throws Exception {
		this.createdGroupPath = this.serverManager.createGroup(
			"test-group", "test-group", null
		);

		assertNotNull(this.createdGroupPath);

		this.serverManager.deleteGroup(this.createdGroupPath);

		await().until(
			() -> this.serverManager.getGitLabApi().getGroupApi().getGroups().isEmpty()
		);

		// prevent tearDown from also attempting to delete the group
		this.createdGroupPath = null;
	}
}
