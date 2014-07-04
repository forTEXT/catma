package de.catma.api.service;

import java.io.ByteArrayOutputStream;
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
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.serialization.tei.TeiUserMarkupCollectionSerializationHandler;

public class UserMarkupCollectionExport extends ServerResource {

	@Get
	public ByteArrayRepresentation export() {
		try {
			Properties properties = 
				(Properties) getContext().getAttributes().get(
					Parameter.catma_properties.name());
			ChallengeResponse cr = getChallengeResponse();
			Repository repo = new RepositoryLoader().open(properties, cr.getIdentifier());

			Form form = getRequest().getResourceRef().getQueryAsForm();
			
			String umcId = form.getFirstValue(Parameter.uid.name());
			String format = form.getFirstValue(Parameter.format.name());

			if ((format != null) && !format.trim().toLowerCase().equals("tei")) {
				throw new ResourceException(
					Status.SERVER_ERROR_NOT_IMPLEMENTED, 
					"requested format not supported yet");
			}
			
			UserMarkupCollectionReference umcRef = new UserMarkupCollectionReference(umcId, new ContentInfoSet());
			
			for (SourceDocument sd : repo.getSourceDocuments()) {
				if (sd.getUserMarkupCollectionRefs().contains(umcRef)) {
					UserMarkupCollection umc = repo.getUserMarkupCollection(umcRef);
					TeiUserMarkupCollectionSerializationHandler handler =
							new TeiUserMarkupCollectionSerializationHandler(
									repo.getTagManager(), false);
					ByteArrayOutputStream teiDocOut = new ByteArrayOutputStream();
					handler.serialize(umc, sd, teiDocOut);
					ByteArrayRepresentation rep = 
						new ByteArrayRepresentation(
							teiDocOut.toByteArray(), MediaType.APPLICATION_XML);
					rep.setCharacterSet(CharacterSet.UTF_8);
					return rep;
				}
			}
			
			throw new ResourceException(
					Status.CLIENT_ERROR_NOT_FOUND, "user markup collection not found");
			
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
