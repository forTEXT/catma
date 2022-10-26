package de.catma.repository.git.graph.interfaces;

import java.net.URI;
import java.nio.file.Path;

public interface DocumentFileURIProvider {
	public URI getSourceDocumentFileURI(String documentId) throws Exception;
}
