package de.catma.repository.git.interfaces;

import java.io.IOException;

public interface ICommonRemoteGitManager {
	/**
	 * checks if a given User or Email exists
	 * @param usernameOrEmail
	 * @return
	 * @throws IOException
	 */
	boolean existsUserOrEmail(String usernameOrEmail) throws IOException;
}
