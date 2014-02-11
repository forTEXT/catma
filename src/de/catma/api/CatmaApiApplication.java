package de.catma.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Reference;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.MapVerifier;

import de.catma.api.service.ApiInfo;
import de.catma.api.service.CorpusExport;
import de.catma.api.service.CorpusList;
import de.catma.api.service.SourceDocumentExport;
import de.catma.api.service.UserMarkupCollectionExport;
import de.catma.api.service.UserMarkupCollectionImport;
import de.catma.util.CloseSafe;

public class CatmaApiApplication extends Application {
	public CatmaApiApplication() {
		this.getStatusService().setHomeRef(new Reference("http://www.catma.de"));
	}
	
	@Override
	public Restlet createInboundRoot() {
		try {
			ServletContext servletContext = 
			(ServletContext)getContext().getAttributes().get(
					"org.restlet.ext.servlet.ServletContext");
	
			Properties properties = new Properties();
			properties.load(new FileInputStream(
					servletContext.getRealPath("catma.properties")));
			
			getContext().getAttributes().put(
				Parameter.catma_properties.name(), properties);
	
			ChallengeAuthenticator guard = 
					new ChallengeAuthenticator(
						getContext(), 
						ChallengeScheme.HTTP_BASIC, 
						"catma_realm");
			
			
			MapVerifier mapVerifier = new MapVerifier();
			loadValidApiUsers(
				mapVerifier, servletContext.getRealPath("apiusers.json"));
			guard.setVerifier(mapVerifier);
			
			Router router = new Router(getContext());
	
	        router.attach("/corpus/list", CorpusList.class);
	        
	        router.attach("/corpus/get", CorpusExport.class);
	        
	        router.attach("/src/get", SourceDocumentExport.class);
	        
	        router.attach("/umc/get", UserMarkupCollectionExport.class);
	        
	        router.attach("/umc/add", UserMarkupCollectionImport.class);
	        
	        router.attach("/info", ApiInfo.class);
	        
			guard.setNext(router);
			
	        return guard;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void loadValidApiUsers(MapVerifier mapVerifier, String apiUsersFilePath) 
					throws IOException, JSONException {
		File apiUsersFile = new File(apiUsersFilePath);
		if (apiUsersFile.exists()) {
			FileInputStream fis = new FileInputStream(apiUsersFile);
			try {
				String apiUsersJson = IOUtils.toString(fis, "UTF-8");
				JSONArray apiUsersList = new JSONArray(apiUsersJson);
				
				for (int i=0; i<apiUsersList.length(); i++) {
					JSONObject entry = apiUsersList.getJSONObject(i);
					mapVerifier.getLocalSecrets().put(
						entry.getString("u"), 
						entry.getString("p").toCharArray());
				}
		
			}
			finally {
				CloseSafe.close(fis);
			}
		}
	}

}
