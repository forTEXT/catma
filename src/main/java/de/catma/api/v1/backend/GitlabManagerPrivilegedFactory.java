package de.catma.api.v1.backend;

import de.catma.api.v1.backend.interfaces.RemoteGitManagerPrivilegedFactory;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;

public class GitlabManagerPrivilegedFactory implements RemoteGitManagerPrivilegedFactory {

	@Override
	public RemoteGitManagerPrivileged create() {
		return new GitlabManagerPrivileged();
	}

}
