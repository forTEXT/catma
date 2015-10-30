package de.catma.heureclea.autotagger;

import java.io.IOException;
import java.net.URLEncoder;

import org.restlet.Context;
import org.restlet.data.Method;
import org.restlet.resource.ClientResource;

import de.catma.document.repository.RepositoryPropertyKey;

public class AnnotationGenerator {

	private enum Parameter {
		cid, 
		tid, 
		id, 
		token, 
		api,
		sid,
		;
		
		public String asParam() {
			return "&"+name()+"=";
		}
		public String asInitialParam() {
			return "?"+name()+"=";
		}
	}

	private String generatorURL;

	public AnnotationGenerator() {
		this.generatorURL = 
			RepositoryPropertyKey.AnnotationGeneratorURL.getValue();
	}

	public void generate(
			String corpusId, TagsetIdentification tagsetIdentification, 
			String identifier, String token, String apiURL, String sourceDocId) throws IOException, InterruptedException {
		
		StringBuilder urlBuilder = new StringBuilder(generatorURL);
		urlBuilder.append(Parameter.cid.asInitialParam()); 
		urlBuilder.append(corpusId);
		urlBuilder.append(Parameter.tid.asParam()); 
		urlBuilder.append(tagsetIdentification.name());
		urlBuilder.append(Parameter.id.asParam()); 
		urlBuilder.append(URLEncoder.encode(identifier, "UTF-8"));
		urlBuilder.append(Parameter.token.asParam()); 
		urlBuilder.append(token);
		urlBuilder.append(Parameter.api.asParam());
		urlBuilder.append(URLEncoder.encode(apiURL, "UTF-8"));
		urlBuilder.append(Parameter.sid.asParam());
		urlBuilder.append(sourceDocId);
		
		ClientResource client = 
				new ClientResource(Context.getCurrent(), Method.GET, urlBuilder.toString());
		
		client.get(); //result is not of interest
	}
}
