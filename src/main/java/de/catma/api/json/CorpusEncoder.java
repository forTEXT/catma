package de.catma.api.json;

import java.util.Collection;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.catma.document.Corpus;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

public class CorpusEncoder {
	
	public String encode(Corpus corpus) throws Exception {
		
		JsonNodeFactory factory = JsonNodeFactory.instance;
		
		ObjectNode corpusJson = factory.objectNode();
		
		corpusJson.put("ID", corpus.getId());
		corpusJson.put("name", corpus.toString());
		ArrayNode contentJson = factory.arrayNode();
		
		corpusJson.set("contents", contentJson);
		
		for (SourceDocument sd : corpus.getSourceDocuments()) {
			ObjectNode sourceDocJson = factory.objectNode();
			sourceDocJson.put("sourceDocID", sd.getID());
			sourceDocJson.put("sourceDocName", sd.toString());
			ContentInfoSet cis = 
					sd.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet();
			
			sourceDocJson.put("sourceDocAuthor", cis.getAuthor());
			sourceDocJson.put("sourceDocDescription", cis.getDescription());
			sourceDocJson.put("sourceDocPublisher", cis.getPublisher());
			
			ArrayNode umcRefListJson = factory.arrayNode();
			sourceDocJson.set("umcList", umcRefListJson);
			
			for (UserMarkupCollectionReference umcRef : corpus.getUserMarkupCollectionRefs(sd)) {
				ObjectNode umcRefJson = factory.objectNode();
				umcRefJson.put("umcID", umcRef.getId());
				umcRefJson.put("umcName", umcRef.getName());
				ContentInfoSet umcCis = umcRef.getContentInfoSet();
				sourceDocJson.put("umcAuthor", umcCis.getAuthor());
				sourceDocJson.put("umcDescription", umcCis.getDescription());
				sourceDocJson.put("umcPublisher", umcCis.getPublisher());	
				umcRefListJson.add(umcRefJson);
			}
			contentJson.add(sourceDocJson);
		}
		
		return corpusJson.toString();
	}

	public String encodeAsList(Collection<Corpus> corpora) throws Exception {
		JsonNodeFactory factory = JsonNodeFactory.instance;

		ArrayNode corporaListJson = factory.arrayNode();
		for (Corpus c : corpora) {
			ObjectNode corpusJson = factory.objectNode();
			corpusJson.put("ID", c.getId());
			corpusJson.put("name", c.toString());
			corporaListJson.add(corpusJson);
		}
		return corporaListJson.toString();
	}

}
