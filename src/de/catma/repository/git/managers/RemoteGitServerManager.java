package de.catma.repository.git.managers;

import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Namespace;
import org.gitlab4j.api.models.Project;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class RemoteGitServerManager implements IRemoteGitServerManager {
	private final String gitLabAdminPersonalAccessToken;
	private final String gitLabServerUrl;

	private final GitLabApi gitLabApi;

	GitLabApi getGitLabApi() {
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
		GroupApi groupApi = gitLabApi.getGroupApi();

		try {
			Group group = groupApi.getGroup(path);
			groupApi.deleteGroup(group);
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to delete remote group", e);
		}
	}
}
