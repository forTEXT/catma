package de.catma.api.v1.serialization.model_wrappers;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.URI;

public class ProjectExportSourceDocument {
    private final String id;
    private final String bodyUrl;
    private final String crc32bChecksum;
    private final int size;
    private final String title;
    private final String author;
    private final String description;
    private final String publisher;
    private final transient URI fileUri;
    private final String responsibleUser;

    public ProjectExportSourceDocument(String id, String bodyUrl, String crc32bChecksum, int size, String title, String author,
                                       String description, String publisher, URI fileUri, String responsibleUser) {
		super();
		this.id = id;
		this.bodyUrl = bodyUrl;
		this.crc32bChecksum = crc32bChecksum;
		this.size = size;
		this.title = title;
		this.author = author;
		this.description = description;
		this.publisher = publisher;
		this.fileUri = fileUri;
		this.responsibleUser = responsibleUser;
	}

	public String getId() {
        return id;
    }

    public String getBodyUrl() {
        return bodyUrl;
    }

    public String getCrc32bChecksum() {
        return crc32bChecksum;
    }

    public int getSize() {
        return size;
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

    @JsonIgnore
    public URI getFileUri() {
		return fileUri;
	}

    public String getResponsibleUser() {
        return responsibleUser;
    }
}
