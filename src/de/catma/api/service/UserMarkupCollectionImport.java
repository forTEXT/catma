package de.catma.api.service;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;

import org.restlet.data.ChallengeResponse;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import de.catma.api.Parameter;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.contenthandler.BOMFilterInputStream;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.util.Pair;

public class UserMarkupCollectionImport extends ServerResource {
	
	private String newUmcID = null;

	@Post
	public String importUmc(String args) {
		try {
			Properties properties = 
				(Properties) getContext().getAttributes().get(
					Parameter.catma_properties.name());
			ChallengeResponse cr = getChallengeResponse();
			Repository repo = new RepositoryLoader().open(
					properties, cr.getIdentifier());
			
			PropertyChangeListener newUmcListener =  
				new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						if (evt.getOldValue() == null) { // creation of new umc
							@SuppressWarnings("unchecked")
							Pair<UserMarkupCollectionReference, SourceDocument> result = 
								(Pair<UserMarkupCollectionReference, SourceDocument>)
									evt.getNewValue();
							newUmcID = result.getFirst().getId();
						}
					}
			};
			try {
				repo.addPropertyChangeListener(
						RepositoryChangeEvent.userMarkupCollectionChanged,
						newUmcListener);
				
				Form form = getRequest().getResourceRef().getQueryAsForm();
				
				String sourceDocId = form.getFirstValue(Parameter.sid.name());
				String corpusId = form.getFirstValue(Parameter.cid.name());
				
				SourceDocument sd = repo.getSourceDocument(sourceDocId);
				if (sd != null) {
	
					byte[] rawTeiXml = args.getBytes(Charset.forName("UTF-8"));
					InputStream is = new ByteArrayInputStream(rawTeiXml);
	
					if (BOMFilterInputStream.hasBOM(rawTeiXml)) {
						is = new BOMFilterInputStream(
								is, Charset.forName("UTF-8"));
					}
				
					repo.importUserMarkupCollection(is, sd);
					
					if (corpusId != null) {
						if (!addToCorpus(corpusId, repo)) {
							throw new ResourceException(
									Status.CLIENT_ERROR_NOT_FOUND, 
									"corpus not found, new umc #" 
											+ newUmcID + " could not be added");
						}
					}
				}
				else {
					throw new ResourceException(
							Status.CLIENT_ERROR_NOT_FOUND, 
							"source document not found");
				}
				final String result = newUmcID;
				
				return result;
			}
			finally {
				newUmcID = null;
				repo.removePropertyChangeListener(
					RepositoryChangeEvent.userMarkupCollectionChanged, 
					newUmcListener);
			}
		}
		catch (Exception e) {
			throw new ResourceException(
					Status.SERVER_ERROR_INTERNAL, "service implementation error");
		}
	}

	private boolean addToCorpus(String corpusId, Repository repo) {
		for (Corpus c : repo.getCorpora()) {
			if (c.getId().equals(corpusId)) {
				try {
					repo.update(
						c, 
						new UserMarkupCollectionReference(
							newUmcID, new ContentInfoSet()));
					return true;
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		
		return false;
	}
}
