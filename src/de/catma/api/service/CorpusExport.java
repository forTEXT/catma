package de.catma.api.service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import org.restlet.data.ChallengeResponse;
import org.restlet.data.Disposition;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.FileRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import de.catma.api.Parameter;
import de.catma.document.Corpus;
import de.catma.document.corpus.CorpusExporter;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryPropertyKey;

public class CorpusExport extends ServerResource {

	@Get
	public FileRepresentation export() {
		try {
			Properties properties = 
				(Properties) getContext().getAttributes().get(
					Parameter.catma_properties.name());
			ChallengeResponse cr = getChallengeResponse();
			Repository repo = new RepositoryLoader().open(properties, cr.getIdentifier());
	
			Collection<Corpus> corpora = repo.getCorpora();
			Form form = getRequest().getResourceRef().getQueryAsForm();
			String corpusId = form.getFirstValue(Parameter.cid.name());
			String format = form.getFirstValue(Parameter.format.name());
			
			if ((format != null) && !format.trim().toLowerCase().equals("plain,tei")) {
				throw new ResourceException
				(Status.SERVER_ERROR_NOT_IMPLEMENTED, "requested format not supported yet");
			}
			
			for (Corpus c : corpora) {
				if (c.getId().equals(corpusId)) {

					CorpusExporter corpusExporter = new CorpusExporter(repo);
					final String name = corpusExporter.cleanupName(c.toString());
					final String fileName = name + corpusExporter.getDate() + ".tar.gz";
					String tempDir =
						properties.getProperty(RepositoryPropertyKey.TempDir.name());
					
					File out = new File(tempDir, fileName);
					FileOutputStream corpusOut = new FileOutputStream(out);
						
					corpusExporter.export(
							name,
							Collections.singletonList(c), corpusOut);
					
					FileRepresentation rep = 
							new FileRepresentation(out, MediaType.APPLICATION_GNU_ZIP);
					
					Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
					disp.setFilename(fileName);
					disp.setSize(out.length());
					rep.setDisposition(disp);
					return rep; 
				}
			}
			throw new ResourceException(
					Status.CLIENT_ERROR_NOT_FOUND, "corpus not found");
			
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

