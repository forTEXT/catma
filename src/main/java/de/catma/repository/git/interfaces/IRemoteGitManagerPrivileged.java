package de.catma.repository.git.interfaces;

import java.io.IOException;

import de.catma.repository.git.GitUser;
import de.catma.util.Pair;

/**
 * Privileged operations for user management
 * 
 * @author db
 *
 */
public interface IRemoteGitManagerPrivileged {

	/**
	 * checks if a given User or Email exists
	 * @param usernameOrEmail
	 * @return
	 * @throws IOException
	 */
	boolean existsUserOrEmail(String usernameOrEmail) throws IOException;
	
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
	 * Creates a new remote user.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param email the email address of the user to create
	 * @param username the username of the user to create
	 * @param password the password of the user to create. A random, 12 character password will be
	 *                 generated if none is supplied.
	 * @param name the name of the user to create
	 * @param isAdmin whether the user to create should be an admin or not. Defaults to false if not
	 *                supplied.
	 * @return the new user ID
	 * @throws IOException if something went wrong while creating the remote
	 *         user
	 */
	int createUser(String email, String username, String password, String name, Boolean isAdmin) throws IOException;

}
