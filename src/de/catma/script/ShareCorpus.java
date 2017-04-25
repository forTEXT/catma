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

public class ShareCorpus extends AbstractScript {

	
	public static void main(String[] args) {
		try {
			new ShareCorpus().run(args);
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
		
		String corpusId = args[4];
		String accessMode = args[5];
		
		StringBuilder urlBuilder = new StringBuilder(baseURL);

		urlBuilder.append("corpus/share?");
		urlBuilder.append("cid="+corpusId);
		urlBuilder.append("&accessmode="+accessMode);
		urlBuilder.append("&userident=");
		
		for (String userIdent : userIdents) {
			ClientResource client = 
					new ClientResource(Context.getCurrent(), Method.GET, urlBuilder.toString() + URLEncoder.encode(userIdent, "UTF-8"));
			
			client.setChallengeResponse(ChallengeScheme.HTTP_BASIC, user, pass);
			client.get(); 
			
			logger.info("shared with user: " + userIdent);
		}
		
	}
}
