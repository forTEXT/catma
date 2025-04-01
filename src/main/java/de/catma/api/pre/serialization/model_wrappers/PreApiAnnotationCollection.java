package de.catma.api.pre.serialization.model_wrappers;

public class PreApiAnnotationCollection {
	private final String id;
    private final String name;
    private final String author;
    private final String description;
    private final String publisher;
    private final String responsibleUser;
    private final String documentId;
	
    public PreApiAnnotationCollection(String id, String name, String author, String description, String publisher, String responsibleUser, String documentId) {
		super();
		this.id = id;
		this.name = name;
		this.author = author;
		this.description = description;
		this.publisher = publisher;
		this.responsibleUser = responsibleUser;
		this.documentId = documentId;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
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

	public String getResponsibleUser() {
		return responsibleUser;
	}

	public String getDocumentId() {
		return documentId;
	}
}
