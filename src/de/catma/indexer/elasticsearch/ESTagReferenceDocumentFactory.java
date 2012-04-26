package de.catma.indexer.elasticsearch;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import de.catma.core.document.Range;

public class ESTagReferenceDocumentFactory implements
		ESDocumentFactory<ESTagReferenceDocument> {

	public ESTagReferenceDocument fromJSON(JSONObject jsonObject)
			throws JSONException {
		String documentId = jsonObject.getString("documentId");
		String userMarkupCollectionId = jsonObject
				.getString("userMarkupCollectionId");
		long tagDefinitionId_l = jsonObject.getLong("tagDefinitionId_l");
		long tagDefinitionId_m = jsonObject.getLong("tagDefinitionId_m");
		long tagInstanceId_l = jsonObject.getLong("tagInstanceId_l");
		long tagInstanceId_m = jsonObject.getLong("tagInstanceId_m");
		String tagPath = jsonObject.getString("tagPath");
		String version = jsonObject.getString("version");
		int c_start = jsonObject.getInt("characterStart");
		int c_end = jsonObject.getInt("characterEnd");

		return new ESTagReferenceDocument(documentId, userMarkupCollectionId,
				new UUID(tagDefinitionId_m, tagDefinitionId_l), new UUID(
						tagInstanceId_m, tagInstanceId_l), tagPath, version,
				new Range(c_start, c_end));

	}

}
