package de.catma.api.v1;

import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import de.catma.api.v1.backend.GitlabManagerPrivilegedFactory;
import de.catma.api.v1.backend.GitlabManagerRestrictedFactory;
import de.catma.api.v1.backend.interfaces.RemoteGitManagerPrivilegedFactory;
import de.catma.api.v1.backend.interfaces.RemoteGitManagerRestrictedFactory;
import de.catma.api.v1.cache.CollectionAnnotationCountCache;
import de.catma.api.v1.cache.ProjectExportSerializerCache;
import de.catma.api.v1.cache.RemoteGitManagerRestrictedProviderCache;
import de.catma.api.v1.oauth.DefaultHttpClientFactory;
import de.catma.api.v1.oauth.HttpServletRequestSessionStorageHandler;
import de.catma.api.v1.oauth.interfaces.HttpClientFactory;
import de.catma.api.v1.oauth.interfaces.SessionStorageHandler;

@Provider
public class ApiInjectionBinder extends AbstractBinder {

	@Override
	protected void configure() {
		// singletons
		bind(ProjectExportSerializerCache.class).to(ProjectExportSerializerCache.class).in(Singleton.class);
		bind(CollectionAnnotationCountCache.class).to(CollectionAnnotationCountCache.class).in(Singleton.class);
		bind(RemoteGitManagerRestrictedProviderCache.class).to(RemoteGitManagerRestrictedProviderCache.class).in(Singleton.class);
		
		// per request, can be overwritten e.g. for testing purposes with a higher rank like .ranked(2)
		bind(GitlabManagerRestrictedFactory.class).to(RemoteGitManagerRestrictedFactory.class).ranked(1);
		bind(GitlabManagerPrivilegedFactory.class).to(RemoteGitManagerPrivilegedFactory.class).ranked(1);
		bind(DefaultHttpClientFactory.class).to(HttpClientFactory.class).ranked(1);
		bind(HttpServletRequestSessionStorageHandler.class).to(SessionStorageHandler.class).ranked(1);
	}

}
