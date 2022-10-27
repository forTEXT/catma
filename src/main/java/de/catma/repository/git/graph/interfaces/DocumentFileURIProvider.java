package de.catma.repository.git.graph.interfaces;

import java.net.URI;

public interface DocumentFileURIProvider {
	URI getDocumentFileURI(String documentId) throws Exception;
}
