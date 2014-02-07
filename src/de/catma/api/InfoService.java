package de.catma.api;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class InfoService extends ServerResource {

	@Get
	public String info() {
		return "test";
	}
}
