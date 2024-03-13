package de.catma.api.pre.backend;

import java.io.IOException;

import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedProvider;
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
		return remoteGitMangerRestrictedFactory.create(accessToken);
	}

}
