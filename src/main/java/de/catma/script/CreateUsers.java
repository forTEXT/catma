package de.catma.script;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

public class CreateUsers extends AbstractScript {

	
	public static void main(String[] args) {
		try {
			new CreateUsers().run(args);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String baseURL;
	private String user;
	private String pass;

	private void run(String[] args) throws IOException {
		baseURL = args[0];
		if (!baseURL.endsWith("/")) {
			baseURL += "/";
		}
		user = args[1];
		pass = args[2];
		
		Set<String> userIdents = new HashSet<>();
		loadFromFile(args[3], userIdents);
		
		StringBuilder urlBuilder = new StringBuilder(baseURL);

		urlBuilder.append("user/create?");
		urlBuilder.append("userident=");
		
		for (String userIdent : userIdents) {
			ClientResource client = 
					new ClientResource(Context.getCurrent(), Method.GET, urlBuilder.toString() + URLEncoder.encode(userIdent, "UTF-8"));
			
			client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, user, pass);
			Representation representation = client.get(); 
			ByteArrayOutputStream stringStream = new ByteArrayOutputStream();
					
			representation.write(stringStream);		
			
			logger.info("created or found user: " + stringStream.toString("UTF-8"));
		}
		
	}
}
