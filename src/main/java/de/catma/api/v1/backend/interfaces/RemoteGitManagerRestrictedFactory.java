package de.catma.api.v1.backend.interfaces;

import java.io.IOException;

import de.catma.oauth.GitLabOauthTokenProvider;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

public interface RemoteGitManagerRestrictedFactory {

	RemoteGitManagerRestricted create(String backendToken) throws IOException;

	RemoteGitManagerRestricted create(GitLabOauthTokenProvider oauthTokenProvider) throws IOException;

}
