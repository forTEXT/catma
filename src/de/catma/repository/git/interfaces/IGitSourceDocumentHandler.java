package de.catma.repository.git.interfaces;

import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.repository.git.exceptions.GitSourceDocumentHandlerException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

public interface IGitSourceDocumentHandler {
    String create(@Nonnull String projectId, @Nullable String sourceDocumentId,
				  @Nonnull InputStream originalSourceDocumentStream, @Nonnull String originalSourceDocumentFileName,
				  @Nonnull InputStream convertedSourceDocumentStream, @Nonnull String convertedSourceDocumentFileName,
				  @Nonnull SourceDocumentInfo sourceDocumentInfo
	) throws GitSourceDocumentHandlerException;

    void delete(@Nonnull String projectId, @Nonnull String sourceDocumentId) throws GitSourceDocumentHandlerException;

    SourceDocument open(@Nonnull String projectId, @Nonnull String sourceDocumentId)
			throws GitSourceDocumentHandlerException;
}