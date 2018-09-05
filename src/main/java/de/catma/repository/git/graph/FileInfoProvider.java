package de.catma.repository.git.graph;

import java.net.URI;
import java.nio.file.Path;

public interface FileInfoProvider {
	public URI getSourceDocumentFileURI(String documentId) throws Exception;
	public Path getTokenizedSourceDocumentPath(String documentId) throws Exception;
}
