package de.catma.repository.git;

import de.catma.repository.git.exceptions.LocalGitRepositoryManagerException;
import de.catma.repository.git.exceptions.RemoteGitServerManagerException;
import de.catma.repository.git.exceptions.TagsetHandlerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.interfaces.ITagsetHandler;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.util.IDGenerator;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.models.User;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class TagsetHandler implements ITagsetHandler {
	static final String TAGSET_ROOT_REPOSITORY_NAME_FORMAT = "%s_tagset";

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

			// TODO: nice name for gitlab
//			String tagsetNameAndPath = String.format(
//					this.TAGSET_ROOT_REPOSITORY_NAME_FORMAT, tagsetId
//			);
			response = this.remoteGitServerManager.createRepository(
					tagsetId, tagsetId, projectId
			);

			// clone the repository locally
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
			String committerName = StringUtils.isNotBlank(gitLabUser.getName()) ? gitLabUser.getName() : gitLabUser.getUsername();
			localGitRepoManager.commit(commitMessage, committerName, gitLabUser.getEmail());
		}
		catch (RemoteGitServerManagerException|LocalGitRepositoryManagerException e) {
			throw new TagsetHandlerException("Failed to create Tagset repo", e);
		}

		return tagsetId;
	}

	@Override
	public void delete(String tagsetId) throws TagsetHandlerException {
		throw new TagsetHandlerException("Not implemented");
	}
}
