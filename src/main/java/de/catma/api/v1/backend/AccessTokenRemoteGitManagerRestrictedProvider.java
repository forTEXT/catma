package de.catma.api.v1.backend;

import java.io.IOException;

import de.catma.api.v1.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.v1.backend.interfaces.RemoteGitManagerRestrictedProvider;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

public class AccessTokenRemoteGitManagerRestrictedProvider implements RemoteGitManagerRestrictedProvider {
	
	private final String accessToken;
	private final RemoteGitManagerRestrictedFactory remoteGitMangerRestrictedFactory;
	
	public AccessTokenRemoteGitManagerRestrictedProvider(String accessToken, RemoteGitManagerRestrictedFactory remoteGitMangerRestrictedFactory) {
		super();
		this.accessToken = accessToken;
		this.remoteGitMangerRestrictedFactory = remoteGitMangerRestrictedFactory;
	}

	@Override
	public RemoteGitManagerRestricted createRemoteGitManagerRestricted() throws IOException {
		// Note: this creates a new instance of RemoteGitManagerRestricted for each request, that is intended
		// as the RemoteGitManagerRestricted implementation is not threadsafe and its usage is not protected by locks
		return remoteGitMangerRestrictedFactory.create(accessToken);
	}

}
