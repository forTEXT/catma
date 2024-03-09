package de.catma.api.pre;

import org.glassfish.jersey.server.ResourceConfig;

public class PreApplication extends ResourceConfig {
	
	public PreApplication() {
		this.register(new ProjectCacheBinder());
	}

}
