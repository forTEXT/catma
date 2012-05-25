package de.catma.indexer.elasticsearch;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import de.catma.indexer.TermInfo;

public class ESPositionIndexDocumentFactory implements
		ESDocumentFactory<ESPositionIndexDocument> {

	public ESPositionIndexDocument fromJSON(JSONObject jsonObject)
			throws JSONException {
		String docid = jsonObject.getString("documentId");
		int c_start = jsonObject.getInt("characterStart");
		int c_end = jsonObject.getInt("characterEnd");
		int offset = jsonObject.getInt("tokenoffset");
		long termid_l = jsonObject.getLong("termId_l");
		long termid_m = jsonObject.getLong("termId_m");
		return new ESPositionIndexDocument(docid, new UUID(termid_m, termid_l),
				new TermInfo(null, c_start, c_end, offset));
	}

}
