package de.catma.api.pre;

import java.io.IOException;

import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

public class CredentialsRemoteGitManagerRestrictedProvider implements RemoteGitManagerRestrictedProvider {
	
	private final String username;
	private final String password;
	
	

	public CredentialsRemoteGitManagerRestrictedProvider(String username, String password) {
		super();
		this.username = username;
		this.password = password;
	}



	@Override
	public RemoteGitManagerRestricted createRemoteGitManagerRestricted() throws IOException {
		return new GitlabManagerRestricted(username, password);
	}

}
