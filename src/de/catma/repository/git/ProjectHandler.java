package de.catma.repository.git;

import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.IGitBasedProjectHandler;
import de.catma.repository.git.interfaces.IProjectHandler;
import de.catma.repository.git.exceptions.ProjectHandlerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.util.IDGenerator;

public class ProjectHandler implements IProjectHandler, IGitBasedProjectHandler {
	private ILocalGitRepositoryManager localGitRepositoryManager;
	private IRemoteGitServerManager remoteGitServerManager;

	private IDGenerator idGenerator;

	// using 'corpus' and not 'project' here so as not to confuse CATMA Projects with GitLab
	// Projects
	final String projectRootRepositoryNameFormat = "%s_corpus";

	public ProjectHandler(ILocalGitRepositoryManager localGitRepositoryManager,
						  IRemoteGitServerManager remoteGitServerManager) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;

		this.idGenerator = new IDGenerator();
	}

	/**
	 * Gets the 'root' repository for a particular CATMA Project (GitLab Group).
	 *
	 * @param groupId the GitLab Group ID
	 * @return the HTTP URL of the repository
	 * @throws ProjectHandlerException if an error occurs when calling the GitLab API
	 */
//	@Override
//	public String getRootRepositoryHttpUrl(int groupId) throws ProjectHandlerException {
//		GroupApi groupApi = gitLabApi.getGroupApi();
//		ProjectApi projectApi = gitLabApi.getProjectApi();
//
//		Project project;
//
//		try {
//			Group group = groupApi.getGroup(groupId);
//			String projectNameAndPath = String.format(projectRootRepositoryNameFormat, group.getPath());
//
//			project = projectApi.getProject(group.getPath(), projectNameAndPath);
//		}
//		catch (GitLabApiException e) {
//			throw new ProjectHandlerException("Error calling GitLab API", e);
//		}
//
//		return project.getHttpUrlToRepo();
//	}

	/**
	 * Creates a new project.
	 *
	 * @param name the name of the project
	 * @param description the description of the project
	 * @return the project ID
	 * @throws ProjectHandlerException if an error occurs when creating the project
	 */
	@Override
	public String create(String name, String description) throws ProjectHandlerException {
		String projectId = idGenerator.generate();

		try {
			// create the group
			String groupPath = this.remoteGitServerManager.createGroup(
				name, projectId, description
			);

			// create the root repository
			String projectNameAndPath = String.format(
				this.projectRootRepositoryNameFormat, projectId
			);
			IRemoteGitServerManager.CreateRepositoryResponse response =
					this.remoteGitServerManager.createRepository(
				projectNameAndPath, projectNameAndPath, groupPath
			);

			// clone the root repository locally
			this.localGitRepositoryManager.clone(response.repositoryHttpUrl);
		}
		catch (RemoteGitServerManagerException|LocalGitRepositoryManagerException e) {
			throw new ProjectHandlerException("Failed to create project", e);
		}

		return projectId;
	}

	/**
	 * Deletes an existing project.
	 * <p>
	 * This will also delete any associated repositories automatically.
	 *
	 * @param projectId the project ID
	 * @throws ProjectHandlerException if an error occurs when deleting the project
	 */
	@Override
	public void delete(String projectId) throws ProjectHandlerException {
		try {
			this.remoteGitServerManager.deleteGroup(projectId);
		}
		catch (RemoteGitServerManagerException e) {
			throw new ProjectHandlerException("Failed to delete project", e);
		}
	}
}
