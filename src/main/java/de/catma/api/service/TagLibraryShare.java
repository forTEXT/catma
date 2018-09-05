package de.catma.api.service;

import java.io.IOException;
import java.util.Properties;

import org.restlet.data.ChallengeResponse;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import de.catma.api.ApiLoginToken;
import de.catma.api.Parameter;
import de.catma.document.AccessMode;
import de.catma.document.repository.Repository;
import de.catma.repository.db.maintenance.UserManager;
import de.catma.tag.TagLibraryReference;

public class TagLibraryShare extends ServerResource {

	@Get
	public void share() {
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
				
				String tagLibraryId = form.getFirstValue(Parameter.tid.name());
				
				AccessMode accessMode = 
					AccessMode.findValueOf(form.getFirstValue(Parameter.accessmode.name()));
				String identifier = 
					form.getFirstValue(Parameter.userident.name());
					
				if (accessMode == null) {
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST, "access mode is not valid");
				}
				
				if ((tagLibraryId == null) || tagLibraryId.trim().isEmpty()) {
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST, "tid is not valid");
				}
				
				if ((identifier == null) || identifier.trim().isEmpty()) {
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST, "userident is not valid");
				}
				
				for (TagLibraryReference tagLibRef : repo.getTagLibraryReferences())  {
					if (tagLibRef.getId().equals(tagLibraryId)) {
						try {
							repo.share(tagLibRef, identifier, accessMode);
						}
						catch (IOException e) {
							throw new ResourceException(
									Status.CLIENT_ERROR_NOT_FOUND, 
									"you seem to have no access rights to this tagLibrary");
						}
						return;
					}
				}
				throw new ResourceException(
						Status.CLIENT_ERROR_NOT_FOUND, "corpus not found");
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
