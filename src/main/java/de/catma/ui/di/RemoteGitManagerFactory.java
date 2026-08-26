package de.catma.ui.di;

import de.catma.oauth.GitLabOauthTokenProvider;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

import java.io.IOException;

/**
 * Factory that builds a RemoteGitManagerRestricted based on login method.
 */
public interface RemoteGitManagerFactory {
	RemoteGitManagerRestricted createFromImpersonationToken(String userImpersonationToken) throws IOException;

	RemoteGitManagerRestricted createFromOauthTokenProvider(GitLabOauthTokenProvider oauthTokenProvider) throws IOException;
}
