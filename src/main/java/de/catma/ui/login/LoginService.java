package de.catma.ui.login;

import de.catma.oauth.GitLabOauthTokenProvider;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

import java.io.IOException;

public interface LoginService {
	void loggedInFromGitLabOauth(GitLabOauthTokenProvider oauthTokenProvider) throws IOException;
	void login(String personalAccessToken) throws IOException;
	void logout();
	RemoteGitManagerRestricted getRemoteGitManagerRestricted();
	void close();
}
