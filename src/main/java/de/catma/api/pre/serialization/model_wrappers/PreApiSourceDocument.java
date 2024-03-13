package de.catma.api.pre.serialization.model_wrappers;

import java.net.URI;

public class PreApiSourceDocument {
    private final String id;
    private final String bodyUrl;
    private final String crc32bChecksum;
    private final int size;
    private final String title;
    private final transient URI fileUri;

    public PreApiSourceDocument(String id, String bodyUrl, String crc32bChecksum, int size, String title, URI fileUri) {
		super();
		this.id = id;
		this.bodyUrl = bodyUrl;
		this.crc32bChecksum = crc32bChecksum;
		this.size = size;
		this.title = title;
		this.fileUri = fileUri;
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
    
    public URI getFileUri() {
		return fileUri;
	}

}
