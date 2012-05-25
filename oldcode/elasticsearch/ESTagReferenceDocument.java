package de.catma.indexer.elasticsearch;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import sun.reflect.Reflection;

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
			TagLibrary tagLibrary) throws NoSuchFieldException {
		super();
		this.documentId = sourceDocId;
		this.userMarkupCollectionId = userMarkupCollectionId;
		this.tagDefinitionId = IDGenerator.catmaIDToUUID(tagReference.getTagDefinition()
				.getID());
		this.tagInstanceId = IDGenerator.catmaIDToUUID(tagReference.getTagInstanceID());
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
		j_root.put("userMarkupCollectionId", userMarkupCollectionId);
		j_root.put("tagInstanceId_l", tagInstanceId.getLeastSignificantBits());
		j_root.put("tagInstanceId_m", tagInstanceId.getMostSignificantBits());
		j_root.put("tagDefinitionId_l", tagDefinitionId.getLeastSignificantBits());
		j_root.put("tagDefinitionId_m", tagDefinitionId.getMostSignificantBits());
		j_root.put("version", version);
		j_root.put("tagPath", tagPath);
		j_root.put("characterStart", range.getStartPoint());
		j_root.put("characterEnd", range.getEndPoint());
		return j_root.toString();
	}

	public UUID getIndexDocumentKey() {
		return UUID.nameUUIDFromBytes((tagInstanceId.toString()
				+ range.getStartPoint() + range.getEndPoint()).getBytes());
	}

	public String toString() {
		return tagPath + "[" + range.getStartPoint() + ":"
				+ range.getEndPoint() + "]" + "@" + this.documentId;
	}
}
