package de.catma.user;

import java.time.LocalDate;

import de.catma.rbac.RBACRole;

public interface Member extends User, Comparable<Member> {

	/**
	 * 
	 * @return the current {@link RBACRole}
	 */
	RBACRole getRole();

	LocalDate getExpiresAt();
}
