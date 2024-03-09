package de.catma.api.pre;

import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

@Provider
public class ProjectCacheBinder extends AbstractBinder {

	@Override
	protected void configure() {
		bind(ProjectCache.class).to(ProjectCache.class).in(Singleton.class);

	}

}
