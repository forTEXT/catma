package de.catma.repository.git.serialization.models;

public class GitMarkupCollectionHeader extends GitHeaderBase {
	private String sourceDocumentId;

	private String author;
	private String publisher;

	public GitMarkupCollectionHeader() {
	}

	public GitMarkupCollectionHeader(
			String name, String description, String responsableUser,
			String forkedFromCommitURL, String sourceDocumentId) {
		super(name, description, responsableUser, forkedFromCommitURL);

		this.sourceDocumentId = sourceDocumentId;
	}

	public String getSourceDocumentId() {
		return this.sourceDocumentId;
	}

	public void setSourceDocumentId(String sourceDocumentId) {
		this.sourceDocumentId = sourceDocumentId;
	}

	public String getAuthor(){return this.author;}

	public void setAuthor(String author){ this.author = author; }

	public String getPublisher(){ return this.publisher; }

	public void setPublisher(String publisher) { this.publisher = publisher; }
}
