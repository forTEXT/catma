package de.catma.repository.git;

import de.catma.repository.git.exceptions.SourceDocumentHandlerException;
import de.catma.repository.git.interfaces.ISourceDocumentHandler;
import de.catma.repository.git.model_wrappers.GitSourceDocumentInfo;

import javax.annotation.Nullable;
import java.io.InputStream;

public class TagsetHandler implements ISourceDocumentHandler {
	@Override
	public String insert(InputStream originalSourceDocumentStream, String originalSourceDocumentFileName, InputStream convertedSourceDocumentStream, String convertedSourceDocumentFileName, GitSourceDocumentInfo gitSourceDocumentInfo, @Nullable String sourceDocumentId, @Nullable String projectId) throws SourceDocumentHandlerException {
		return null;
	}

	@Override
	public void remove(String sourceDocumentId) throws SourceDocumentHandlerException {

	}
}
