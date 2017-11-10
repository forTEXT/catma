package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.TagsetHandlerException;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ITagsetHandler {
	String create(
			@Nonnull String projectId,
			@Nullable String tagsetId,
			@Nonnull String name,
			@Nullable String description
	) throws TagsetHandlerException;

	void delete(String tagsetId) throws TagsetHandlerException;

	TagsetDefinition open(@Nonnull String projectId, @Nonnull String tagsetId) throws TagsetHandlerException;

	String createTagDefinition(@Nonnull String projectId, @Nonnull String tagsetId,
							   @Nonnull TagDefinition tagDefinition)
			throws TagsetHandlerException;
}
