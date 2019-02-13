package de.catma.ui.login;

import java.io.IOException;
import java.util.Optional;

import com.vaadin.server.VaadinSession;

import de.catma.repository.git.GitUser;
import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.util.Pair;

/**
 * This implementation asks a gitlab server and caches the API afterwards in session
 * 
 * @author db
 *
 */
public class GitlabLoginService implements LoginService {

	@Override
	public IRemoteGitManagerRestricted login(String username, String password) throws IOException {
		IRemoteGitManagerRestricted api = GitlabManagerRestricted.gitlabLogin(username, password);
		VaadinSession.getCurrent().setAttribute(IRemoteGitManagerRestricted.class, api);
		return api;
	}


	@Override
	public IRemoteGitManagerRestricted loggedInFromThirdParty(String identifier, String provider, String email, String name) throws IOException {
		IRemoteGitManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();		
        Pair<GitUser, String> userAndToken = 
        		gitlabManagerPrivileged.acquireImpersonationToken(identifier, provider, email, name);

		IRemoteGitManagerRestricted api = GitlabManagerRestricted.fromToken(userAndToken.getFirst(), userAndToken.getSecond());
		VaadinSession.getCurrent().setAttribute(IRemoteGitManagerRestricted.class, api);

		return api;
	}

	@Override
	public Optional<IRemoteGitManagerRestricted> getAPI() {
		return Optional.ofNullable(VaadinSession.getCurrent().getAttribute(IRemoteGitManagerRestricted.class));
	}


	@Override
	public void logout() {
		VaadinSession.getCurrent().setAttribute(IRemoteGitManagerRestricted.class, null);		
	}
	
}
