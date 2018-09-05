package de.catma.api.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.restlet.data.ChallengeResponse;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import de.catma.api.ApiLoginToken;
import de.catma.api.Parameter;
import de.catma.document.repository.Repository;
import de.catma.repository.db.maintenance.UserManager;
import de.catma.user.UserProperty;

public class UserCreate extends ServerResource {

	@Get
	public String create() {
		try {
			Properties properties = 
				(Properties) getContext().getAttributes().get(
					Parameter.catma_properties.name());
			ChallengeResponse cr = getChallengeResponse();

			UserManager userManager = new UserManager();
			ApiLoginToken loginToken = new ApiLoginToken(cr.getIdentifier());
			Repository repo = 
					new RepositoryLoader().open(properties, cr.getIdentifier());
			try {
				Form form = getRequest().getResourceRef().getQueryAsForm();
				String identifier = 
						form.getFirstValue(Parameter.userident.name());
				
				if (identifier == null) {
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST, "identifier cannot be empty");
				}
				
				identifier = identifier.trim();
				
				if (identifier.isEmpty()) {
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST, "identifier cannot be empty");
				}
				
				Map<String,String> userIdentification = new HashMap<>();
				userIdentification.put(UserProperty.identifier.name(), identifier);
				
				repo.createIfAbsent(userIdentification);
				
				return identifier;
			}
			finally {
				userManager.logout(loginToken);
			}
		}
		catch (ResourceException re) {
			throw new ResourceException(re.getStatus(), re.getMessage(), re);
		}
		catch (Exception e) {
			throw new ResourceException(
					Status.SERVER_ERROR_INTERNAL, "service implementation error", e);
		}
	}
}
