package de.catma.repository.git;

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
import java.util.Properties;

public class RemoteGitServerManager implements IRemoteGitServerManager {
	private String gitLabAdminPersonalAccessToken;
	private String gitLabServerUrl;

	private GitLabApi gitLabApi;

	protected GitLabApi getGitLabApi() {
		return this.gitLabApi;
	}

	public RemoteGitServerManager(Properties catmaProperties) {
		this.gitLabAdminPersonalAccessToken = catmaProperties.getProperty(
			"GitLabAdminPersonalAccessToken"
		);
		this.gitLabServerUrl = catmaProperties.getProperty("GitLabServerUrl");

		this.gitLabApi = new GitLabApi(this.gitLabServerUrl, this.gitLabAdminPersonalAccessToken);
	}

	@Override
	public int createRepository(String name, @Nullable String path)
			throws RemoteGitServerManagerException {
		ProjectApi projectApi = this.gitLabApi.getProjectApi();

		Project project = new Project();
		project.setName(name);

		if (StringUtils.isNotEmpty(path)) {
			project.setPath(path);
		}

		try {
			project = projectApi.createProject(project);
			return project.getId();
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to create remote Git repository", e);
		}
	}

	@Override
	public int createRepository(String name, @Nullable String path, int groupId)
			throws RemoteGitServerManagerException {
		ProjectApi projectApi = this.gitLabApi.getProjectApi();

		Namespace namespace = new Namespace();
		namespace.setId(groupId);

		Project project = new Project();
		project.setName(name);
		project.setNamespace(namespace);

		if (StringUtils.isNotEmpty(path)) {
			project.setPath(path);
		}

		try {
			project = projectApi.createProject(project);
			return project.getId();
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to create remote Git repository", e);
		}
	}

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

	@Override
	public int createGroup(String name, String path, @Nullable String description)
			throws RemoteGitServerManagerException {
		GroupApi groupApi = this.gitLabApi.getGroupApi();

		try {
			// none of the addGroup overloads accept a Group object parameter
			groupApi.addGroup(
				name, path, description,
				null, null, null, null,
				null, null, null
			);

			// fetch the group (none of the addGroup overloads return a group or its id)
			Group group = groupApi.getGroup(path);
			return group.getId();
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to create remote group", e);
		}
	}

	@Override
	public void deleteGroup(int groupId) throws RemoteGitServerManagerException {
		GroupApi groupApi = gitLabApi.getGroupApi();

		try {
			groupApi.deleteGroup(groupId);
		}
		catch (GitLabApiException e) {
			throw new RemoteGitServerManagerException("Failed to delete remote group", e);
		}
	}
}
