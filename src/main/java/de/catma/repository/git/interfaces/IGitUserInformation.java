package de.catma.repository.git.interfaces;

/**
 * Necessary git information
 * 
 * @author db
 *
 */
public interface IGitUserInformation {

	/**
	 * @return the gitlab username e.g. the identifier
	 */
	String getUsername();

	/**
	 * 
	 * @return the impersonation token
	 */
	String getPassword();

	/**
	 * 
	 * @return email 
	 */
	String getEmail();

}
