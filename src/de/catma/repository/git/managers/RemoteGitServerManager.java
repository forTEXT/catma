package de.catma.repository.git.managers;

import de.catma.repository.git.managers.gitlab4j_api_custom.models.ImpersonationToken;
import de.catma.repository.git.managers.gitlab4j_api_custom.CustomUserApi;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.util.Pair;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.*;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Namespace;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class RemoteGitServerManager implements IRemoteGitServerManager {
	private final String gitLabAdminPersonalAccessToken;
	private final String gitLabServerUrl;

	private final GitLabApi adminGitLabApi;

	public GitLabApi getAdminGitLabApi() {
		return this.adminGitLabApi;
	}

	private final User gitLabUser;

	public User getGitLabUser() {
		return this.gitLabUser;
	}

	private final String gitLabUserImpersonationToken;

	public String getGitLabUserImpersonationToken() {
		return this.gitLabUserImpersonationToken;
	}

	private final GitLabApi userGitLabApi;

	public GitLabApi getUserGitLabApi() {
		return this.userGitLabApi;
	}

	static final String GITLAB_USER_EMAIL_ADDRESS_FORMAT = "catma-user-%s@catma.de";
	static final String GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME = "catma-default-ipt";

	// <for testing purposes only
	public boolean replaceGitLabServerUrl = false;

	private String checkGitLabServerUrl(String url) {
		if (!this.replaceGitLabServerUrl) {
			return url;
		}

		try {
			URL currentUrl = new URL(url);
			URL gitLabServerUrl = new URL(this.gitLabServerUrl);
			URL newUrl = new URL(
				gitLabServerUrl.getProtocol(), gitLabServerUrl.getHost(), gitLabServerUrl.getPort(),
				currentUrl.getFile()
			);
			return newUrl.toString();
		}
		catch (IOException e) {
			return null;
		}
	}
	// />

	private static final char[] PWD_CHARS = (
		"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
		"0123456789~`!@#$%^&*()-_=+[{]}\\|;:\'\",<.>/?"
	).toCharArray();

	/**
	 * Creates a new RemoteGitServerManager instance.
	 * <p>
	 * Unless stated otherwise, all actions are performed as the <code>catmaUser</code> supplied
	 * (technically, as a GitLab user that represents the CATMA user).
	 *
	 * @param catmaProperties a {@link Properties} object
	 * @param catmaUser a {@link de.catma.user.User} object
	 * @throws RemoteGitServerManagerException if something went wrong during instantiation
	 */
	public RemoteGitServerManager(Properties catmaProperties, de.catma.user.User catmaUser)
			throws RemoteGitServerManagerException {
		this.gitLabAdminPersonalAccessToken = catmaProperties.getProperty(
			"GitLabAdminPersonalAccessToken"
		);
		this.gitLabServerUrl = catmaProperties.getProperty("GitLabServerUrl");

		this.adminGitLabApi = new GitLabApi(
			this.gitLabServerUrl, this.gitLabAdminPersonalAccessToken
		);

		Pair<User, String> userRawTokenPair = this.acquireImpersonationToken(catmaUser);
		this.gitLabUser = userRawTokenPair.getFirst();
		this.gitLabUserImpersonationToken = userRawTokenPair.getSecond();

		this.userGitLabApi = new GitLabApi(this.gitLabServerUrl, this.gitLabUserImpersonationToken);
	}

	/**
	 * Creates a new remote repository with the <code>name</code> and <code>path</code> supplied.
	 *
	 * @param name the name of the repository to create
	 * @param path the path of the repository to create
	 * @return a {@link CreateRepositoryResponse} object containing the repository ID and HTTP URL
	 * @throws RemoteGitServerManagerException if something went wrong while creating the remote
	 *         repository
	 */
	@Override
	public CreateRepositoryResponse createRepository(String name, @Nullable String path)
			throws RemoteGitServerManagerException {
		ProjectApi projectApi = this.userGitLabApi.getProjectApi();

		Project project = new Project();
		project.setName(name);

		if (StringUtils.isNotEmpty(path)) {
			project.setPath(path);
		}

		try {
			project = projectApi.createProject(project);
			return new CreateRepositoryResponse(
				null, project.getId(),
				this.checkGitLabServerUrl(project.getHttpUrlToRepo())
			);
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to create remote Git repository", e);
		}
	}

	/**
	 * Creates a new remote repository with the <code>name</code> and <code>path</code> supplied
	 * within the group specified by the <code>groupId</code>.
	 *
	 * @param name the name of the repository to create
	 * @param path the path of the repository to create
	 * @param groupPath the path of the group within which to create the repository
	 * @return a {@link CreateRepositoryResponse} object containing the repository ID and HTTP URL
	 * @throws RemoteGitServerManagerException if something went wrong while creating the remote
	 *         repository
	 */
	@Override
	public CreateRepositoryResponse createRepository(
			String name, @Nullable String path, String groupPath)
			throws RemoteGitServerManagerException {
		GroupApi groupApi = this.userGitLabApi.getGroupApi();
		ProjectApi projectApi = this.userGitLabApi.getProjectApi();

		try {
			Group group = groupApi.getGroup(groupPath);

			Namespace namespace = new Namespace();
			namespace.setId(group.getId());

			Project project = new Project();
			project.setName(name);
			project.setNamespace(namespace);

			if (StringUtils.isNotEmpty(path)) {
				project.setPath(path);
			}

			project = projectApi.createProject(project);
			return new CreateRepositoryResponse(
				groupPath, project.getId(),
				this.checkGitLabServerUrl(project.getHttpUrlToRepo())
			);
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to create remote Git repository", e);
		}
	}

	/**
	 * Deletes an existing remote repository identified by <code>repositoryId</code>.
	 *
	 * @param repositoryId the ID of the repository to delete
	 * @throws RemoteGitServerManagerException if something went wrong while deleting the remote
	 *         repository
	 */
	@Override
	public void deleteRepository(int repositoryId) throws RemoteGitServerManagerException {
		ProjectApi projectApi = this.userGitLabApi.getProjectApi();

		try {
			projectApi.deleteProject(repositoryId);
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to delete remote Git repository", e);
		}
	}

	/**
	 * Creates a new remote group with the <code>name</code>, <code>path</code> and
	 * <code>description</code> supplied.
	 *
	 * @param name the name of the group to create
	 * @param path the path of the group to create
	 * @param description the description of the group to create
	 * @return the new group path
	 * @throws RemoteGitServerManagerException if something went wrong while creating the remote
	 *         group
	 */
	@Override
	public String createGroup(String name, String path, @Nullable String description)
			throws RemoteGitServerManagerException {
		GroupApi groupApi = this.userGitLabApi.getGroupApi();

		try {
			// none of the addGroup overloads accept a Group object parameter
			groupApi.addGroup(
				name, path, description,
				null, null, null, null,
				null, null, null
			);

			return path;
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to create remote group", e);
		}
	}

	/**
	 * Gets the names of all repositories in the remote group identified by <code>path</code>.
	 *
	 * @param path the path of the group whose repository names you want to get
	 * @return a {@link List<String>} of names
	 * @throws RemoteGitServerManagerException if something went wrong while fetching the repository
	 *         names of the remote group
	 */
	@Override
	public List<String> getGroupRepositoryNames(String path)
			throws RemoteGitServerManagerException {
		GroupApi groupApi = this.userGitLabApi.getGroupApi();

		try {
			Group group = groupApi.getGroup(path);
			List<Project> projects = group.getProjects();
			return projects.stream().map(Project::getName).collect(Collectors.toList());
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException(
				"Failed to get repository names for group", e
			);
		}
	}

	/**
	 * Deletes an existing remote group identified by <code>path</code>.
	 * <p>
	 * NB: Also deletes any child repositories!
	 *
	 * @param path the path of the group to delete
	 * @throws RemoteGitServerManagerException if something went wrong while deleting the remote
	 *         group
	 */
	@Override
	public void deleteGroup(String path) throws RemoteGitServerManagerException {
		GroupApi groupApi = this.userGitLabApi.getGroupApi();

		try {
			Group group = groupApi.getGroup(path);
			groupApi.deleteGroup(group);
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to delete remote group", e);
		}
	}

	// ***** methods below perform their actions as a GitLab admin *****

	/**
	 * Acquires (gets or creates) a GitLab impersonation token for the supplied
	 * <code>catmaUser</code>.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param catmaUser a {@link de.catma.user.User} object
	 * @return a {@link Pair} object with the first value being a {@link User} object and the second
	 *         value being the raw impersonation token string
	 * @throws RemoteGitServerManagerException if something went wrong while acquiring the GitLab
	 *         impersonation token
	 */
	private Pair<User, String> acquireImpersonationToken(de.catma.user.User catmaUser)
			throws RemoteGitServerManagerException {
		Pair<User, Boolean> userWasCreatedPair = this.acquireUser(catmaUser);
		User user = userWasCreatedPair.getFirst();
		Boolean wasCreated = userWasCreatedPair.getSecond();

		String impersonationToken = null;

		if (wasCreated) {
			// if the user was newly created, we need to create an impersonation token
			impersonationToken = this.createImpersonationToken(
				user.getId(), RemoteGitServerManager.GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME
			);
		}
		else {
			// if the user already existed, we expect there to be an impersonation token that we can
			// fetch
			CustomUserApi customUserApi = new CustomUserApi(this.adminGitLabApi);

			try {
				List<ImpersonationToken> impersonationTokens = customUserApi.getImpersonationTokens(
					user.getId(), "active"
				);

				// strictly speaking a user shouldn't have more than one impersonation token if
				// created through this class, but this at least allows for special cases
				for (ImpersonationToken token : impersonationTokens) {
					if (token.name.equals(
							RemoteGitServerManager.GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME)) {
						impersonationToken = token.token;
						break;
					}
				}
			}
			catch (GitLabApiException e) {
				throw new RemoteGitServerManagerException(
					"Failed to acquire impersonation token", e
				);
			}
		}

		if (impersonationToken == null) {
			String errorMessage = String.format(
				"Failed to acquire impersonation token for CATMA user ID `%s`, identifier `%s`. " +
				"This could be because the associated GitLab user ID `%s` does not have an " +
				"active impersonation token called `%s`, or has so many tokens that " +
				"CustomUserApi.getImpersonationTokens(int, String) is not returning it.",
				catmaUser.getUserId(), catmaUser.getIdentifier(),
				user.getId(), RemoteGitServerManager.GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME
			);
			throw new RemoteGitServerManagerException(errorMessage);
		}

		@SuppressWarnings("unchecked")
		Pair<User, String> retVal = new Pair(user, impersonationToken);

		return retVal;
	}

	/**
	 * Acquires (gets or creates) a GitLab user for the supplied <code>catmaUser</code>.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param catmaUser a {@link de.catma.user.User} object
	 * @return a {@link Pair} object with the first value being a {@link User} object and the second
	 *         value being a boolean indicating whether or not the user was newly created
	 * @throws RemoteGitServerManagerException if something went wrong while acquiring the GitLab
	 *         user
	 */
	private Pair<User, Boolean> acquireUser(de.catma.user.User catmaUser)
			throws RemoteGitServerManagerException {
		UserApi userApi = this.adminGitLabApi.getUserApi();
		Boolean userCreated = false;

		try {
			// we use the CATMA user identifier as the GitLab username
			User user = userApi.getUser(catmaUser.getIdentifier());

			// create the GitLab user if they don't exist
			// we generate a fake catma-user-<id>@catma.de email address for the GitLab user for a
			// few reasons:
			// 1. it gives us another way to match CATMA users to GitLab users, in addition to the
			//    CATMA user identifier / GitLab username, which could in theory change
			// 2. the CATMA user's email address could change
			// 3. we don't want GitLab to send any emails to the users
			if (user == null) {
				String gitLabUserEmailAddress = String.format(
					RemoteGitServerManager.GITLAB_USER_EMAIL_ADDRESS_FORMAT, catmaUser.getUserId()
				);
				user = this._createUser(
					gitLabUserEmailAddress, catmaUser.getIdentifier(), null,
					catmaUser.getName(), null
				);
				userCreated = true;
			}

			@SuppressWarnings("unchecked")
			Pair<User, Boolean> retVal = new Pair(user, userCreated);
			return retVal;
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to acquire remote user", e);
		}
	}

	// it's more convenient to work with the User class internally, which is why this method exists
	private User _createUser(String email, String username, @Nullable String password, String name,
							@Nullable Boolean isAdmin) throws RemoteGitServerManagerException {
		UserApi userApi = this.adminGitLabApi.getUserApi();

		if (password == null) {
			// generate a random password
			password = RandomStringUtils.random(
				12, 0, RemoteGitServerManager.PWD_CHARS.length-1,
				false, false, RemoteGitServerManager.PWD_CHARS, new SecureRandom()
			);
		}

		if (isAdmin == null) {
			isAdmin = false;
		}

		User user = new User();
		user.setEmail(email);
		user.setUsername(username);
		user.setName(name);
		user.setIsAdmin(isAdmin);

		try {
			user = userApi.createUser(user, password, null);
			return user;
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to create user", e);
		}
	}

	/**
	 * Creates a new remote user.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param email the email address of the user to create
	 * @param username the username of the user to create
	 * @param password the password of the user to create. A random, 12 character password will be
	 *                 generated if none is supplied.
	 * @param name the name of the user to create
	 * @param isAdmin whether the user to create should be an admin or not. Defaults to false if not
	 *                supplied.
	 * @return the new user ID
	 * @throws RemoteGitServerManagerException if something went wrong while creating the remote
	 *         user
	 */
	@Override
	public int createUser(String email, String username, @Nullable String password,
						   String name, @Nullable Boolean isAdmin)
			throws RemoteGitServerManagerException {
		User user = this._createUser(email, username, password, name, isAdmin);
		return user.getId();
	}

	/**
	 * Creates a new impersonation token for the GitLab user identified by <code>userId</code>.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param userId the ID of the user for which to create the impersonation token
	 * @param tokenName the name of the impersonation token to create
	 * @return the new token
	 * @throws RemoteGitServerManagerException if something went wrong while creating the
	 *         impersonation token
	 */
	private String createImpersonationToken(int userId, String tokenName)
			throws RemoteGitServerManagerException {
		CustomUserApi customUserApi = new CustomUserApi(this.adminGitLabApi);

		try {
			ImpersonationToken impersonationToken = customUserApi.createImpersonationToken(
				userId, tokenName, null, null
			);
			return impersonationToken.token;
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to create impersonation token", e);
		}
	}
}
