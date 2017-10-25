package de.catma.repository.git.serialization.models;

public class MarkupCollectionHeader extends HeaderBase {
	private String sourceDocumentId;

	private String author;
	private String publisher;

	public MarkupCollectionHeader(){
		super("", "");
		this.sourceDocumentId = "";
		this.author = "";
		this.publisher = "";
	}

	public MarkupCollectionHeader(String name, String description, String sourceDocumentId) {
		super(name, description);
		this.sourceDocumentId = sourceDocumentId;
		this.author = "";
		this.publisher = "";
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
