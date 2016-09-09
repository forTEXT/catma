package de.catma.api.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.restlet.data.ChallengeResponse;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import de.catma.api.ApiLoginToken;
import de.catma.api.Parameter;
import de.catma.document.AccessMode;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.repository.db.maintenance.UserManager;

public class CorpusShare extends ServerResource {

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

				Collection<Corpus> corpora = repo.getCorpora();
				Form form = getRequest().getResourceRef().getQueryAsForm();
				
				String corpusId = form.getFirstValue(Parameter.cid.name());
				AccessMode accessMode = 
					AccessMode.findValueOf(form.getFirstValue(Parameter.accessmode.name()));
				String identifier = 
					form.getFirstValue(Parameter.userident.name());
					
				if (accessMode == null) {
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST, "access mode is not valid");
				}
				
				if ((corpusId == null) || corpusId.trim().isEmpty()) {
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST, "cid is not valid");
				}
				
				if ((identifier == null) || identifier.trim().isEmpty()) {
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST, "userident is not valid");
				}
				
				for (Corpus corpus : repo.getCorpora())  {
					if (corpus.getId().equals(corpusId)) {
						try {
							repo.share(corpus, identifier, accessMode);
						}
						catch (IOException e) {
							throw new ResourceException(
									Status.CLIENT_ERROR_NOT_FOUND, "you seem to have no access rights to this corpus");
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
