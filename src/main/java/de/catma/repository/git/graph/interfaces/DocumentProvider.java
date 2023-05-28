package de.catma.repository.git.graph.interfaces;

import de.catma.document.source.SourceDocument;

import java.io.IOException;

public interface DocumentProvider {
    SourceDocument getDocument(String documentId) throws IOException;
}
