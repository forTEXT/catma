package de.catma.ui.login;

import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

import java.io.IOException;

public interface LoginService {
	RemoteGitManagerRestricted login(String username, String password) throws IOException;
	RemoteGitManagerRestricted login(String personalAccessToken) throws IOException;
	RemoteGitManagerRestricted loggedInFromThirdParty(String identifier, String provider, String email, String name) throws IOException;
	void logout();
	RemoteGitManagerRestricted getAPI();
	void close();
}
