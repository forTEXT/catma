package de.catma.api.pre;

import java.io.IOException;

import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

public interface RemoteGitManagerRestrictedProvider {
	RemoteGitManagerRestricted createRemoteGitManagerRestricted() throws IOException;
}
