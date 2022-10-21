package de.catma.repository.git.managers.interfaces;

import java.io.IOException;

import de.catma.rbac.IRBACManager;

public interface ICommonRemoteGitManager extends IRBACManager {
	/**
	 * checks if a given User or Email exists
	 * @param usernameOrEmail
	 * @return
	 * @throws IOException
	 */
	boolean existsUserOrEmail(String usernameOrEmail) throws IOException;
}
