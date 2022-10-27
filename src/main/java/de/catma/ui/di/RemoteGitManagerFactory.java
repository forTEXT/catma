package de.catma.ui.di;

import java.io.IOException;

import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;

/**
 * Factory that builds a RemoteGitManagerRestricted based on login method.
 * 
 * @author db
 *
 */
public interface RemoteGitManagerFactory {

	RemoteGitManagerRestricted createFromImpersonationToken(
			String userImpersonationToken) throws IOException;

	RemoteGitManagerRestricted createFromUsernameAndPassword(
			String username, 
			String password) throws IOException;

}
