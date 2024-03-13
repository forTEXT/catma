package de.catma.api.pre;

import org.glassfish.jersey.server.ResourceConfig;

public class PreApplication extends ResourceConfig {
	
	public static final String API_PACKAGE = "pre"; // project resource export
	public static final String API_VERSION = "v1";

	public PreApplication() {
		this.register(new PreApiInjectionBinder());
	}

}
