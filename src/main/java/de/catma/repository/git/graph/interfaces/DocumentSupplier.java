package de.catma.repository.git.graph.interfaces;

import de.catma.document.source.SourceDocument;

import java.io.IOException;

public interface DocumentSupplier {
    SourceDocument get(String documentId) throws IOException;
}
