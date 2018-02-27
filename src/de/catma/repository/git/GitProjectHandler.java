package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;

import de.catma.document.source.SourceDocumentInfo;
import de.catma.repository.git.exceptions.GitMarkupCollectionHandlerException;
import de.catma.repository.git.exceptions.GitProjectHandlerException;
import de.catma.repository.git.exceptions.GitSourceDocumentHandlerException;
import de.catma.repository.git.exceptions.GitTagsetHandlerException;
import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.IGitProjectHandler;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.util.IDGenerator;

public class GitProjectHandler implements IGitProjectHandler {
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	private final IDGenerator idGenerator;

	private static final String PROJECT_ROOT_REPOSITORY_NAME_FORMAT = "%s_root";

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

		//TODO: consider creating local git projects for offline use

		String projectId = idGenerator.generate() + "_" + name.replaceAll("[^\\p{Alnum}]", "_");

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the group
			String groupPath = this.remoteGitServerManager.createGroup(
				projectId, projectId, name
			);

			// create the root repository
			String projectNameAndPath = GitProjectHandler.getProjectRootRepositoryName(name);

			IRemoteGitServerManager.CreateRepositoryResponse response =
					this.remoteGitServerManager.createRepository(
				name, projectNameAndPath, groupPath
			);

			// clone the root repository locally
			localGitRepoManager.clone(
				projectId,
				response.repositoryHttpUrl,
				null,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getPassword()
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

			localGitRepoManager.open(projectId, GitTagsetHandler.getTagsetRepositoryName(newTagsetId));
			localGitRepoManager.push(remoteGitServerManager.getUsername(), remoteGitServerManager.getPassword());
			String tagsetRepoRemoteUrl = localGitRepoManager.getRemoteUrl(null);
			localGitRepoManager.detach(); // need to explicitly detach so that we can call open below

			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectHandler.getProjectRootRepositoryName(projectId));

			// add the submodule
			File targetSubmodulePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					TAGSET_SUBMODULES_DIRECTORY_NAME,
					newTagsetId
			).toFile();

			localGitRepoManager.addSubmodule(
					targetSubmodulePath,
					tagsetRepoRemoteUrl,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getPassword()
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

			localGitRepoManager.open(projectId, GitMarkupCollectionHandler.getMarkupCollectionRepositoryName(newMarkupCollectionId));
			localGitRepoManager.push(remoteGitServerManager.getUsername(), remoteGitServerManager.getPassword());
			String markupCollectionRepoRemoteUrl = localGitRepoManager.getRemoteUrl(null);
			localGitRepoManager.detach(); // need to explicitly detach so that we can call open below

			// open the project root repo
			localGitRepoManager.open(projectId, GitProjectHandler.getProjectRootRepositoryName(projectId));

			// add the submodule
			File targetSubmodulePath = Paths.get(
					localGitRepoManager.getRepositoryWorkTree().toString(),
					MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME,
					newMarkupCollectionId
			).toFile();

			localGitRepoManager.addSubmodule(
					targetSubmodulePath,
					markupCollectionRepoRemoteUrl,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getPassword()
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
	 * @param sourceDocumentInfo a {@link SourceDocumentInfo} object
	 * @return the <code>sourceDocumentId</code> if one was provided, otherwise a new source
	 *         document ID
	 * @throws GitProjectHandlerException if an error occurs while creating the source document
	 */
	@Override
	public String createSourceDocument(
			@Nonnull String projectId, @Nullable String sourceDocumentId,
			@Nonnull InputStream originalSourceDocumentStream, @Nonnull String originalSourceDocumentFileName,
			@Nonnull InputStream convertedSourceDocumentStream, @Nonnull String convertedSourceDocumentFileName,
			@Nonnull SourceDocumentInfo sourceDocumentInfo
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
					sourceDocumentInfo
			);

			repoManager.open(projectId, GitSourceDocumentHandler.getSourceDocumentRepositoryName(sourceDocumentId));
			repoManager.push(remoteGitServerManager.getUsername(), remoteGitServerManager.getPassword());

			String remoteUri = repoManager.getRemoteUrl(null);
			repoManager.close();

			// open the project root repository
			repoManager.open(projectId, GitProjectHandler.getProjectRootRepositoryName(projectId));

			// create the submodule
			File targetSubmodulePath = Paths.get(
					repoManager.getRepositoryWorkTree().toString(),
					SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME,
					sourceDocumentId
			).toFile();

			repoManager.addSubmodule(
				targetSubmodulePath, remoteUri,
				remoteGitServerManager.getUsername(),
				remoteGitServerManager.getPassword()
			);
		}
		catch (GitSourceDocumentHandlerException |LocalGitRepositoryManagerException e) {
			throw new GitProjectHandlerException("Failed to create source document", e);
		}

		return sourceDocumentId;
	}
}
