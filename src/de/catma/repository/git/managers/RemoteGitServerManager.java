package de.catma.repository.git.managers;

import de.catma.repository.git.managers.gitlab4j_api_custom.models.ImpersonationToken;
import de.catma.repository.git.managers.gitlab4j_api_custom.CustomUserApi;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
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

	private final GitLabApi gitLabApi;

	public GitLabApi getGitLabApi() {
		return this.gitLabApi;
	}

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

	public RemoteGitServerManager(Properties catmaProperties) {
		this.gitLabAdminPersonalAccessToken = catmaProperties.getProperty(
			"GitLabAdminPersonalAccessToken"
		);
		this.gitLabServerUrl = catmaProperties.getProperty("GitLabServerUrl");

		this.gitLabApi = new GitLabApi(this.gitLabServerUrl, this.gitLabAdminPersonalAccessToken);
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
		ProjectApi projectApi = this.gitLabApi.getProjectApi();

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
		GroupApi groupApi = this.gitLabApi.getGroupApi();
		ProjectApi projectApi = this.gitLabApi.getProjectApi();

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
		ProjectApi projectApi = this.gitLabApi.getProjectApi();

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
		GroupApi groupApi = this.gitLabApi.getGroupApi();

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
		GroupApi groupApi = this.gitLabApi.getGroupApi();

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
		GroupApi groupApi = this.gitLabApi.getGroupApi();

		try {
			Group group = groupApi.getGroup(path);
			groupApi.deleteGroup(group);
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to delete remote group", e);
		}
	}

	/**
	 * Creates a new remote user.
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
		UserApi userApi = this.gitLabApi.getUserApi();

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
			return user.getId();
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to create user", e);
		}
	}

	/**
	 * Creates a new impersonation token for the user identified by <code>userId</code>.
	 *
	 * @param userId the ID of the user for which to create the impersonation token
	 * @param tokenName the name of the impersonation token to create
	 * @return the new token
	 * @throws RemoteGitServerManagerException if something went wrong while creating the
	 *         impersonation token
	 */
	public String createImpersonationToken(int userId, String tokenName)
			throws RemoteGitServerManagerException {
		CustomUserApi customUserApi = new CustomUserApi(this.gitLabApi);

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
