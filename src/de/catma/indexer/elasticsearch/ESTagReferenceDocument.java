package de.catma.indexer.elasticsearch;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import de.catma.core.document.Range;
import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.tag.TagLibrary;
import de.catma.core.util.IDGenerator;

public class ESTagReferenceDocument {

	private UUID tagReferenceId;
	private String documentId;
	private String userMarkupCollectionId;
	private UUID tagDefinitionId;
	private UUID tagInstanceId;
	private String tagPath;
	private String version;
	private Range range;

	public ESTagReferenceDocument(String sourceDocId,
			String userMarkupCollectionId, TagReference tagReference,
			TagLibrary tagLibrary) {
		super();
		this.documentId = sourceDocId;
		this.userMarkupCollectionId = userMarkupCollectionId;
		this.tagDefinitionId = catmaIDToUUID(tagReference.getTagDefinition()
				.getID());
		this.tagInstanceId = catmaIDToUUID(tagReference.getTagInstanceID());
		this.tagPath = tagLibrary.getTagPath(tagReference.getTagDefinition());
		this.range = tagReference.getRange();
		this.version = tagReference.getTagDefinition().getVersion().toString();
		this.tagReferenceId = this.getIndexDocumentKey();
	}

	public ESTagReferenceDocument(String documentId,
			String userMarkupCollectionId, UUID tagDefinitionId,
			UUID tagInstanceId, String tagPath, String version, Range range) {
		super();
		this.documentId = documentId;
		this.userMarkupCollectionId = userMarkupCollectionId;
		this.tagDefinitionId = tagDefinitionId;
		this.tagInstanceId = tagInstanceId;
		this.tagPath = tagPath;
		this.version = version;
		this.range = range;
		this.tagReferenceId = this.getIndexDocumentKey();
	}

	private UUID catmaIDToUUID(String catmastr) {
		int index = catmastr.indexOf(IDGenerator.ID_PREFIX)+IDGenerator.ID_PREFIX.length();
		return UUID.fromString(catmastr.substring(index));
	}

	public UUID getTagReferenceId() {
		return tagReferenceId;
	}

	public String getDocumentId() {
		return documentId;
	}

	public String getUserMarkupCollectionId() {
		return userMarkupCollectionId;
	}

	public UUID getTagDefinitionId() {
		return tagDefinitionId;
	}

	public UUID getTagInstanceId() {
		return tagInstanceId;
	}

	public String getTagPath() {
		return tagPath;
	}

	public String getVersion() {
		return version;
	}

	public Range getRange() {
		return range;
	}

	public String toJSON() throws JSONException {
		JSONObject j_root = new JSONObject();
		j_root.put("documentId", documentId);
		j_root.put("markupDocumentId", userMarkupCollectionId);
		j_root.put("tagInstanceId_l", tagInstanceId.getLeastSignificantBits());
		j_root.put("tagInstanceId_m", tagInstanceId.getMostSignificantBits());
		j_root.put("tagPath", tagPath);
		j_root.put("characterStart", range.getStartPoint());
		j_root.put("characterEnd", range.getEndPoint());
		return j_root.toString();
	}

	public UUID getIndexDocumentKey() {
		return UUID.nameUUIDFromBytes((tagInstanceId.toString()
				+ range.getStartPoint() + range.getEndPoint()).getBytes());
	}

	public static ESTagReferenceDocument fromJSON(String jsonstring)
			throws JSONException {
		return fromJSON(new JSONObject(jsonstring));
	}

	public static ESTagReferenceDocument fromJSON(JSONObject jsonObject)
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

	public String toString() {
		return tagPath + "[" + range.getStartPoint() + ":"
				+ range.getEndPoint() + "]" + "@" + this.documentId;
	}
}
