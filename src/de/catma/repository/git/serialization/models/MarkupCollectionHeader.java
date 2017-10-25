package de.catma.repository.git.serialization.models;

import java.util.Map;
import java.util.TreeMap;

public class MarkupCollectionHeader extends HeaderBase {
	private String sourceDocumentId;
	private String sourceDocumentVersion;

	private TreeMap<String, String> tagsets; // <UUID, version>

	public MarkupCollectionHeader() {
		this.tagsets = new TreeMap<>();
	}

	public MarkupCollectionHeader(String name, String description,
								  String sourceDocumentId, String sourceDocumentVersion) {
		super(name, description);
		this.tagsets = new TreeMap<>();

		this.sourceDocumentId = sourceDocumentId;
		this.sourceDocumentVersion = sourceDocumentVersion;
	}

	public String getSourceDocumentId() {
		return this.sourceDocumentId;
	}

	public void setSourceDocumentId(String sourceDocumentId) {
		this.sourceDocumentId = sourceDocumentId;
	}

	public String getSourceDocumentVersion() {
		return this.sourceDocumentVersion;
	}

	public void setSourceDocumentVersion(String sourceDocumentVersion) {
		this.sourceDocumentVersion = sourceDocumentVersion;
	}

	public TreeMap<String, String> getTagsets() {
		return this.tagsets;
	}

	public void setTagsets(TreeMap<String, String> tagsets) {
		this.tagsets = tagsets;
	}

	public void addTagset(Map.Entry<String, String> tagsetEntry) {
		this.tagsets.put(tagsetEntry.getKey(), tagsetEntry.getValue());
	}

	public void removeTagset(String tagsetUuid) {
		this.tagsets.remove(tagsetUuid);
	}
}
