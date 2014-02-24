package de.catma.api.service;

import java.util.Collection;
import java.util.Properties;

import org.restlet.data.ChallengeResponse;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import de.catma.api.Parameter;
import de.catma.api.json.CorpusEncoder;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;

public class CorpusList extends ServerResource {
	
	private CorpusEncoder corpusEncoder;

	public CorpusList() {
		this.corpusEncoder = new CorpusEncoder();
	}
	
	@Get
	public String list() {
		try {
			Properties properties = 
				(Properties) getContext().getAttributes().get(
					Parameter.catma_properties.name());
			ChallengeResponse cr = getChallengeResponse();
			Repository repo = new RepositoryLoader().open(properties, cr.getIdentifier());

			Collection<Corpus> corpora = repo.getCorpora();
			Form form = getRequest().getResourceRef().getQueryAsForm();
			String corpusId = form.getFirstValue(Parameter.cid.name());

			if (corpusId != null) {
				for (Corpus c : corpora) {
					if (c.getId().equals(corpusId)) {
						return corpusEncoder.encode(c);
					}
				}
				throw new ResourceException(
						Status.CLIENT_ERROR_NOT_FOUND, "corpus not found");
			}
			else {
				return corpusEncoder.encodeAsList(corpora);
			}
		}
		catch (Exception e) {
			throw new ResourceException(
					Status.SERVER_ERROR_INTERNAL, "service implementation error");
		}
	}

}
