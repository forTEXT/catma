package de.catma.api.pre;

import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import de.catma.api.pre.backend.GitlabManagerPrivilegedFactory;
import de.catma.api.pre.backend.GitlabManagerRestrictedFactory;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerPrivilegedFactory;
import de.catma.api.pre.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.pre.cache.ProjectCache;
import de.catma.api.pre.cache.RemoteGitManagerRestrictedProviderCache;
import de.catma.api.pre.oauth.DefaultHttpClientFactory;
import de.catma.api.pre.oauth.GoogleOauthHandler;
import de.catma.api.pre.oauth.HttpServletRequestSessionStorageHandler;
import de.catma.api.pre.oauth.interfaces.HttpClientFactory;
import de.catma.api.pre.oauth.interfaces.OauthHandler;
import de.catma.api.pre.oauth.interfaces.SessionStorageHandler;

@Provider
public class PreApiInjectionBinder extends AbstractBinder {

	@Override
	protected void configure() {
		// singletons
		bind(ProjectCache.class).to(ProjectCache.class).in(Singleton.class);
		bind(RemoteGitManagerRestrictedProviderCache.class).to(RemoteGitManagerRestrictedProviderCache.class).in(Singleton.class);
		
		// per request, can be overwritten e.g. for testing purposes with a higher rank like .ranked(2)
		bind(GitlabManagerRestrictedFactory.class).to(RemoteGitManagerRestrictedFactory.class).ranked(1);
		bind(GitlabManagerPrivilegedFactory.class).to(RemoteGitManagerPrivilegedFactory.class).ranked(1);
		bind(GoogleOauthHandler.class).to(OauthHandler.class).ranked(1);
		bind(DefaultHttpClientFactory.class).to(HttpClientFactory.class).ranked(1);
		bind(HttpServletRequestSessionStorageHandler.class).to(SessionStorageHandler.class).ranked(1);
	}

}
