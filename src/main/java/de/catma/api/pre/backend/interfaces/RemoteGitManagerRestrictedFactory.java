package de.catma.api.pre.backend.interfaces;

import java.io.IOException;

import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

public interface RemoteGitManagerRestrictedFactory {

	RemoteGitManagerRestricted create(String backendToken) throws IOException;

	RemoteGitManagerRestricted create(String username, String password) throws IOException;

}
