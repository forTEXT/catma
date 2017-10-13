package de.catma.repository.git;

import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.interfaces.ITagsetHandler;

import javax.annotation.Nullable;

public class TagsetHandler implements ITagsetHandler {
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitServerManager remoteGitServerManager;

	public TagsetHandler(ILocalGitRepositoryManager localGitRepositoryManager,
							 IRemoteGitServerManager remoteGitServerManager) {
		this.localGitRepositoryManager = localGitRepositoryManager;
		this.remoteGitServerManager = remoteGitServerManager;
	}

	@Override
	public String create(String name, String description) {
		return null;
	}

	@Override
	public void delete(String tagsetId) {

	}
}
