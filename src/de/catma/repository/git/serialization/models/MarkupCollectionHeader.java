package de.catma.repository.git.serialization.models;

public class MarkupCollectionHeader extends HeaderBase {
	private String sourceDocumentId;

	public MarkupCollectionHeader(String name, String description, String sourceDocumentId) {
		super(name, description);
		this.sourceDocumentId = sourceDocumentId;
	}

	public String getSourceDocumentId() {
		return this.sourceDocumentId;
	}

	public void setSourceDocumentId(String sourceDocumentId) {
		this.sourceDocumentId = sourceDocumentId;
	}
}
