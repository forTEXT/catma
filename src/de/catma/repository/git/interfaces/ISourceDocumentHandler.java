package de.catma.repository.git.interfaces;

import de.catma.document.source.SourceDocument;
import de.catma.repository.git.exceptions.SourceDocumentHandlerException;
import de.catma.repository.git.serialization.model_wrappers.GitSourceDocumentInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

public interface ISourceDocumentHandler {
    String insert(@Nonnull String projectId, @Nullable String sourceDocumentId,
				  @Nonnull InputStream originalSourceDocumentStream, @Nonnull String originalSourceDocumentFileName,
				  @Nonnull InputStream convertedSourceDocumentStream, @Nonnull String convertedSourceDocumentFileName,
				  @Nonnull GitSourceDocumentInfo gitSourceDocumentInfo
	) throws SourceDocumentHandlerException;

    void remove(String sourceDocumentId) throws SourceDocumentHandlerException;

    SourceDocument open(String sourceDocumentId, String projectId) throws SourceDocumentHandlerException;
}
