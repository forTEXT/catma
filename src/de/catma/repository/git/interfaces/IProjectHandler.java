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
	String createMarkupCollection(@Nonnull String projectId,
								  @Nullable String markupCollectionId,
								  @Nonnull String name,
								  @Nullable String description,
								  @Nonnull String sourceDocumentId,
								  @Nonnull String sourceDocumentVersion
	) throws ProjectHandlerException;

	void addTagsetToMarkupCollection(String projectId, String markupCollectionId, String tagsetId, String tagsetVersion)
			throws ProjectHandlerException;

	// source document operations
	String createSourceDocument(
			@Nonnull String projectId, @Nullable String sourceDocumentId,
			@Nonnull InputStream originalSourceDocumentStream, @Nonnull String originalSourceDocumentFileName,
			@Nonnull InputStream convertedSourceDocumentStream, @Nonnull String convertedSourceDocumentFileName,
			@Nonnull GitSourceDocumentInfo gitSourceDocumentInfo
	) throws ProjectHandlerException;
}
