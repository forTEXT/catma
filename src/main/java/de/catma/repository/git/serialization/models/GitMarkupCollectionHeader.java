package de.catma.repository.git.serialization.models;

public class GitMarkupCollectionHeader extends GitHeaderBase {
	private String sourceDocumentId;
	private String sourceDocumentVersion;

	private String author;
	private String publisher;

	public GitMarkupCollectionHeader() {
	}

	public GitMarkupCollectionHeader(String name, String description,
									 String sourceDocumentId, String sourceDocumentVersion) {
		super(name, description);

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

	public String getAuthor(){return this.author;}

	public void setAuthor(String author){ this.author = author; }

	public String getPublisher(){ return this.publisher; }

	public void setPublisher(String publisher) { this.publisher = publisher; }
}
