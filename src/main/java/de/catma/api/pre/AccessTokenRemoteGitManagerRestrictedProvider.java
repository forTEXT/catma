package de.catma.api.pre;

import java.io.IOException;

import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

public class AccessTokenRemoteGitManagerRestrictedProvider implements RemoteGitManagerRestrictedProvider {
	
	private final String accessToken;
	
	public AccessTokenRemoteGitManagerRestrictedProvider(String accessToken) {
		super();
		this.accessToken = accessToken;
	}

	@Override
	public RemoteGitManagerRestricted createRemoteGitManagerRestricted() throws IOException {
		return new GitlabManagerRestricted(accessToken);
	}

}
