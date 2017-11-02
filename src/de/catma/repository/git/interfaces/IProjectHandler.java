package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.ProjectHandlerException;
import de.catma.repository.git.serialization.model_wrappers.GitSourceDocumentInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

public interface IProjectHandler {
	String create(String name, String description) throws ProjectHandlerException;

	void delete(String projectId) throws ProjectHandlerException;

	// tagset operations
	String createTagset(@Nonnull String projectId,
						@Nullable String tagsetId,
						@Nonnull String name,
						@Nullable String description
	) throws ProjectHandlerException;

	// markup collection operations
	void addTagsetToMarkupCollection(String projectId, String markupCollectionId, String tagsetId, String tagsetVersion)
			throws ProjectHandlerException;

	// source document operations
	String insertSourceDocument(
			String projectId,
			InputStream originalSourceDocumentStream, String originalSourceDocumentFileName,
			InputStream convertedSourceDocumentStream, String convertedSourceDocumentFileName,
			GitSourceDocumentInfo gitSourceDocumentInfo,
			@Nullable String sourceDocumentId) throws ProjectHandlerException;
}
