package de.catma.api.pre.serialization.model_wrappers;

public class PreApiAnnotationCollection {
	private final String id;
    private final String title;
    private final String author;
    private final String description;
    private final String publisher;
    private final String documentId;
	
    public PreApiAnnotationCollection(String id, String title, String author, String description, String publisher, String documentId) {
		super();
		this.id = id;
		this.title = title;
		this.author = author;
		this.description = description;
		this.publisher = publisher;
		this.documentId = documentId;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public String getDescription() {
		return description;
	}

	public String getPublisher() {
		return publisher;
	}
	
	public String getDocumentId() {
		return documentId;
	}
}
