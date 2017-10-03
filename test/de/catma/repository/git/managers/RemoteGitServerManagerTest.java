package de.catma.repository.git.managers;

import de.catma.repository.db.DBUser;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.managers.gitlab4j_api_custom.CustomUserApi;
import de.catma.repository.git.managers.gitlab4j_api_custom.models.ImpersonationToken;
import org.gitlab4j.api.*;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.awaitility.Awaitility.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class RemoteGitServerManagerTest {
	private Properties catmaProperties;
	private de.catma.user.User catmaUser;
	private RemoteGitServerManager serverManager;

	private IRemoteGitServerManager.CreateRepositoryResponse createRepositoryResponse = null;
	private String createdGroupPath = null;
	private Integer createdUserId = null;

	public RemoteGitServerManagerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
	}

	@Before
	public void setUp() throws Exception {
		// create a fake CATMA user which we'll use to instantiate the RemoteGitServerManager
		this.catmaUser = new DBUser(
			1, "catma-testuser", false, false, false
		);

		this.serverManager = new RemoteGitServerManager(this.catmaProperties, this.catmaUser);
	}

	@After
	public void tearDown() throws Exception {
		GitLabApi adminGitLabApi = this.serverManager.getAdminGitLabApi();
		GroupApi groupApi = adminGitLabApi.getGroupApi();
		ProjectApi projectApi = adminGitLabApi.getProjectApi();
		UserApi userApi = adminGitLabApi.getUserApi();

		// delete any GitLab resources created by test methods
		if (this.createRepositoryResponse != null) {
			if (this.createRepositoryResponse.groupPath != null) {
				// if the repo referenced by this.createRepositoryResponse was created within a
				// group, we can just delete the group (and any associated repos will be deleted
				// too) ...
				this.serverManager.deleteGroup(this.createRepositoryResponse.groupPath);
				await().until(() -> groupApi.getGroups().isEmpty());
			}
			else {
				// ... otherwise we must delete the repo
				this.serverManager.deleteRepository(this.createRepositoryResponse.repositoryId);
				await().until(() -> projectApi.getProjects().isEmpty());
			}

			this.createRepositoryResponse = null;
		}

		// group created separately from any repo
		if (this.createdGroupPath != null) {
			this.serverManager.deleteGroup(this.createdGroupPath);
			await().until(() -> groupApi.getGroups().isEmpty());
			this.createdGroupPath = null;
		}

		// not the same user as would have been created by the RemoteGitServerManager constructor,
		// see below
		if (this.createdUserId != null) {
			userApi.deleteUser(this.createdUserId);
			RemoteGitServerManagerTest.awaitUserDeleted(userApi, this.createdUserId);
		}

		// delete the GitLab user that the RemoteGitServerManager constructor in setUp would have
		// created
		// we do this last because GitLab seems to ignore deleteUser calls if the user still has
		// contributions (groups or repos), but that's probably because gitlab4j doesn't pass the
		// "hard_delete" parameter (which *is* safer, but it would be nice to have control over it)
		// the fact that the UI only shows the hard delete option for users that still have
		// contributions confirms this theory, as does the documentation at
		// https://docs.gitlab.com/ee/user/profile/account/delete_account.html#associated-records
		User user = this.serverManager.getGitLabUser();
		userApi.deleteUser(user.getId());
		RemoteGitServerManagerTest.awaitUserDeleted(userApi, user.getId());
	}

	private static void awaitUserDeleted(UserApi userApi, int userId) {
		await().until(() -> {
			try {
				userApi.getUser(userId);
				return false;
			}
			catch (GitLabApiException e) {
				return true;
			}
		});
	}

	@Test
	public void testInstantiationCreatesGitLabUser() throws Exception {
		UserApi userApi = this.serverManager.getAdminGitLabApi().getUserApi();
		List<User> users = userApi.getUsers();

		// we should have an admin user, the "ghost" user & one representing the CATMA user
		assertEquals(3, users.size());

		// hamcrest's hasItem(T item) matcher is not behaving as documented and is expecting the
		// users collection to contain *only* this.serverManager.getGitLabUser()
//		assertThat(users, hasItem(this.serverManager.getGitLabUser()));

		User matchedUser = null;
		for (User user : users) {
			if (user.getId().equals(this.serverManager.getGitLabUser().getId())) {
				matchedUser = user;
				break;
			}
		}

		assertNotNull(matchedUser);
		assertEquals(this.catmaUser.getIdentifier(), matchedUser.getUsername());
		assertEquals(this.catmaUser.getName(), matchedUser.getName());
		String expectedGitLabUserEmailAddress = String.format(
			RemoteGitServerManager.GITLAB_USER_EMAIL_ADDRESS_FORMAT, this.catmaUser.getUserId()
		);
		assertEquals(expectedGitLabUserEmailAddress, matchedUser.getEmail());

		// assert that the user has the expected impersonation token
		CustomUserApi customUserApi = new CustomUserApi(this.serverManager.getAdminGitLabApi());
		List<ImpersonationToken> impersonationTokens = customUserApi.getImpersonationTokens(
			this.serverManager.getGitLabUser().getId(), null
		);

		assertEquals(1, impersonationTokens.size());
		assertEquals(RemoteGitServerManager.GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME,
			impersonationTokens.get(0).name
		);

		// assert that re-instantiating the RemoteGitServerManager causes it to re-use the existing
		// GitLab user
		RemoteGitServerManager tmpServerManager = new RemoteGitServerManager(
			this.catmaProperties, this.catmaUser
		);

		users = userApi.getUsers();

		// we should *still* have an admin user, the "ghost" user & one representing the CATMA user
		assertEquals(3, users.size());
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

		Project project = this.serverManager.getAdminGitLabApi().getProjectApi().getProject(
			this.createRepositoryResponse.repositoryId
		);

		assertNotNull(project);
		assertEquals("test-repo", project.getName());
		assertEquals(this.serverManager.getGitLabUser().getId(), project.getOwner().getId());
	}

	@Test
	public void createRepositoryInGroup() throws Exception {
		this.createdGroupPath = this.serverManager.createGroup(
			"test-group", "test-group", null
		);

		assertNotNull(this.createdGroupPath);

		Group group = this.serverManager.getAdminGitLabApi().getGroupApi().getGroup(
			this.createdGroupPath
		);

		assertNotNull(group);
		assertEquals("test-group", group.getName());
		assertEquals("test-group", group.getPath());

		// to assert that the user is the owner of the new group, get the groups for the user using
		// the *user-specific* GitLabApi instance
		List<Group> groups = this.serverManager.getUserGitLabApi().getGroupApi().getGroups();

		assertEquals(1, groups.size());
		assertEquals(group.getId(), groups.get(0).getId());

		this.createRepositoryResponse = this.serverManager.createRepository(
			"test-repo", null, this.createdGroupPath
		);

		assertNotNull(this.createRepositoryResponse);
		assert this.createRepositoryResponse.repositoryId > 0;

		Project project = this.serverManager.getAdminGitLabApi().getProjectApi().getProject(
			this.createRepositoryResponse.repositoryId
		);

		assertNotNull(project);
		assertEquals("test-repo", project.getName());
		assertEquals(this.serverManager.getGitLabUser().getId(), project.getCreatorId());

		List<Project> repositoriesInGroup = this.serverManager.getAdminGitLabApi().getGroupApi()
				.getProjects(group.getId());

		assertEquals(1, repositoriesInGroup.size());
		assertEquals(
			this.createRepositoryResponse.repositoryId, (int)repositoriesInGroup.get(0).getId()
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
			() -> this.serverManager.getAdminGitLabApi().getProjectApi().getProjects().isEmpty()
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

		Group group = this.serverManager.getAdminGitLabApi().getGroupApi().getGroup(
			this.createdGroupPath
		);
		assertNotNull(group);
		assertEquals("test-group", group.getName());
		assertEquals("test-group", group.getPath());

		// to assert that the user is the owner of the new group, get the groups for the user using
		// the *user-specific* GitLabApi instance
		List<Group> groups = this.serverManager.getUserGitLabApi().getGroupApi().getGroups();

		assertEquals(1, groups.size());
		assertEquals(group.getId(), groups.get(0).getId());
	}

	@Test
	public void getGroupRepositoryNames() throws Exception {
		this.createdGroupPath = this.serverManager.createGroup(
			"test-group", "test-group", null
		);

		assertNotNull(this.createdGroupPath);

		this.createRepositoryResponse = this.serverManager.createRepository(
			"test-repo-1", null, this.createdGroupPath
		);

		assertNotNull(this.createRepositoryResponse);
		assert this.createRepositoryResponse.repositoryId > 0;

		this.createRepositoryResponse = this.serverManager.createRepository(
			"test-repo-2", null, this.createdGroupPath
		);

		assertNotNull(this.createRepositoryResponse);
		assert this.createRepositoryResponse.repositoryId > 0;

		List<String> repositoryNames = this.serverManager.getGroupRepositoryNames(
			this.createdGroupPath
		);
		repositoryNames.sort(null);

		List<String> expectedRepositoryNames = new ArrayList<String>(2);
		expectedRepositoryNames.add("test-repo-1");
		expectedRepositoryNames.add("test-repo-2");

		assertArrayEquals(expectedRepositoryNames.toArray(), repositoryNames.toArray());

		// prevent tearDown from attempting to delete the group twice
		this.createRepositoryResponse = null;
	}

	@Test
	public void deleteGroup() throws Exception {
		this.createdGroupPath = this.serverManager.createGroup(
			"test-group", "test-group", null
		);

		assertNotNull(this.createdGroupPath);

		this.serverManager.deleteGroup(this.createdGroupPath);

		await().until(
			() -> this.serverManager.getAdminGitLabApi().getGroupApi().getGroups().isEmpty()
		);

		// prevent tearDown from also attempting to delete the group
		this.createdGroupPath = null;
	}

	@Test
	public void createUser() throws Exception {
		this.createdUserId = this.serverManager.createUser(
			"testuser@catma.de", "testuser", null, "Test User",
			null
		);

		assertNotNull(this.createdUserId);
		assert this.createdUserId > 0;

		User user = this.serverManager.getAdminGitLabApi().getUserApi().getUser(this.createdUserId);
		assertNotNull(user);
		assertEquals("testuser@catma.de", user.getEmail());
		assertEquals("testuser", user.getUsername());
		assertEquals("Test User", user.getName());
//		assertFalse(user.getIsAdmin()); // seems to always return null
		assert user.getCanCreateGroup();
		assert user.getCanCreateProject();
		assertEquals("active", user.getState());
	}

	@Test
	public void createAdminUser() throws Exception {
		this.createdUserId = this.serverManager.createUser(
			"testadminuser@catma.de", "testadminuser", null,
			"Test AdminUser", true
		);

		assertNotNull(this.createdUserId);
		assert this.createdUserId > 0;

		User user = this.serverManager.getAdminGitLabApi().getUserApi().getUser(this.createdUserId);
		assertNotNull(user);
		assertEquals("testadminuser@catma.de", user.getEmail());
		assertEquals("testadminuser", user.getUsername());
		assertEquals("Test AdminUser", user.getName());
//		assert user.getIsAdmin(); // seems to always return null
		assert user.getCanCreateGroup();
		assert user.getCanCreateProject();
		assertEquals("active", user.getState());
	}
}
