package de.catma.api.pre.backend;

import java.io.IOException;

import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedProvider;
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
		// Note: this creates a new instance of RemoteGitManagerRestricted for each request, that is intended
		// as the RemoteGitManagerRestricted implementation is not threadsafe and its usage is not protected by locks

		return remoteGitMangerRestrictedFactory.create(username, password);
	}

}
