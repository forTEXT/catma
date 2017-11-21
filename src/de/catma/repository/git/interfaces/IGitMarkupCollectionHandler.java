package de.catma.repository.git.interfaces;

import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.repository.git.exceptions.GitMarkupCollectionHandlerException;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IGitMarkupCollectionHandler {
	String create(
			@Nonnull String projectId,
			@Nullable String markupCollectionId,
			@Nonnull String name,
			@Nullable String description,
			@Nonnull String sourceDocumentId,
			@Nonnull String sourceDocumentVersion
	) throws GitMarkupCollectionHandlerException;

	void delete(@Nonnull String projectId, @Nonnull String markupCollectionId) throws GitMarkupCollectionHandlerException;

	void addTagset(
			@Nonnull String projectId,
			@Nonnull String markupCollectionId,
			@Nonnull String tagsetId,
			@Nonnull String tagsetVersion
	) throws GitMarkupCollectionHandlerException;

	void removeTagset(@Nonnull String projectId, @Nonnull String markupCollectionId, @Nonnull String tagsetId)
			throws GitMarkupCollectionHandlerException;

	String createTagInstance(
			@Nonnull String projectId,
			@Nonnull String markupCollectionId,
			@Nonnull JsonLdWebAnnotation annotation
	) throws GitMarkupCollectionHandlerException;

	UserMarkupCollection open(@Nonnull String projectId, @Nonnull String markupCollectionId)
			throws GitMarkupCollectionHandlerException;
}
