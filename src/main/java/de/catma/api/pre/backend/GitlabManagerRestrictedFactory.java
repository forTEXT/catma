package de.catma.api.pre.backend;

import java.io.IOException;

import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

public class GitlabManagerRestrictedFactory implements RemoteGitManagerRestrictedFactory {

	@Override
	public RemoteGitManagerRestricted create(String backendToken) throws IOException {
		return new GitlabManagerRestricted(backendToken);
	}

	@Override
	public RemoteGitManagerRestricted create(String username, String password) throws IOException {
		return new GitlabManagerRestricted(username, password);
	}

}
