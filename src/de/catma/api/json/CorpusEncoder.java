package de.catma.api.json;

import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONObject;

import de.catma.document.Corpus;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

public class CorpusEncoder {
	
	public String encode(Corpus corpus) throws Exception {
		JSONObject corpusJson = new JSONObject();
		corpusJson.put("ID", corpus.getId());
		corpusJson.put("name", corpus.toString());
		JSONArray contentJson = new JSONArray();
		corpusJson.put("contents", contentJson);
		
		for (SourceDocument sd : corpus.getSourceDocuments()) {
			JSONObject sourceDocJson = new JSONObject();
			sourceDocJson.put("sourceDocID", sd.getID());
			sourceDocJson.put("sourceDocName", sd.toString());
			ContentInfoSet cis = 
					sd.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet();
			
			sourceDocJson.put("sourceDocAuthor", cis.getAuthor());
			sourceDocJson.put("sourceDocDescription", cis.getDescription());
			sourceDocJson.put("sourceDocPublisher", cis.getPublisher());
			
			JSONArray umcRefListJson = new JSONArray();
			sourceDocJson.put("umcList", umcRefListJson);
			
			for (UserMarkupCollectionReference umcRef : corpus.getUserMarkupCollectionRefs(sd)) {
				JSONObject umcRefJson = new JSONObject();
				umcRefJson.put("umcID", umcRef.getId());
				umcRefJson.put("umcName", umcRef.getName());
				ContentInfoSet umcCis = umcRef.getContentInfoSet();
				sourceDocJson.put("umcAuthor", umcCis.getAuthor());
				sourceDocJson.put("umcDescription", umcCis.getDescription());
				sourceDocJson.put("umcPublisher", umcCis.getPublisher());	
				umcRefListJson.put(umcRefJson);
			}
			contentJson.put(sourceDocJson);
		}
		
		return corpusJson.toString(2);
	}

	public String encodeAsList(Collection<Corpus> corpora) throws Exception {
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
