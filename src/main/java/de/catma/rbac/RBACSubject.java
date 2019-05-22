package de.catma.rbac;

import de.catma.user.User;

/**
 * Subject in the RBAC system. Usually this is implemented in {@link User} with the {@link #User.getUserId()} method
 *
 * @author db
 *
 */
public interface RBACSubject {
	
	/**
	 * Retrieves the identifier of a subject
	 * @return the identifier
	 */
	Integer getUserId();
}
