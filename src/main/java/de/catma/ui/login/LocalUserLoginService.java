package de.catma.ui.login;

import java.io.IOException;

import com.vaadin.server.VaadinSession;

import de.catma.repository.git.GitUser;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.ui.di.RemoteGitManagerFactory;
import de.catma.util.Pair;

public class LocalUserLoginService implements LoginService {

	private RemoteGitManagerRestricted api;
	
	
	private final RemoteGitManagerFactory remoteGitManagerFactory;
		
	public LocalUserLoginService(RemoteGitManagerFactory remoteGitManagerFactory) {
		this.remoteGitManagerFactory = remoteGitManagerFactory;
	}
	
	@Override
	public RemoteGitManagerRestricted login(String username, String password) throws IOException {
		String user = System.getProperty("user.name");
		Pair<GitUser, String> userTokenPair = new GitlabManagerPrivileged().acquireImpersonationToken(
				user, "catma", user+"@catma.de",user);
		api = remoteGitManagerFactory.createFromImpersonationToken(userTokenPair.getSecond());
		return api;
	}

	@Override
	public RemoteGitManagerRestricted loggedInFromThirdParty(String identifier, String provider, String email, String name) throws IOException {
		RemoteGitManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
        Pair<GitUser, String> userAndToken = 
        		gitlabManagerPrivileged.acquireImpersonationToken(identifier, provider, email, name);

		api = remoteGitManagerFactory.createFromImpersonationToken(userAndToken.getSecond());
		return api;
	}

	@Override
	public RemoteGitManagerRestricted login(String personalAccessToken) throws IOException {
		api = remoteGitManagerFactory.createFromImpersonationToken(personalAccessToken);
		return api;
	}
	
	
	@Override
	public RemoteGitManagerRestricted getAPI() {
		if(this.api == null){
			try {
				api = login("","");
			} catch (IOException e) {
				api = null; //NOOP 
			}
		}
		return api;
	}

	@Override
	public void logout() {
		VaadinSession.getCurrent().close();
    	VaadinSession.getCurrent().getSession().invalidate();
	}

	@Override
	public void close() {
		api = null;
	}
}
