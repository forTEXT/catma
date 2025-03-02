package de.catma.api.pre.backend;

import de.catma.api.pre.backend.interfaces.RemoteGitManagerPrivilegedFactory;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;

public class GitlabManagerPrivilegedFactory implements RemoteGitManagerPrivilegedFactory {

	@Override
	public RemoteGitManagerPrivileged create() {
		return new GitlabManagerPrivileged();
	}

}
