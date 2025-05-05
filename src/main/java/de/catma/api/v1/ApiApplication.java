package de.catma.api.v1;

import org.glassfish.jersey.server.ResourceConfig;

public class ApiApplication extends ResourceConfig {
	public ApiApplication() {
		this.register(new ApiInjectionBinder());
	}
}
