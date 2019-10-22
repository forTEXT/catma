package de.catma.ui.login;

import java.io.IOException;

import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;

public interface LoginService {
	
	/**
	 * Login to backend.
	 *  
	 * @param username
	 * @param password
	 * @return API
	 * @throws IOException
	 */
	IRemoteGitManagerRestricted login(String username, String password) throws IOException;
	
	/**
	 * Obtain an API with an already authenticated user from third party e.g. google
	 * 
	 * @param identifier
	 * @param provider
	 * @param email
	 * @param name
	 * @return API
	 * @throws IOException
	 */
	IRemoteGitManagerRestricted loggedInFromThirdParty(
			String identifier, String provider, String email, String name) throws IOException;
	
	/**
	 * It's implementation specific how to store the current API
	 * 
	 * @return current API
	 */
	IRemoteGitManagerRestricted getAPI();
	
	/**
	 * logout the current user
	 */
	void logout();
	
	
}
