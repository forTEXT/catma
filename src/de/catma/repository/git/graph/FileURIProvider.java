package de.catma.repository.git.graph;

import java.net.URI;

public interface FileURIProvider {
	public URI getSourceDocumentFileURI(String documentId) throws Exception;
}
