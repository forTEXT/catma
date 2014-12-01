package de.catma.api.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import de.catma.api.crypto.TokenGenerator;
import de.catma.api.crypto.TokenGeneratorName;
import de.catma.backgroundservice.DefaultBackgroundServiceProvider;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryManager;
import de.catma.document.repository.RepositoryReference;
import de.catma.tag.TagManager;

public class RepositoryLoader {

	public Repository open(Properties properties, String identifier) throws Exception {
		InitialContext context = new InitialContext();
		TokenGenerator tokenGenerator = 
				(TokenGenerator) context.lookup(TokenGeneratorName.TOKENGENERATOR.name());

		// strip of timestamp
		identifier = tokenGenerator.getIdentifier(identifier);
		
		TagManager tagManager = new TagManager();
		RepositoryManager repoManager = new RepositoryManager(
				new DefaultBackgroundServiceProvider(), 
				tagManager, properties);
		if (!repoManager.getRepositoryReferences().isEmpty()) {
			RepositoryReference repoRef = 
				repoManager.getRepositoryReferences().iterator().next();
			Map<String,String> userIdentification = 
					new HashMap<String, String>(1);
			userIdentification.put("user.ident", identifier);
			
			Repository repo = repoManager.openRepository(repoRef, userIdentification);
			return repo;
		}
		else {
			throw new ResourceException(
				Status.CLIENT_ERROR_NOT_FOUND, "no repository configured");
		}
	}
}
