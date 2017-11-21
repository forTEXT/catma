package de.catma.repository.git;

import de.catma.repository.git.exceptions.*;
import de.catma.repository.git.interfaces.IGitProjectHandler;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.managers.GitLabServerManager;
import de.catma.repository.git.serialization.model_wrappers.GitSourceDocumentInfo;
import de.catma.util.IDGenerator;
import org.apache.commons.io.FileUtils;
import org.gitlab4j.api.models.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

public class GitProjectHandler implements IGitProjectHandler {
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	private final IDGenerator idGenerator;

	// using 'corpus' and not 'project' here so as not to confuse CATMA Projects with GitLab
	// Projects
	private static final String PROJECT_ROOT_REPOSITORY_NAME_FORMAT = "%s_corpus";

	public static String getProjectRootRepositoryName(String projectId) {
		return String.format(PROJECT_ROOT_REPOSITORY_NAME_FORMAT, projectId);
	}

	public static final String TAGSET_SUBMODULES_DIRECTORY_NAME = "tagsets";
	public static final String MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME = "collections";
	public static final String SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME = "documents";

	public GitProjectHandler(ILocalGitRepositoryManager localGitRepositoryManager,
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
	 * @throws GitProjectHandlerException if an error occurs when creating the project
	 */
	@Override
	public String create(@Nonnull String name, @Nullable String description) throws GitProjectHandlerException {
		String projectId = idGenerator.generate();

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the group
			String groupPath = this.remoteGitServerManager.createGroup(
				name, projectId, description
			);

			// create the root repository
			String projectNameAndPath = GitProjectHandler.getProjectRootRepositoryName(projectId);

			IRemoteGitServerManager.CreateRepositoryResponse response =
					this.remoteGitServerManager.createRepository(
				projectNameAndPath, projectNameAndPath, groupPath
			);

			// clone the root repository locally
			GitLabServerManager gitLabServerManager =
					(GitLabServerManager)this.remoteGitServerManager;
			User gitLabUser = gitLabServerManager.getGitLabUser();
			String gitLabUserImpersonationToken = gitLabServerManager
					.getGitLabUserImpersonationToken();

			localGitRepoManager.clone(
				response.repositoryHttpUrl,
				null,
				gitLabUser.getUsername(),
				gitLabUserImpersonationToken
			);
		}
		catch (RemoteGitServerManagerException|LocalGitRepositoryManagerException e) {
			throw new GitProjectHandlerException("Failed to create project", e);
		}

		return projectId;
	}

	/**
	 * Deletes an existing project.
	 * <p>
	 * This will also delete any associated repositories automatically (local & remote).
	 *
	 * @param projectId the ID of the project to delete
	 * @throws GitProjectHandlerException if an error occurs when deleting the project
	 */
	@Override
	public void delete(@Nonnull String projectId) throws GitProjectHandlerException {
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
			throw new GitProjectHandlerException("Failed to delete project", e);
		}
	}

	// tagset operations
	public String createTagset(@Nonnull String projectId,
							   @Nullable String tagsetId,
							   @Nonnull String name,
							   @Nullable String description
	) throws GitProjectHandlerException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(localGitRepoManager, this.remoteGitServerManager);

			// create the tagset
			String newTagsetId = gitTagsetHandler.create(projectId, tagsetId, name, description);

			// push the newly created tagset repo to the server in preparation for adding it to the project root repo
			// as a submodule
			// TODO: create a provider to retrieve the auth details so that we don't have to cast to the implementation
			GitLabServerManager gitLabServerManager = (GitLabServerManager)this.remoteGitServerManager;

			User gitLabUser = gitLabServerManager.getGitLabUser();
			String gitLabUserImpersonationToken = gitLabServerManager.getGitLabUserImpersonationToken();

			localGitRepoManager.open(GitTagsetHandler.getTagsetRepositoryName(newTagsetId));
			localGitRepoManager.push(gitLabUser.getUsername(), gitLabUserImpersonationToken);
			String tagsetRepoRemoteUrl = localGitRepoManager.getRemoteUrl(null);
			localGitRepoManager.detach(); // need to explicitly detach so that we can call open below

			// open the project root repo
			localGitRepoManager.open(GitProjectHandler.getProjectRootRepositoryName(projectId));

			// add the submodule
			File targetSubmodulePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					TAGSET_SUBMODULES_DIRECTORY_NAME,
					newTagsetId
			).toFile();

			localGitRepoManager.addSubmodule(
					targetSubmodulePath,
					tagsetRepoRemoteUrl,
					gitLabUser.getUsername(),
					gitLabUserImpersonationToken
			);

			return newTagsetId;
		}
		catch (GitTagsetHandlerException |LocalGitRepositoryManagerException e) {
			throw new GitProjectHandlerException("Failed to create tagset", e);
		}
	}

	// markup collection operations
	public String createMarkupCollection(@Nonnull String projectId,
										 @Nullable String markupCollectionId,
										 @Nonnull String name,
										 @Nullable String description,
										 @Nonnull String sourceDocumentId,
										 @Nonnull String sourceDocumentVersion
	) throws GitProjectHandlerException {

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
					localGitRepoManager, this.remoteGitServerManager
			);

			// create the markup collection
			String newMarkupCollectionId = gitMarkupCollectionHandler.create(
					projectId,
					markupCollectionId,
					name,
					description,
					sourceDocumentId,
					sourceDocumentVersion
			);

			// push the newly created markup collection repo to the server in preparation for adding it to the project
			// root repo as a submodule
			// TODO: create a provider to retrieve the auth details so that we don't have to cast to the implementation
			GitLabServerManager gitLabServerManager = (GitLabServerManager)this.remoteGitServerManager;

			User gitLabUser = gitLabServerManager.getGitLabUser();
			String gitLabUserImpersonationToken = gitLabServerManager.getGitLabUserImpersonationToken();

			localGitRepoManager.open(GitMarkupCollectionHandler.getMarkupCollectionRepositoryName(newMarkupCollectionId));
			localGitRepoManager.push(gitLabUser.getUsername(), gitLabUserImpersonationToken);
			String markupCollectionRepoRemoteUrl = localGitRepoManager.getRemoteUrl(null);
			localGitRepoManager.detach(); // need to explicitly detach so that we can call open below

			// open the project root repo
			localGitRepoManager.open(GitProjectHandler.getProjectRootRepositoryName(projectId));

			// add the submodule
			File targetSubmodulePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME,
					newMarkupCollectionId
			).toFile();

			localGitRepoManager.addSubmodule(
					targetSubmodulePath,
					markupCollectionRepoRemoteUrl,
					gitLabUser.getUsername(),
					gitLabUserImpersonationToken
			);

			return newMarkupCollectionId;
		}
		catch (GitMarkupCollectionHandlerException |LocalGitRepositoryManagerException e) {
			throw new GitProjectHandlerException("Failed to create markup collection", e);
		}
	}

	// source document operations

	/**
	 * Creates a new source document within the project identified by <code>projectId</code>.
	 *
	 * @param projectId the ID of the project within which the source document must be created
	 * @param sourceDocumentId the ID of the source document to create. If none is provided, a new
	 *                         ID will be generated.
	 * @param originalSourceDocumentStream a {@link InputStream} object representing the original,
	 *                                     unmodified source document
	 * @param originalSourceDocumentFileName the file name of the original, unmodified source
	 *                                       document
	 * @param convertedSourceDocumentStream a {@link InputStream} object representing the converted,
	 *                                      UTF-8 encoded source document
	 * @param convertedSourceDocumentFileName the file name of the converted, UTF-8 encoded source
	 *                                        document
	 * @param gitSourceDocumentInfo a {@link GitSourceDocumentInfo} wrapper object
	 * @return the <code>sourceDocumentId</code> if one was provided, otherwise a new source
	 *         document ID
	 * @throws GitProjectHandlerException if an error occurs while creating the source document
	 */
	@Override
	public String createSourceDocument(
			@Nonnull String projectId, @Nullable String sourceDocumentId,
			@Nonnull InputStream originalSourceDocumentStream, @Nonnull String originalSourceDocumentFileName,
			@Nonnull InputStream convertedSourceDocumentStream, @Nonnull String convertedSourceDocumentFileName,
			@Nonnull GitSourceDocumentInfo gitSourceDocumentInfo
	) throws GitProjectHandlerException {
		try (ILocalGitRepositoryManager repoManager = this.localGitRepositoryManager) {
			GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
				repoManager, this.remoteGitServerManager
			);

			// create the source document within the project
			sourceDocumentId = gitSourceDocumentHandler.create(
					projectId, sourceDocumentId,
					originalSourceDocumentStream, originalSourceDocumentFileName,
					convertedSourceDocumentStream, convertedSourceDocumentFileName,
					gitSourceDocumentInfo
			);

			GitLabServerManager gitLabServerManager = (GitLabServerManager)this.remoteGitServerManager;
			String gitLabUserImpersonationToken = gitLabServerManager.getGitLabUserImpersonationToken();

			repoManager.open(GitSourceDocumentHandler.getSourceDocumentRepositoryName(sourceDocumentId));
			repoManager.push(gitLabServerManager.getGitLabUser().getUsername(), gitLabUserImpersonationToken);

			String remoteUri = repoManager.getRemoteUrl(null);
			repoManager.close();

			// open the project root repository
			repoManager.open(GitProjectHandler.getProjectRootRepositoryName(projectId));

			// create the submodule
			File targetSubmodulePath = Paths.get(
					repoManager.getRepositoryWorkTree().toString(),
					SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME,
					sourceDocumentId
			).toFile();

			repoManager.addSubmodule(
				targetSubmodulePath, remoteUri,
				gitLabServerManager.getGitLabUser().getUsername(), gitLabUserImpersonationToken
			);
		}
		catch (GitSourceDocumentHandlerException |LocalGitRepositoryManagerException e) {
			throw new GitProjectHandlerException("Failed to create source document", e);
		}

		return sourceDocumentId;
	}
}
