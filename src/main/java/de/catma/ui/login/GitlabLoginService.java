package de.catma.ui.login;

import com.vaadin.server.VaadinSession;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.ui.di.RemoteGitManagerFactory;
import de.catma.util.Pair;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Authenticates with the GitLab server and holds a reference to a corresponding {@link RemoteGitManagerRestricted}
 */
public class GitlabLoginService implements LoginService {
	private static final AtomicInteger USER_COUNT = new AtomicInteger(0);

	private final Logger logger = Logger.getLogger(GitlabLoginService.class.getName());

	private final RemoteGitManagerFactory remoteGitManagerFactory;

	private RemoteGitManagerRestricted remoteGitManagerRestricted;

	public GitlabLoginService(RemoteGitManagerFactory remoteGitManagerFactory) {
		this.remoteGitManagerFactory = remoteGitManagerFactory;
	}

	@Override
	public void login(String username, String password) throws IOException {
		remoteGitManagerRestricted = remoteGitManagerFactory.createFromUsernameAndPassword(username, password);
		logLoginEvent("username/password");
	}

	@Override
	public void login(String personalAccessToken) throws IOException {
		remoteGitManagerRestricted = remoteGitManagerFactory.createFromImpersonationToken(personalAccessToken);
		logLoginEvent("token");
	}

	@Override
	public void loggedInFromThirdParty(String identifier, String provider, String email, String name) throws IOException {
		RemoteGitManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
		Pair<GitUser, String> userAndToken = gitlabManagerPrivileged.acquireImpersonationToken(identifier, provider, email, name);
		remoteGitManagerRestricted = remoteGitManagerFactory.createFromImpersonationToken(userAndToken.getSecond());
		logLoginEvent("third party");
	}

	private void logLoginEvent(String authMethod) {
		if (remoteGitManagerRestricted == null) {
			return;
		}

		int count = USER_COUNT.incrementAndGet();

		logger.info(
				String.format(
						"User \"%1$s\" logged in with method \"%2$s\" (users: %3$d)",
						remoteGitManagerRestricted.getUser().preciseName(),
						authMethod,
						count
				)
		);
	}

	@Override
	public void logout() {
		VaadinSession.getCurrent().close();
	}

	private void logLogoutEvent() {
		if (remoteGitManagerRestricted == null) {
			return;
		}

		int count = USER_COUNT.decrementAndGet();

		logger.info(
				String.format(
						"User \"%s\" logged out (users: %d)",
						remoteGitManagerRestricted.getUser().preciseName(),
						count
				)
		);
	}

	@Override
	public RemoteGitManagerRestricted getRemoteGitManagerRestricted() {
		return remoteGitManagerRestricted;
	}

	@Override
	public void close() {
		logLogoutEvent();
		remoteGitManagerRestricted = null;
	}
}
