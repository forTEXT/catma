package de.catma.repository.git;

import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.exceptions.TagsetHandlerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.interfaces.ITagsetHandler;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.util.IDGenerator;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class TagsetHandler implements ITagsetHandler {
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	private final IDGenerator idGenerator;

	public TagsetHandler(ILocalGitRepositoryManager localGitRepositoryManager,
							 IRemoteGitServerManager remoteGitServerManager) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;

		this.idGenerator = new IDGenerator();
	}

	@Override
	public String create(String name, String description, String projectId) throws TagsetHandlerException {
		String tagsetId = idGenerator.generate();

		//TODO: write the name and description into the header.json file

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the tagset repository
			IRemoteGitServerManager.CreateRepositoryResponse response;

			response = this.remoteGitServerManager.createRepository(
					tagsetId, tagsetId, projectId
			);

			// clone the repository locally
			RemoteGitServerManager remoteGitServerManagerImpl =
					(RemoteGitServerManager)this.remoteGitServerManager;
			String gitLabUserImpersonationToken = remoteGitServerManagerImpl
					.getGitLabUserImpersonationToken();

			String authenticatedRepositoryUrl = GitLabAuthenticationHelper
					.buildAuthenticatedRepositoryUrl(
							response.repositoryHttpUrl, gitLabUserImpersonationToken
					);

			localGitRepoManager.clone(
					authenticatedRepositoryUrl,
					null,
					remoteGitServerManagerImpl.getGitLabUser().getUsername(),
					gitLabUserImpersonationToken
			);

			// write header.json into the local repo
			File targetHeaderFile = new File(
					localGitRepoManager.getRepositoryWorkTree(), "header.json"
			);

			byte[] headerBytes = "Serialized properties".getBytes(StandardCharsets.UTF_8);

			localGitRepoManager.add(
					targetHeaderFile, headerBytes
			);

			// commit newly added files
			String commitMessage = String.format("Adding %s", targetHeaderFile.getName());
			localGitRepoManager.commit(commitMessage);
		}
		catch (RemoteGitServerManagerException|LocalGitRepositoryManagerException|URISyntaxException e) {
			throw new TagsetHandlerException("Failed to create Tagset repo", e);
		}

		return tagsetId;
	}

	@Override
	public void delete(String tagsetId) throws TagsetHandlerException {

	}
}
