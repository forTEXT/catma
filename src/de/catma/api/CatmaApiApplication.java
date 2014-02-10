package de.catma.api;

import java.io.FileInputStream;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.MapVerifier;

import de.catma.api.service.CorpusExport;
import de.catma.api.service.CorpusList;
import de.catma.api.service.SourceDocumentExport;
import de.catma.api.service.UserMarkupCollectionExport;

public class CatmaApiApplication extends Application {
	
	public CatmaApiApplication() {
		System.out.println("CREATE");
	}
	
	@Override
	public synchronized Restlet createInboundRoot() {
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
			
			mapVerifier.getLocalSecrets().put("heureclea", "secret".toCharArray());
			guard.setVerifier(mapVerifier);
			
			Router router = new Router(getContext());
	
	        // Defines only one route
	        router.attach("/info", InfoService.class);
	        
	        router.attach("/corpus/list", CorpusList.class);
	        
	        router.attach("/corpus/get", CorpusExport.class);
	        
	        router.attach("/src/get", SourceDocumentExport.class);
	        
	        router.attach("/umc/get", UserMarkupCollectionExport.class);
	        
			guard.setNext(router);
			
	        
	        return guard;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
