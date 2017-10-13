package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.ProjectHandlerException;
import de.catma.repository.git.model_wrappers.GitSourceDocumentInfo;

import javax.annotation.Nullable;
import java.io.InputStream;

public interface IProjectHandler {
	String create(String name, String description) throws ProjectHandlerException;
	void delete(String projectId) throws ProjectHandlerException;

	// source document operations
	String insertSourceDocument(
			String projectId,
			InputStream originalSourceDocumentStream, String originalSourceDocumentFileName,
			InputStream convertedSourceDocumentStream, String convertedSourceDocumentFileName,
			GitSourceDocumentInfo gitSourceDocumentInfo,
			@Nullable String sourceDocumentId) throws ProjectHandlerException;
}
