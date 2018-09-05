package de.catma.api.service;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class ApiInfo extends ServerResource {
	
	@Get
	public String getApiInfo() {
		try {
			return IOUtils.toString(
					ApiInfo.class.getClassLoader().getResourceAsStream(
							"de/catma/api/catma_api_spec.txt"));
		} catch (IOException e) {
			throw new ResourceException(
					Status.SERVER_ERROR_INTERNAL, "service implementation error");
		}
	}
}
