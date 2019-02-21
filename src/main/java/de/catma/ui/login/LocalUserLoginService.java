package de.catma.ui.login;

import java.io.IOException;
import java.util.Optional;

import com.vaadin.server.VaadinSession;

import de.catma.repository.git.GitUser;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.util.Pair;

public class LocalUserLoginService implements LoginService {
	
	@Override
	public IRemoteGitManagerRestricted login(String username, String password) throws IOException {
		String user = System.getProperty("user.name");
		Pair<GitUser, String> userTokenPair = new GitlabManagerPrivileged().acquireImpersonationToken(
				user, "catma", user+"@catma.de",user);
		IRemoteGitManagerRestricted api = GitlabManagerRestricted.fromToken(userTokenPair.getFirst(),userTokenPair.getSecond());
		VaadinSession.getCurrent().setAttribute(IRemoteGitManagerRestricted.class, api);

		return api;
	}

	@Override
	public IRemoteGitManagerRestricted loggedInFromThirdParty(String identifier, String provider, String email, String name) throws IOException {
		IRemoteGitManagerRestricted api = GitlabManagerRestricted.gitlabLogin("", "");
		VaadinSession.getCurrent().setAttribute(IRemoteGitManagerRestricted.class, api);

		return api;
	}

	@Override
	public Optional<IRemoteGitManagerRestricted> getAPI() {
		IRemoteGitManagerRestricted api = VaadinSession.getCurrent().getAttribute(IRemoteGitManagerRestricted.class);
		if(api == null){
			try {
				api = login("","");
			} catch (IOException e) {
				api = null; //NOOP 
			}
		}
		return Optional.ofNullable(api);
	}

	@Override
	public void logout() {
		//NOOP
	}
}
