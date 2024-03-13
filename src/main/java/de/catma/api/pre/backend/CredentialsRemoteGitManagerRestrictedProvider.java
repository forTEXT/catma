package de.catma.api.pre.backend;

import java.io.IOException;

import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedProvider;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

public class CredentialsRemoteGitManagerRestrictedProvider implements RemoteGitManagerRestrictedProvider {
	
	private final String username;
	private final String password;
	private final RemoteGitManagerRestrictedFactory remoteGitMangerRestrictedFactory;
	

	public CredentialsRemoteGitManagerRestrictedProvider(String username, String password, RemoteGitManagerRestrictedFactory remoteGitMangerRestrictedFactory) {
		super();
		this.username = username;
		this.password = password;
		this.remoteGitMangerRestrictedFactory = remoteGitMangerRestrictedFactory;
	}

	@Override
	public RemoteGitManagerRestricted createRemoteGitManagerRestricted() throws IOException {
		return remoteGitMangerRestrictedFactory.create(username, password);
	}

}
