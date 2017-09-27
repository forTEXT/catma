package de.catma.repository.git;

import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.IProjectHandler;
import de.catma.repository.git.exceptions.ProjectHandlerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.util.IDGenerator;

public class ProjectHandler implements IProjectHandler {
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	private final IDGenerator idGenerator;

	// using 'corpus' and not 'project' here so as not to confuse CATMA Projects with GitLab
	// Projects
	static final String PROJECT_ROOT_REPOSITORY_NAME_FORMAT = "%s_corpus";

	public ProjectHandler(ILocalGitRepositoryManager localGitRepositoryManager,
						  IRemoteGitServerManager remoteGitServerManager) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;

		this.idGenerator = new IDGenerator();
	}

	/**
	 * Creates a new project.
	 *
	 * @param name the name of the project to create
	 * @param description the description of the project to create
	 * @return the new project ID
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
				ProjectHandler.PROJECT_ROOT_REPOSITORY_NAME_FORMAT, projectId
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
	 * @param projectId the ID of the project to delete
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
