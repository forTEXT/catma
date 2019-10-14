package de.catma.ui.di;

import java.io.IOException;

import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;

/**
 * Factory that builds an IRemoteGitManagerRestricted base on login method.
 * 
 * @author db
 *
 */
public interface IRemoteGitManagerFactory {

	IRemoteGitManagerRestricted createFromToken(
			String userImpersonationToken) throws IOException;

	IRemoteGitManagerRestricted createFromUsernameAndPassword(
			String username, 
			String password) throws IOException;

}
