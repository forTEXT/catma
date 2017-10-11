package de.catma.repository.git;

import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.IProjectHandler;
import de.catma.repository.git.exceptions.ProjectHandlerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.util.IDGenerator;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ProjectHandler implements IProjectHandler {
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	private final IDGenerator idGenerator;

	// using 'corpus' and not 'project' here so as not to confuse CATMA Projects with GitLab
	// Projects
	static final String PROJECT_ROOT_REPOSITORY_NAME_FORMAT = "%s_corpus";

	static final String PROJECT_ROOT_REPOSITORY_DEFAULT_GITIGNORE = "tagsets\ncollections\ndocuments";

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
			RemoteGitServerManager remoteGitServerManagerImpl =
					(RemoteGitServerManager)this.remoteGitServerManager;
			String gitLabUserImpersonationToken = remoteGitServerManagerImpl
					.getGitLabUserImpersonationToken();

			String authenticatedRepositoryUrl = GitLabAuthenticationHelper
					.buildAuthenticatedRepositoryUrl(
						response.repositoryHttpUrl, gitLabUserImpersonationToken
					);

			this.localGitRepositoryManager.clone(
				authenticatedRepositoryUrl,
				remoteGitServerManagerImpl.getGitLabUser().getUsername(),
				gitLabUserImpersonationToken
			);

			File repositoryWorkTree = this.localGitRepositoryManager.getRepositoryWorkTree();

			// write .gitignore into the local repo
			File targetGitIgnoreFile = new File(repositoryWorkTree, ".gitignore");
			this.localGitRepositoryManager.add(
				targetGitIgnoreFile,
				ProjectHandler.PROJECT_ROOT_REPOSITORY_DEFAULT_GITIGNORE.getBytes(StandardCharsets.UTF_8)
			);

			// write empty tagsets.json, collections.json & documents.json into the local repo
			File targetTagsetsFile = new File(repositoryWorkTree, "tagsets.json");
			this.localGitRepositoryManager.add(targetTagsetsFile, new byte[]{});

			File targetCollectionsFile = new File(repositoryWorkTree, "collections.json");
			this.localGitRepositoryManager.add(targetCollectionsFile, new byte[]{});

			File targetDocumentsFile = new File(repositoryWorkTree, "documents.json");
			this.localGitRepositoryManager.add(targetDocumentsFile, new byte[]{});

			// commit newly added files
			String commitMessage = String.format("Adding %s, %s, %s and %s", targetTagsetsFile.getName(),
					targetCollectionsFile.getName(), targetDocumentsFile.getName(), targetGitIgnoreFile.getName());
			this.localGitRepositoryManager.commit(commitMessage);
		}
		catch (RemoteGitServerManagerException|LocalGitRepositoryManagerException|
				URISyntaxException e) {
			throw new ProjectHandlerException("Failed to create project", e);
		}

		return projectId;
	}

	/**
	 * Deletes an existing project.
	 * <p>
	 * This will also delete any associated repositories automatically (local & remote).
	 *
	 * @param projectId the ID of the project to delete
	 * @throws ProjectHandlerException if an error occurs when deleting the project
	 */
	@Override
	public void delete(String projectId) throws ProjectHandlerException {
		try {
			List<String> repositoryNames = this.remoteGitServerManager.getGroupRepositoryNames(
				projectId
			);

			for (String name : repositoryNames) {
				FileUtils.deleteDirectory(
					new File(this.localGitRepositoryManager.getRepositoryBasePath(), name)
				);
			}

			this.remoteGitServerManager.deleteGroup(projectId);
		}
		catch (RemoteGitServerManagerException|IOException e) {
			throw new ProjectHandlerException("Failed to delete project", e);
		}
	}
}
