package de.catma.repository.git;

import de.catma.repository.git.exceptions.*;
import de.catma.repository.git.interfaces.IProjectHandler;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.serialization.model_wrappers.GitSourceDocumentInfo;
import de.catma.util.IDGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
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

	String getProjectRepoName(String projectId){
		return String.format(PROJECT_ROOT_REPOSITORY_NAME_FORMAT, projectId);
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
			String projectNameAndPath = getProjectRepoName(projectId);

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

	// tagset operations


	// markup collection operations
	/**
	 * Adds an existing tagset, identified by <code>tagsetId</code> and <code>tagsetVersion</code>, to the markup
	 * collection identified by <code>projectId</code> and <code>markupCollectionId</code>.
	 *
	 * @param projectId the ID of the project that contains the markup collection the tagset must be added to
	 * @param markupCollectionId the ID of the markup collection that the tagset must be added to
	 * @param tagsetId the ID of the tagset to add
	 * @param tagsetVersion the version of the tagset to add
	 * @throws ProjectHandlerException if an error occurs while adding the tagset
	 */
	public void addTagsetToMarkupCollection(String projectId, String markupCollectionId,
											String tagsetId, String tagsetVersion)
			throws ProjectHandlerException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			MarkupCollectionHandler markupCollectionHandler = new MarkupCollectionHandler(
				localGitRepoManager, this.remoteGitServerManager
			);

			// TODO: verify that there are no un-committed or -pushed changes

			// TODO: currently we're doing "Getting an update from the submodule’s remote", but we'll
			// (probably) want to do "Updating a submodule in-place in the container" instead
			// https://medium.com/@porteneuve/mastering-git-submodules-34c65e940407

			RemoteGitServerManager remoteGitServerManagerImpl = (RemoteGitServerManager)this.remoteGitServerManager;
			User gitLabUser = remoteGitServerManagerImpl.getGitLabUser();
			String gitLabUserImpersonationToken = remoteGitServerManagerImpl.getGitLabUserImpersonationToken();

			// TODO: this shouldn't happen here at all - the markup collection should have been added to the project
			// already before this method is called
			localGitRepoManager.open(markupCollectionId);
			String markupCollectionRemoteUrl = localGitRepoManager.getRemoteUrl(null);
			localGitRepoManager.close();

			String projectRepoName = getProjectRepoName(projectId);
			localGitRepoManager.open(projectRepoName);
			File markupCollectionSubmoduleTargetPath = new File(
				String.format("%s/collections/%s", localGitRepoManager.getRepositoryWorkTree(), markupCollectionId)
			);
			localGitRepoManager.addSubmodule(
				markupCollectionSubmoduleTargetPath, markupCollectionRemoteUrl,
				gitLabUser.getUsername(), gitLabUserImpersonationToken
			);
			localGitRepoManager.close();

			// add the tagset to the markup collection
			// TODO: this should affect the submodule directly, not the original markup collection repo
			// (which might not even exist anymore locally as we only need it for the initial push)
			markupCollectionHandler.addTagset(markupCollectionId, tagsetId, tagsetVersion);

			// TODO: this should push the submodule, not the original markup collection repo
			localGitRepoManager.open(markupCollectionId);
			localGitRepoManager.push(gitLabUser.getUsername(), gitLabUserImpersonationToken);
			localGitRepoManager.close();

			// open the markup collection submodule
			// a submodule can't be opened like any other repo, but we can get a Repository object using
			// the SubmoduleWalk class and we can then construct a new Git object with that Repository object

//			localGitRepoManager.open(String.format("%s/collections/%s", projectRepoName, markupCollectionId));

			// update submodule

			// using submoduleUpdate do this for the time being, however that will need to change as submoduleUpdate
			// doesn't let us specify a revision or branch name (tagsetVersion)

//			localGitRepoManager.fetch(gitLabUser.getUsername(), gitLabUserImpersonationToken);
//			localGitRepoManager.checkout(tagsetVersion);
//			localGitRepoManager.commit(
//				// TODO: get the new markup collection commit hash, replacing "?" below
//				// LocalGitRepositoryManager should probably always return the new hash when commit or addAndCommit are
//				// called
//				String.format("Updating markup collection submodule %s to version %s", markupCollectionId, "?"),
//				StringUtils.isNotBlank(gitLabUser.getName()) ? gitLabUser.getName() : gitLabUser.getUsername(),
//				gitLabUser.getEmail()
//			);

			localGitRepoManager.open(projectRepoName);
			((LocalGitRepositoryManager)localGitRepoManager).getGitApi().submoduleUpdate().call();
		}
		catch (MarkupCollectionHandlerException|LocalGitRepositoryManagerException|GitAPIException e) {
			throw new ProjectHandlerException("Failed to add tagset to markup collection", e);
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
			repoManager.open(getProjectRepoName(projectId));

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
