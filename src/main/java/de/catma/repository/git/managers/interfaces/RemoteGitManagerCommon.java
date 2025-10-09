package de.catma.repository.git.managers.interfaces;

import de.catma.rbac.RBACManager;

import java.io.IOException;

public interface RemoteGitManagerCommon extends RBACManager {
	boolean existsUserOrEmail(String usernameOrEmail) throws IOException;
}
