package de.catma.repository.git;

import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.MarkupCollectionHandlerException;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IMarkupCollectionHandler;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.models.HeaderBase;
import de.catma.repository.git.serialization.models.MarkupCollectionHeader;
import de.catma.util.IDGenerator;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.models.User;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class MarkupCollectionHandler implements IMarkupCollectionHandler {
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	public MarkupCollectionHandler(ILocalGitRepositoryManager localGitRepositoryManager,
								   IRemoteGitServerManager remoteGitServerManager) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
	}

	/**
	 * Creates a new markup collection.
	 *
	 * @param name the name of the new markup collection
	 * @param description the description of the new markup collection
	 * @param sourceDocumentId the ID of the source document to which the new markup collection relates
	 * @param projectId the ID of the project within which the new markup collection must be created
	 * @param markupCollectionId the ID of the new markup collection. If none is provided, a new ID will be
	 *                           generated.
	 * @return the <code>markupCollectionId</code> if one was provided, otherwise a new markup collection ID
	 * @throws MarkupCollectionHandlerException if an error occurs while creating the markup collection
	 */
	@Override
	public String create(String name, String description, String sourceDocumentId, String projectId,
						 @Nullable String markupCollectionId)
			throws MarkupCollectionHandlerException {
		if (markupCollectionId == null) {
			IDGenerator idGenerator = new IDGenerator();
			markupCollectionId = idGenerator.generate();
		}

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the markup collection repository
			IRemoteGitServerManager.CreateRepositoryResponse createRepositoryResponse =
					this.remoteGitServerManager.createRepository(
						markupCollectionId, markupCollectionId, projectId
					);

			// clone the repository locally
			RemoteGitServerManager remoteGitServerManagerImpl =
					(RemoteGitServerManager)this.remoteGitServerManager;
			User gitLabUser = remoteGitServerManagerImpl.getGitLabUser();
			String gitLabUserImpersonationToken = remoteGitServerManagerImpl
					.getGitLabUserImpersonationToken();

			localGitRepoManager.clone(
				createRepositoryResponse.repositoryHttpUrl,
				null,
				gitLabUser.getUsername(),
				gitLabUserImpersonationToken
			);

			// write header.json into the local repo
			File targetHeaderFile = new File(
				localGitRepoManager.getRepositoryWorkTree(), "header.json"
			);
			MarkupCollectionHeader header = new MarkupCollectionHeader(name, description, sourceDocumentId);
			String serializedHeader = new SerializationHelper<MarkupCollectionHeader>().serialize(header);
			localGitRepoManager.addAndCommit(
				targetHeaderFile, serializedHeader.getBytes(StandardCharsets.UTF_8),
				StringUtils.isNotBlank(gitLabUser.getName()) ? gitLabUser.getName() : gitLabUser.getUsername(),
				gitLabUser.getEmail()
			);
		}
		catch (RemoteGitServerManagerException|LocalGitRepositoryManagerException e) {
			throw new MarkupCollectionHandlerException("Failed to create markup collection", e);
		}

		return markupCollectionId;
	}

	@Override
	public void delete(String markupCollectionId) throws MarkupCollectionHandlerException {
		throw new MarkupCollectionHandlerException("Not implemented");
	}

	@Override
	public void addTagset(String tagsetId, String markupCollectionId) throws MarkupCollectionHandlerException {
		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// open the tagset repository so that we can get its remote
			localGitRepoManager.open(tagsetId);
			String tagsetRemoteUri = localGitRepoManager.getRemoteUrl(null);
			localGitRepoManager.close();

			// open the markup collection repository
			localGitRepoManager.open(markupCollectionId);

			// create the submodule
			File targetSubmodulePath = Paths.get(
				localGitRepoManager.getRepositoryWorkTree().toString(), "tagsets", tagsetId
			).toFile();

			RemoteGitServerManager remoteGitServerManagerImpl = (RemoteGitServerManager)this.remoteGitServerManager;
			String gitLabUserImpersonationToken = remoteGitServerManagerImpl.getGitLabUserImpersonationToken();

			localGitRepoManager.addSubmodule(
				targetSubmodulePath, tagsetRemoteUri,
				remoteGitServerManagerImpl.getGitLabUser().getUsername(), gitLabUserImpersonationToken
			);
		}
		catch (LocalGitRepositoryManagerException e) {
			throw new MarkupCollectionHandlerException("Failed to add tagset", e);
		}
	}

	@Override
	public void removeTagset(String tagsetId, String markupCollectionId) throws MarkupCollectionHandlerException {
		// it should only be possible to remove a tagset if there are no tag instances referring to any of its tag
		// definitions
		throw new MarkupCollectionHandlerException("Not implemented");
	}
}
