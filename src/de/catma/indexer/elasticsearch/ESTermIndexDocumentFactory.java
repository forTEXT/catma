package de.catma.indexer.elasticsearch;

import org.json.JSONException;
import org.json.JSONObject;

public class ESTermIndexDocumentFactory implements
		ESDocumentFactory<ESTermIndexDocument> {

	public ESTermIndexDocument fromJSON(JSONObject jsonObject)
			throws JSONException {
		String docid = jsonObject.getString("documentId");
		String term = jsonObject.getString("term");
		int freq = jsonObject.getInt("frequency");
		return new ESTermIndexDocument(docid, term, freq);
	}

}
