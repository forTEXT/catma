package de.catma.repository;

import de.catma.document.source.SourceDocument;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;

public interface ISourceDocumentHandler {
    void insert(final SourceDocument sourceDocument) throws IOException, GitAPIException;
}
