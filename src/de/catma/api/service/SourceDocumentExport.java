package de.catma.api.service;

import java.util.Properties;

import org.restlet.data.ChallengeResponse;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.ByteArrayRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import de.catma.api.Parameter;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;

public class SourceDocumentExport extends ServerResource{

	@Get
	public ByteArrayRepresentation export() {
		try {
			Properties properties = 
				(Properties) getContext().getAttributes().get(
					Parameter.catma_properties.name());
			ChallengeResponse cr = getChallengeResponse();
			Repository repo = new RepositoryLoader().open(properties, cr.getIdentifier());

			Form form = getRequest().getResourceRef().getQueryAsForm();
			
			String sourceDocId = form.getFirstValue(Parameter.sid.name());
			String format = form.getFirstValue(Parameter.format.name());

			if ((format != null) && !format.trim().toLowerCase().equals("plain")) {
				throw new ResourceException(
					Status.SERVER_ERROR_NOT_IMPLEMENTED, 
					"requested format not supported yet");
			}
			
			SourceDocument sd = repo.getSourceDocument(sourceDocId);
			if (sd != null) {
				ByteArrayRepresentation rep = new ByteArrayRepresentation(
						sd.getContent().getBytes("UTF-8"), MediaType.TEXT_PLAIN);
				
				rep.setCharacterSet(CharacterSet.UTF_8);
				
				return rep;
			}
			throw new ResourceException(
					Status.CLIENT_ERROR_NOT_FOUND, "source document not found");
			
		}
		catch (Exception e) {
			throw new ResourceException(
					Status.SERVER_ERROR_INTERNAL, "service implementation error");
		}
	}
}
