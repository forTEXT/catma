package de.catma.repository.git.graph.interfaces;

import de.catma.document.source.SourceDocument;

import java.util.List;

public interface DocumentsProvider {
    List<SourceDocument> getDocuments();
}
