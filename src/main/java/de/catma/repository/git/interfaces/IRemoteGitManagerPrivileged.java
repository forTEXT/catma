package de.catma.repository.git.interfaces;

import java.io.IOException;
import java.time.LocalDate;

import de.catma.repository.git.GitUser;
import de.catma.util.Pair;

/**
 * Privileged operations for user management
 * 
 * @author db
 *
 */
public interface IRemoteGitManagerPrivileged extends ICommonRemoteGitManager {

	
	/**
	 * Acquires (gets or creates) a GitLab impersonation token for the supplied
	 * <code>catmaUser</code>.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param identifier unique identifier
	 * @param email email address
	 * @param name name
	 * @return a {@link Pair} object with the first value being a {@link GitUser} object and the second
	 *         value being the raw impersonation token string
	 * @throws IOException if something went wrong while acquiring the GitLab
	 *         impersonation token
	 */
	Pair<GitUser, String> acquireImpersonationToken(String identifier, String provider, String email, String name) throws IOException;

	/**
	 * Creates a new personal access token for the GitLab user identified by <code>userId</code>, with a default scope
	 * of <code>Scope.API</code>.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param userId the ID of the user for which to create the personal access token
	 * @param tokenName the name of the personal access token to create
	 * @param expiresAt the expiry date of the personal access token
	 * @return the new token
	 * @throws IOException if something went wrong while creating the personal access token
	 */
	public String createPersonalAccessToken(Long userId, String tokenName, LocalDate expiresAt) throws IOException;

	/**
	 * Creates a new remote user.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param email the email address of the user to create
	 * @param username the username of the user to create
	 * @param password the password of the user to create. A random, 12 character password will be
	 *                 generated if none is supplied.
	 * @param name the name of the user to create
	 * @return the new user ID
	 * @throws IOException if something went wrong while creating the remote
	 *         user
	 */
	Long createUser(String email, String username, String password, String name) throws IOException;

	/**
	 * Changes a user 
	 * @param userId
	 * @param name
	 * @param password
	 * @throws IOException
	 */
	void modifyUserAttributes(Long userId, String name, String password) throws IOException;

}
