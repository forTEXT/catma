package de.catma.ui.login;

import java.io.IOException;

import com.vaadin.server.VaadinSession;

import de.catma.repository.git.GitUser;
import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.ui.di.IRemoteGitManagerFactory;
import de.catma.util.Pair;

/**
 * This implementation asks a gitlab server and caches the API afterwards in session
 * 
 * @author db
 *
 */
public class GitlabLoginService implements LoginService {

	private IRemoteGitManagerRestricted api;
	
	private final IRemoteGitManagerFactory iRemoteGitManagerFactory;
		
	public GitlabLoginService(IRemoteGitManagerFactory iRemoteGitManagerFactory) {
		this.iRemoteGitManagerFactory=iRemoteGitManagerFactory;
	}
	
	@Override
	public IRemoteGitManagerRestricted login(String username, String password) throws IOException {
		api = iRemoteGitManagerFactory.createFromUsernameAndPassword(username, password);
		return api;
	}

	@Override
	public IRemoteGitManagerRestricted login(String personalAccessToken) throws IOException {
		api = iRemoteGitManagerFactory.createFromImpersonationToken(personalAccessToken);
		return api;
	}

	@Override
	public IRemoteGitManagerRestricted loggedInFromThirdParty(String identifier, String provider, String email, String name) throws IOException {
		IRemoteGitManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();		
        Pair<GitUser, String> userAndToken = 
        		gitlabManagerPrivileged.acquireImpersonationToken(identifier, provider, email, name);

		api = iRemoteGitManagerFactory.createFromImpersonationToken(userAndToken.getSecond());
		return api;
	}

	@Override
	public IRemoteGitManagerRestricted getAPI() {
		return api;
	}


	@Override
	public void logout() {
		VaadinSession.getCurrent().close();
		api = null;
    	VaadinSession.getCurrent().getSession().invalidate();
	}
	
}
