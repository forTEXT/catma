package de.catma.repository.git.graph;

import java.net.URI;
import java.nio.file.Path;

public interface DocumentFileURIProvider {
	public URI getSourceDocumentFileURI(String documentId) throws Exception;
}
