package de.catma.repository.git.managers;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.common.eventbus.EventBus;
import de.catma.backgroundservice.BackgroundService;
import de.catma.properties.CATMAProperties;
import org.apache.commons.lang3.RandomStringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.UserApi;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.PersonalAccessToken;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.catma.repository.git.CreateRepositoryResponse;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;

public class GitLabServerManagerTest {
	private GitlabManagerPrivileged gitlabManagerPrivileged;
	private GitlabManagerRestricted gitlabManagerRestricted;

	private ArrayList<String> groupsToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<Integer> repositoriesToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<Integer> usersToDeleteOnTearDown = new ArrayList<>();

	public GitLabServerManagerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		Properties catmaProperties = new Properties();
		catmaProperties.load(new FileInputStream(propertiesFile));
		CATMAProperties.INSTANCE.setProperties(catmaProperties);
	}

	@BeforeEach
	public void setUp() throws Exception {
		// create a fake CATMA user which we'll use to instantiate GitlabManagerRestricted (using the corresponding impersonation token)
		Integer randomUserId = Integer.parseInt(RandomStringUtils.randomNumeric(3));
		String username = String.format("testuser-%s", randomUserId);
		String email = String.format("%s@catma.de", username);
		String name = String.format("Test User %s", randomUserId);

		gitlabManagerPrivileged = new GitlabManagerPrivileged();
		String impersonationToken = gitlabManagerPrivileged.acquireImpersonationToken(username, "catma", email, name).getSecond();

		EventBus mockEventBus = mock(EventBus.class);
		BackgroundService mockBackgroundService = mock(BackgroundService.class);
		gitlabManagerRestricted = new GitlabManagerRestricted(mockEventBus, mockBackgroundService, impersonationToken);
	}

	@AfterEach
	public void tearDown() throws Exception {
		GitLabApi adminGitLabApi = gitlabManagerPrivileged.getGitLabApi();
		GroupApi groupApi = adminGitLabApi.getGroupApi();
		ProjectApi projectApi = adminGitLabApi.getProjectApi();
		UserApi userApi = adminGitLabApi.getUserApi();

		if (groupsToDeleteOnTearDown.size() > 0) {
			for (String groupPath : groupsToDeleteOnTearDown) {
				gitlabManagerRestricted.deleteGroup(groupPath);
				await().until(() -> groupApi.getGroups().isEmpty());
			}
		}

		if (repositoriesToDeleteOnTearDown.size() > 0) {
			for (Integer repositoryId : repositoriesToDeleteOnTearDown) {
				gitlabManagerRestricted.deleteRepository(repositoryId);
				await().until(() -> projectApi.getProjects().isEmpty());
			}
		}

		if (usersToDeleteOnTearDown.size() > 0) {
			for (Integer userId : usersToDeleteOnTearDown) {
				userApi.deleteUser(userId);
				GitLabServerManagerTest.awaitUserDeleted(userApi, userId);
			}
		}

		// delete the GitLab user that we created in setUp, including associated groups/repos
		// TODO: explicit deletion of associated groups/repos (above) is now superfluous since we are doing a hard delete
		userApi.deleteUser(gitlabManagerRestricted.getUser().getUserId(), true);
//		GitLabServerManagerTest.awaitUserDeleted(userApi, gitlabManagerRestricted.getUser().getUserId());
	}

	public static void awaitUserDeleted(UserApi userApi, int userId) {
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
		UserApi userApi = gitlabManagerPrivileged.getGitLabApi().getUserApi();
		List<User> users = userApi.getUsers();

		// we should have an admin user, the "ghost" user & one representing the CATMA user
		assertEquals(3, users.size());

		// hamcrest's hasItem(T item) matcher is not behaving as documented and is expecting the
		// users collection to contain *only* this.serverManager.getGitLabUser()
//		assertThat(users, hasItem(this.serverManager.getGitLabUser()));

		User matchedUser = null;
		for (User user : users) {
			if (user.getId().equals(gitlabManagerRestricted.getUser().getUserId())) {
				matchedUser = user;
				break;
			}
		}

		assertNotNull(matchedUser);
		assertEquals(gitlabManagerRestricted.getUser().getIdentifier(), matchedUser.getUsername());
		assertEquals(gitlabManagerRestricted.getUser().getName(), matchedUser.getName());

		// assert that the user has the expected impersonation token
		List<PersonalAccessToken> impersonationTokens = userApi.getImpersonationTokens(gitlabManagerRestricted.getUser().getUserId());

		assertEquals(1, impersonationTokens.size());
		assertEquals(GitlabManagerPrivileged.GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME, impersonationTokens.get(0).getName());
	}

//	@Test
//	public void createRepository() throws Exception {
//		String randomRepoName = Randomizer.getRepoName();
//
//		CreateRepositoryResponse createRepositoryResponse = this.serverManager.createRepository(
//			randomRepoName, null
//		);
//		this.repositoriesToDeleteOnTearDown.add(createRepositoryResponse.repositoryId);
//
//		assertNotNull(createRepositoryResponse);
//		assert createRepositoryResponse.repositoryId > 0;
//		assertNotNull(createRepositoryResponse.repositoryHttpUrl);
//		assertNull(createRepositoryResponse.groupPath);
//
//		Project project = this.serverManager.getAdminGitLabApi().getProjectApi().getProject(
//			createRepositoryResponse.repositoryId
//		);
//
//		assertNotNull(project);
//		assertEquals(randomRepoName, project.getName());
//		assertEquals(this.serverManager.getGitLabUser().getId(), project.getOwner().getId());
//	}
//
//	@Test
//	public void createRepositoryInGroup() throws Exception {
//		String randomGroupNameAndPath = Randomizer.getGroupName();
//
//		String createdGroupPath = this.serverManager.createGroup(
//			randomGroupNameAndPath, randomGroupNameAndPath, null
//		);
//		this.groupsToDeleteOnTearDown.add(createdGroupPath);
//
//		assertNotNull(createdGroupPath);
//
//		Group group = this.serverManager.getAdminGitLabApi().getGroupApi().getGroup(createdGroupPath);
//
//		assertNotNull(group);
//		assertEquals(randomGroupNameAndPath, group.getName());
//		assertEquals(randomGroupNameAndPath, group.getPath());
//
//		// to assert that the user is the owner of the new group, get the groups for the user using
//		// the *user-specific* GitLabApi instance
//		List<Group> groups = this.serverManager.getUserGitLabApi().getGroupApi().getGroups();
//
//		assertEquals(1, groups.size());
//		assertEquals(group.getId(), groups.get(0).getId());
//
//		String randomRepoName = Randomizer.getRepoName();
//
//		CreateRepositoryResponse createRepositoryResponse = this.serverManager.createRepository(
//			randomRepoName, null, createdGroupPath
//		);
//		// we don't add the repositoryId to this.repositoriesToDeleteOnTearDown as deletion of the group will take care
//		// of that for us
//
//		assertNotNull(createRepositoryResponse);
//		assert createRepositoryResponse.repositoryId > 0;
//
//		Project project = this.serverManager.getAdminGitLabApi().getProjectApi().getProject(
//			createRepositoryResponse.repositoryId
//		);
//
//		assertNotNull(project);
//		assertEquals(randomRepoName, project.getName());
//		assertEquals(this.serverManager.getGitLabUser().getId(), project.getCreatorId());
//
//		List<Project> repositoriesInGroup = this.serverManager.getAdminGitLabApi().getGroupApi()
//				.getProjects(group.getId());
//
//		assertEquals(1, repositoriesInGroup.size());
//		assertEquals(
//			createRepositoryResponse.repositoryId, (int)repositoriesInGroup.get(0).getId()
//		);
//	}
//
//	@Test
//	public void deleteRepository() throws Exception {
//		CreateRepositoryResponse createRepositoryResponse = this.serverManager.createRepository(
//			Randomizer.getRepoName(), null
//		);
//		// we don't add the repositoryId to this.repositoriesToDeleteOnTearDown as this is the delete test
//
//		assertNotNull(createRepositoryResponse);
//		assert createRepositoryResponse.repositoryId > 0;
//
//		this.serverManager.deleteRepository(createRepositoryResponse.repositoryId);
//
//		await().until(
//			() -> this.serverManager.getAdminGitLabApi().getProjectApi().getProjects().isEmpty()
//		);
//	}
//
//	@Test
//	public void createGroup() throws Exception {
//		String randomGroupNameAndPath = Randomizer.getGroupName();
//
//		String createdGroupPath = this.serverManager.createGroup(
//			randomGroupNameAndPath, randomGroupNameAndPath, null
//		);
//		this.groupsToDeleteOnTearDown.add(createdGroupPath);
//
//		assertNotNull(createdGroupPath);
//
//		Group group = this.serverManager.getAdminGitLabApi().getGroupApi().getGroup(createdGroupPath);
//		assertNotNull(group);
//		assertEquals(randomGroupNameAndPath, group.getName());
//		assertEquals(randomGroupNameAndPath, group.getPath());
//
//		// to assert that the user is the owner of the new group, get the groups for the user using
//		// the *user-specific* GitLabApi instance
//		List<Group> groups = this.serverManager.getUserGitLabApi().getGroupApi().getGroups();
//
//		assertEquals(1, groups.size());
//		assertEquals(group.getId(), groups.get(0).getId());
//	}
//
//	@Test
//	public void getGroupRepositoryNames() throws Exception {
//		String randomGroupNameAndPath = Randomizer.getGroupName();
//
//		String createdGroupPath = this.serverManager.createGroup(
//			randomGroupNameAndPath, randomGroupNameAndPath, null
//		);
//		this.groupsToDeleteOnTearDown.add(createdGroupPath);
//
//		assertNotNull(createdGroupPath);
//
//		String randomRepoName1 = Randomizer.getRepoName();
//		CreateRepositoryResponse createRepositoryResponse = this.serverManager.createRepository(
//			randomRepoName1, null, createdGroupPath
//		);
//		// we don't add the repositoryId to this.repositoriesToDeleteOnTearDown as deletion of the group will take care
//		// of that for us
//
//		assertNotNull(createRepositoryResponse);
//		assert createRepositoryResponse.repositoryId > 0;
//
//		String randomRepoName2 = Randomizer.getRepoName();
//		createRepositoryResponse = this.serverManager.createRepository(
//			randomRepoName2, null, createdGroupPath
//		);
//		// we don't add the repositoryId to this.repositoriesToDeleteOnTearDown as deletion of the group will take care
//		// of that for us
//
//		assertNotNull(createRepositoryResponse);
//		assert createRepositoryResponse.repositoryId > 0;
//
//		List<String> repositoryNames = this.serverManager.getGroupRepositoryNames(createdGroupPath);
//		repositoryNames.sort(null);
//
//		List<String> expectedRepositoryNames = new ArrayList<String>(2);
//		expectedRepositoryNames.add(randomRepoName1);
//		expectedRepositoryNames.add(randomRepoName2);
//		expectedRepositoryNames.sort(null);
//
//		assertArrayEquals(expectedRepositoryNames.toArray(), repositoryNames.toArray());
//	}
//
//	@Test
//	public void deleteGroup() throws Exception {
//		String randomGroupNameAndPath = Randomizer.getGroupName();
//
//		String createdGroupPath = this.serverManager.createGroup(
//			randomGroupNameAndPath, randomGroupNameAndPath, null
//		);
//		// we don't add the groupPath to this.groupsToDeleteOnTearDown as this is the delete test
//
//		assertNotNull(createdGroupPath);
//
//		this.serverManager.deleteGroup(createdGroupPath);
//
//		await().until(
//			() -> this.serverManager.getAdminGitLabApi().getGroupApi().getGroups().isEmpty()
//		);
//	}
//
//	@Test
//	public void createUser() throws Exception {
//		Integer createdUserId = this.serverManager.createUser(
//			"testuser@catma.de", "testuser", null, "Test User",
//			null
//		);
//		this.usersToDeleteOnTearDown.add(createdUserId);
//
//		assertNotNull(createdUserId);
//		assert createdUserId > 0;
//
//		User user = this.serverManager.getAdminGitLabApi().getUserApi().getUser(createdUserId);
//		assertNotNull(user);
//		assertEquals("testuser@catma.de", user.getEmail());
//		assertEquals("testuser", user.getUsername());
//		assertEquals("Test User", user.getName());
////		assertFalse(user.getIsAdmin()); // seems to always return null
//		assert user.getCanCreateGroup();
//		assert user.getCanCreateProject();
//		assertEquals("active", user.getState());
//	}
//
//	@Test
//	public void createAdminUser() throws Exception {
//		Integer createdUserId = this.serverManager.createUser(
//			"testadminuser@catma.de", "testadminuser", null,
//			"Test AdminUser", true
//		);
//		this.usersToDeleteOnTearDown.add(createdUserId);
//
//		assertNotNull(createdUserId);
//		assert createdUserId > 0;
//
//		User user = this.serverManager.getAdminGitLabApi().getUserApi().getUser(createdUserId);
//		assertNotNull(user);
//		assertEquals("testadminuser@catma.de", user.getEmail());
//		assertEquals("testadminuser", user.getUsername());
//		assertEquals("Test AdminUser", user.getName());
////		assert user.getIsAdmin(); // seems to always return null
//		assert user.getCanCreateGroup();
//		assert user.getCanCreateProject();
//		assertEquals("active", user.getState());
//	}
}
