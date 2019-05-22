package de.catma.user;

import de.catma.rbac.RBACRole;

public interface Member extends User {

	/**
	 * 
	 * @return the current {@link RBACRole}
	 */
	RBACRole getRole();
}
