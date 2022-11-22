package de.catma.repository.git.managers.interfaces;

import java.io.IOException;

import de.catma.rbac.IRBACManager;

public interface RemoteGitManagerCommon extends IRBACManager {
	/**
	 * checks if a given user or email exists
	 * @param usernameOrEmail
	 * @return
	 * @throws IOException
	 */
	boolean existsUserOrEmail(String usernameOrEmail) throws IOException;
}
