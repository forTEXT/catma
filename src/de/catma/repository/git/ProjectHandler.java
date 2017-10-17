package de.catma.repository.git;

import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.exceptions.SourceDocumentHandlerException;
import de.catma.repository.git.interfaces.IProjectHandler;
import de.catma.repository.git.exceptions.ProjectHandlerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.model_wrappers.GitSourceDocumentInfo;
import de.catma.util.IDGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.models.User;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

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

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
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
			User gitLabUser = remoteGitServerManagerImpl.getGitLabUser();
			String gitLabUserImpersonationToken = remoteGitServerManagerImpl
					.getGitLabUserImpersonationToken();

			localGitRepoManager.clone(
				response.repositoryHttpUrl,
				null,
				gitLabUser.getUsername(),
				gitLabUserImpersonationToken
			);

			File repositoryWorkTree = localGitRepoManager.getRepositoryWorkTree();

			// write empty tagsets.json into the local repo
			File targetTagsetsFile = new File(repositoryWorkTree, "tagsets.json");
			localGitRepoManager.addAndCommit(
				targetTagsetsFile, new byte[]{},
				StringUtils.isNotBlank(gitLabUser.getName()) ? gitLabUser.getName() : gitLabUser.getUsername(),
				gitLabUser.getEmail()
			);
		}
		catch (RemoteGitServerManagerException|LocalGitRepositoryManagerException e) {
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

	// source document operations

	/**
	 * Inserts a new source document into the project identified by <code>projectId</code>.
	 *
	 * @param projectId the ID of the project that the source document must be inserted into
	 * @param originalSourceDocumentStream a {@link InputStream} object representing the original,
	 *                                     unmodified source document
	 * @param originalSourceDocumentFileName the file name of the original, unmodified source
	 *                                       document
	 * @param convertedSourceDocumentStream a {@link InputStream} object representing the converted,
	 *                                      UTF-8 encoded source document
	 * @param convertedSourceDocumentFileName the file name of the converted, UTF-8 encoded source
	 *                                        document
	 * @param gitSourceDocumentInfo a {@link GitSourceDocumentInfo} wrapper object
	 * @param sourceDocumentId the ID of the source document to insert. If none is provided, a new
	 *                         ID will be generated.
	 * @return the <code>sourceDocumentId</code> if one was provided, otherwise a new source
	 *         document ID
	 * @throws ProjectHandlerException if an error occurs while inserting the source document
	 */
	@Override
	public String insertSourceDocument(
			String projectId,
			InputStream originalSourceDocumentStream, String originalSourceDocumentFileName,
			InputStream convertedSourceDocumentStream, String convertedSourceDocumentFileName,
			GitSourceDocumentInfo gitSourceDocumentInfo,
			@Nullable String sourceDocumentId) throws ProjectHandlerException {
		try (ILocalGitRepositoryManager repoManager = this.localGitRepositoryManager) {
			SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler(
				repoManager, this.remoteGitServerManager
			);

			// insert the source document into the project
			sourceDocumentId = sourceDocumentHandler.insert(
					originalSourceDocumentStream, originalSourceDocumentFileName,
					convertedSourceDocumentStream, convertedSourceDocumentFileName,
					gitSourceDocumentInfo, sourceDocumentId, projectId);

			RemoteGitServerManager remoteGitServerManagerImpl = (RemoteGitServerManager)this.remoteGitServerManager;
			String gitLabUserImpersonationToken = remoteGitServerManagerImpl.getGitLabUserImpersonationToken();

			repoManager.open(sourceDocumentId);
			repoManager.push(remoteGitServerManagerImpl.getGitLabUser().getUsername(), gitLabUserImpersonationToken);

			String remoteUri = repoManager.getRemoteUrl(null);
			repoManager.close();

			// open the project root repository
			repoManager.open(String.format(ProjectHandler.PROJECT_ROOT_REPOSITORY_NAME_FORMAT, projectId));

			// create the submodule
			File targetSubmodulePath = Paths.get(
				repoManager.getRepositoryWorkTree().toString(), "documents", sourceDocumentId
			).toFile();

			repoManager.addSubmodule(
				targetSubmodulePath, remoteUri,
				remoteGitServerManagerImpl.getGitLabUser().getUsername(), gitLabUserImpersonationToken
			);
		}
		catch (SourceDocumentHandlerException|LocalGitRepositoryManagerException e) {
			throw new ProjectHandlerException("Failed to insert source document", e);
		}

		return sourceDocumentId;
	}
}
