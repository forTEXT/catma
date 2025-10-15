package de.catma.ui.login;

import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

import java.io.IOException;

public interface LoginService {
	void login(String username, String password) throws IOException;
	void login(String personalAccessToken) throws IOException;
	void loggedInFromThirdParty(String identifier, String provider, String email, String name) throws IOException;
	void logout();
	RemoteGitManagerRestricted getRemoteGitManagerRestricted();
	void close();
}
