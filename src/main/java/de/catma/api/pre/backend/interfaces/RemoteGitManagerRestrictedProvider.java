package de.catma.api.pre.backend.interfaces;

import java.io.IOException;

import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

public interface RemoteGitManagerRestrictedProvider {
	RemoteGitManagerRestricted createRemoteGitManagerRestricted() throws IOException;
}
