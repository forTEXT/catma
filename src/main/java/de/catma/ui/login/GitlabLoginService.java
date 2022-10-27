package de.catma.ui.login;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.vaadin.server.VaadinSession;

import de.catma.repository.git.GitUser;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.ui.di.RemoteGitManagerFactory;
import de.catma.util.Pair;

/**
 * This implementation asks a gitlab server and caches the API afterwards in session
 * 
 * @author db
 *
 */
public class GitlabLoginService implements LoginService {
	
	private static final AtomicInteger USER_COUNT = new AtomicInteger(0);
	
	private Logger logger = Logger.getLogger(this.getClass().getName());

	private RemoteGitManagerRestricted api;
	
	private final RemoteGitManagerFactory remoteGitManagerFactory;
		
	public GitlabLoginService(RemoteGitManagerFactory remoteGitManagerFactory) {
		this.remoteGitManagerFactory = remoteGitManagerFactory;
	}
	
	@Override
	public RemoteGitManagerRestricted login(String username, String password) throws IOException {
		api = remoteGitManagerFactory.createFromUsernameAndPassword(username, password);
		logLoginEvent("username/pwd");

		return api;
	}
	
	private void logLoginEvent(String authMethod) {
		if (api != null) {
			int count = USER_COUNT.incrementAndGet();
			logger.info(
				String.format(
					"User %1$s logged in by %2$s auth (users: %3$d)",
					api.getUser().preciseName(),
					authMethod,
					count));
		}	
	}

	private void logLogoutEvent() {
		if (api != null) {
			int count = USER_COUNT.decrementAndGet();
			logger.info(
				String.format(
					"User %1$s has been logged out (users: %2$d)",
					api.getUser().preciseName(),
					count));
		}
	}
	
	@Override
	public RemoteGitManagerRestricted login(String personalAccessToken) throws IOException {
		api = remoteGitManagerFactory.createFromImpersonationToken(personalAccessToken);
		logLoginEvent("token");
		return api;
	}

	@Override
	public RemoteGitManagerRestricted loggedInFromThirdParty(String identifier, String provider, String email, String name) throws IOException {
		RemoteGitManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
        Pair<GitUser, String> userAndToken = 
        		gitlabManagerPrivileged.acquireImpersonationToken(identifier, provider, email, name);

		api = remoteGitManagerFactory.createFromImpersonationToken(userAndToken.getSecond());
		logLoginEvent("third party");
		
		return api;
	}

	@Override
	public RemoteGitManagerRestricted getAPI() {
		return api;
	}

	@Override
	public void close() {
		logLogoutEvent();
		api = null;
	}

	@Override
	public void logout() {
		VaadinSession.getCurrent().close();
	}
	
}
