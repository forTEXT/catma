package de.catma.api.service;

import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.Form;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import de.catma.backgroundservice.DebugBackgroundServiceProvider;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryManager;
import de.catma.document.repository.RepositoryReference;
import de.catma.tag.TagManager;

public class CorpusList extends ServerResource {
	
	public CorpusList() {

	}
	
	@Get
	public String list() {
		try {
			Form form = getRequest().getResourceRef().getQueryAsForm();
			String corpusId = form.getFirstValue("id");

			
			Properties properties = 
					(Properties) getContext().getAttributes().get("catma_properties");
			
			TagManager tagManager = new TagManager();
			RepositoryManager repoManager = new RepositoryManager(
					new DebugBackgroundServiceProvider(), 
					tagManager, properties);
			if (!repoManager.getRepositoryReferences().isEmpty()) {
				RepositoryReference repoRef = 
					repoManager.getRepositoryReferences().iterator().next();
				ChallengeResponse cr = getChallengeResponse();
				Map<String,String> userIdentification = 
						new HashMap<String, String>(1);
				userIdentification.put("user.ident", cr.getIdentifier());
				
				Repository repo = repoManager.openRepository(repoRef, userIdentification);

				Collection<Corpus> corpora = repo.getCorpora();
	
				if (corpusId != null) {
					for (Corpus c : corpora) {
						if (c.getId().equals(corpusId)) {
							return c.toString();
						}
					}
				}
				else {
					JSONArray corporaListJson = new JSONArray();
					for (Corpus c : corpora) {
						JSONObject corpusJson = new JSONObject();
						corpusJson.put("ID", c.getId());
						corpusJson.put("name", c.toString());
						corporaListJson.put(corpusJson);
					}
					return corporaListJson.toString(2);
				}
			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
