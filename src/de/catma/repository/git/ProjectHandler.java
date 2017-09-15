package de.catma.repository.git;

import de.catma.util.IDGenerator;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;

import java.util.Properties;

public class ProjectHandler {
	private final String projectNameFormat = "group_%s_corpus";

	private Properties catmaProperties;
	private String gitLabAdminPersonalAccessToken;
	private String gitLabServerUrl;
	private GitLabApi gitLabApi;
	IDGenerator idGenerator;

	public ProjectHandler(Properties catmaProperties) {
		this.catmaProperties = catmaProperties;

		this.gitLabAdminPersonalAccessToken = catmaProperties.getProperty("GitLabAdminPersonalAccessToken");
		this.gitLabServerUrl = catmaProperties.getProperty("GitLabServerUrl");

		this.gitLabApi = new GitLabApi(this.gitLabServerUrl, this.gitLabAdminPersonalAccessToken);

		this.idGenerator = new IDGenerator();
	}

	/**
	 * Creates the 'root' repository (GitLab Project) for the CATMA Project (GitLab Group).
	 * This repository will reference other repositories (Tagsets, Markup Collections and Source Documents) in the CATMA Project.
	 *
	 * @param group the GitLab Group within which to create the root repository
	 * @return the GitLab Project ID
	 */
	private int createRootRepository(Group group) throws GitLabApiException {
		ProjectApi projectApi = gitLabApi.getProjectApi();

		// using 'corpus' and not 'project' here so as not to confuse CATMA Projects with GitLab Projects
		String projectNameAndPath = String.format(projectNameFormat, group.getPath());

		Project project = projectApi.createProject(group.getId(), projectNameAndPath);
		project.setPath(projectNameAndPath);

		return project.getId();
	}

	/**
	 * Gets the 'root' repository for a particular CATMA Project (GitLab Group).
	 *
	 * @param groupId the ID of the GitLab Group
	 * @return the HTTP URL of the repository
	 * @throws GitLabApiException if an error occurs when calling the GitLab API
	 */
	public String getRootRepositoryHttpUrl(int groupId) throws GitLabApiException {
		GroupApi groupApi = gitLabApi.getGroupApi();
		ProjectApi projectApi = gitLabApi.getProjectApi();

		Group group = groupApi.getGroup(groupId);
		String projectNameAndPath = String.format(projectNameFormat, group.getPath());

		Project project = projectApi.getProject(group.getPath(), projectNameAndPath);

		return project.getHttpUrlToRepo();
	}

	/**
	 * Creates a new CATMA Project (GitLab Group).
	 * This will also automatically create the 'root' repository (GitLab Project).
	 *
	 * @param name the name of the project
	 * @param description the description of the project
	 * @return the GitLab Group ID
	 * @throws GitLabApiException if an error occurs when calling the GitLab API
	 */
	public int create(String name, String description) throws GitLabApiException {
		GroupApi groupApi = gitLabApi.getGroupApi();
		ProjectApi projectApi = gitLabApi.getProjectApi();

		// create a unique path for the GitLab group
		String path = idGenerator.generate();

		// create the GitLab group
		groupApi.addGroup(
			name, path, description,
			null, null, null, null, null, null, null
		);

		// fetch the GitLab group (none of the addGroup overloads return a group or its id)
		Group group = groupApi.getGroup(path);

		// create the root repository
		createRootRepository(group);

		return group.getId();
	}

	/**
	 * Deletes an existing CATMA Project (GitLab Group).
	 * This will also automatically delete any associated repositories (GitLab Projects).
	 *
	 * @param groupId the GitLab Group ID
	 * @throws GitLabApiException if an error occurs when calling the GitLab API
	 */
	public void delete(int groupId) throws GitLabApiException {
		GroupApi groupApi = gitLabApi.getGroupApi();

		groupApi.deleteGroup(groupId);
	}
}