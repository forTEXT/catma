package de.catma.repository.git.interfaces;

import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.repository.git.exceptions.MarkupCollectionHandlerException;
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
	) throws MarkupCollectionHandlerException;

	void delete(@Nonnull String projectId, @Nonnull String markupCollectionId) throws MarkupCollectionHandlerException;

	void addTagset(
			@Nonnull String projectId,
			@Nonnull String markupCollectionId,
			@Nonnull String tagsetId,
			@Nonnull String tagsetVersion
	) throws MarkupCollectionHandlerException;

	void removeTagset(@Nonnull String projectId, @Nonnull String markupCollectionId, @Nonnull String tagsetId)
			throws MarkupCollectionHandlerException;

	String createTagInstance(
			@Nonnull String projectId,
			@Nonnull String markupCollectionId,
			@Nonnull JsonLdWebAnnotation annotation
	) throws MarkupCollectionHandlerException;

	UserMarkupCollection open(@Nonnull String projectId, @Nonnull String markupCollectionId)
			throws MarkupCollectionHandlerException;
}