package de.catma.repository.git;

import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.util.Properties;

import static org.awaitility.Awaitility.*;
import static org.junit.Assert.*;

public class RemoteGitServerManagerTest {
	private Properties catmaProperties;
	private RemoteGitServerManager serverManager;

	private Integer createdGroupId = null;
	private Integer createdRepositoryId = null;

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
		// delete repos first, otherwise the call fails if the repo was deleted as part of the group
		if (this.createdRepositoryId != null) {
			this.serverManager.deleteRepository(this.createdRepositoryId);
			await().until(
				() -> this.serverManager.getGitLabApi().getProjectApi().getProjects().isEmpty()
			);
		}
		if (this.createdGroupId != null) {
			this.serverManager.deleteGroup(this.createdGroupId);
			await().until(
				() -> this.serverManager.getGitLabApi().getGroupApi().getGroups().isEmpty()
			);
		}
	}

	@Test
	public void createRepository() throws Exception {
		this.createdRepositoryId = this.serverManager.createRepository(
			"test-repo", null
		);

		assertNotNull(this.createdRepositoryId);
		assert this.createdRepositoryId > 0;

		Project project = this.serverManager.getGitLabApi().getProjectApi().getProject(
			this.createdRepositoryId
		);

		assertNotNull(project);
		assertEquals("test-repo", project.getName());
	}

	@Test
	public void createRepositoryInGroup() throws Exception {
		this.createdGroupId = this.serverManager.createGroup(
			"test-group", "test-group", null
		);

		assertNotNull(this.createdGroupId);
		assert this.createdGroupId > 0;

		Group group = this.serverManager.getGitLabApi().getGroupApi().getGroup(this.createdGroupId);

		assertNotNull(group);
		assertEquals("test-group", group.getName());
		assertEquals("test-group", group.getPath());

		this.createdRepositoryId = this.serverManager.createRepository(
			"test-repo", null, this.createdGroupId
		);

		assertNotNull(this.createdRepositoryId);
		assert this.createdRepositoryId > 0;

		Project project = this.serverManager.getGitLabApi().getProjectApi().getProject(
			this.createdRepositoryId
		);

		assertNotNull(project);
		assertEquals("test-repo", project.getName());
	}

	@Test
	public void deleteRepository() throws Exception {
		this.createdRepositoryId = this.serverManager.createRepository(
			"test-repo", null
		);

		assertNotNull(this.createdRepositoryId);
		assert this.createdRepositoryId > 0;

		this.serverManager.deleteRepository(this.createdRepositoryId);

		await().until(
			() -> this.serverManager.getGitLabApi().getProjectApi().getProjects().isEmpty()
		);

		// prevent tearDown from also attempting to delete the repository
		this.createdRepositoryId = null;
	}

	@Test
	public void createGroup() throws Exception {
		this.createdGroupId = this.serverManager.createGroup(
			"test-group", "test-group", null
		);

		assertNotNull(this.createdGroupId);
		assert this.createdGroupId > 0;

		Group group = this.serverManager.getGitLabApi().getGroupApi().getGroup(this.createdGroupId);
		assertNotNull(group);
		assertEquals("test-group", group.getName());
	}

	@Test
	public void deleteGroup() throws Exception {
		this.createdGroupId = this.serverManager.createGroup(
			"test-group", "test-group", null
		);

		assertNotNull(this.createdGroupId);
		assert this.createdGroupId > 0;

		this.serverManager.deleteGroup(this.createdGroupId);

		await().until(
			() -> this.serverManager.getGitLabApi().getGroupApi().getGroups().isEmpty()
		);

		// prevent tearDown from also attempting to delete the group
		this.createdGroupId = null;
	}
}
