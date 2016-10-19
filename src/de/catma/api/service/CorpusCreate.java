package de.catma.api.service;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Properties;

import org.restlet.data.ChallengeResponse;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import de.catma.api.ApiLoginToken;
import de.catma.api.Parameter;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.repository.db.maintenance.UserManager;

public class CorpusCreate extends ServerResource {

	private String newCorpusID;

	public String create() {
		try {
			Properties properties = 
				(Properties) getContext().getAttributes().get(
					Parameter.catma_properties.name());
			ChallengeResponse cr = getChallengeResponse();

			
			PropertyChangeListener newCorpusListener = 
				new PropertyChangeListener() {
					
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						if (evt.getOldValue() == null) {
							Corpus corpus = (Corpus) evt.getNewValue();
							
							newCorpusID = corpus.getId();
						}
						
					}
				};
				
			UserManager userManager = new UserManager();
			ApiLoginToken loginToken = new ApiLoginToken(cr.getIdentifier());
			Repository repo = 
					new RepositoryLoader().open(properties, cr.getIdentifier());
			try {

				Collection<Corpus> corpora = repo.getCorpora();
				Form form = getRequest().getResourceRef().getQueryAsForm();
				String corpusName = form.getFirstValue(Parameter.cname.name());
				
				if ((corpusName == null) || corpusName.trim().isEmpty()) {
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST, "corpus name is not valid");
				}
				
				for (Corpus corpus : corpora) {
					if (corpus.toString().equals(corpusName)) {
						throw new ResourceException(
								Status.CLIENT_ERROR_BAD_REQUEST, "corpus name already exists");
					}
				}
				repo.addPropertyChangeListener(
						RepositoryChangeEvent.corpusChanged, newCorpusListener);
				repo.createCorpus(corpusName);
				
				final String result = newCorpusID;
				
				return result;
			}
			finally {
				userManager.logout(loginToken);
				
				newCorpusID = null;
				repo.removePropertyChangeListener(
					RepositoryChangeEvent.corpusChanged, 
					newCorpusListener);
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
