package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.SourceDocumentHandlerException;
import de.catma.repository.git.serialization.model_wrappers.GitSourceDocumentInfo;

import javax.annotation.Nullable;
import java.io.InputStream;

public interface ISourceDocumentHandler {
    String insert(InputStream originalSourceDocumentStream, String originalSourceDocumentFileName,
				  InputStream convertedSourceDocumentStream, String convertedSourceDocumentFileName,
				  GitSourceDocumentInfo gitSourceDocumentInfo,
				  @Nullable String sourceDocumentId,
				  @Nullable String projectId) throws SourceDocumentHandlerException;

    void remove(String sourceDocumentId) throws SourceDocumentHandlerException;
}
