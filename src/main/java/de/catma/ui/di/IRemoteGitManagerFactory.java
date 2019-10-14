package de.catma.ui.di;

import java.io.IOException;

import com.google.inject.assistedinject.Assisted;

import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;

/**
 * Factory that builds an IRemoteGitManagerRestricted base on login method.
 * 
 * @author db
 *
 */
public interface IRemoteGitManagerFactory {

	IRemoteGitManagerRestricted createFromToken(
			@Assisted("token") String userImpersonationToken) throws IOException;

	IRemoteGitManagerRestricted createFromUsernameAndPassword(
			@Assisted("username")String username, 
			@Assisted("password") String password) throws IOException;

}
