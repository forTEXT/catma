package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.GitTagsetHandlerException;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IGitTagsetHandler {
	String create(
			@Nonnull String projectId,
			@Nullable String tagsetId,
			@Nonnull String name,
			@Nullable String description
	) throws GitTagsetHandlerException;

	void delete(@Nonnull String projectId, @Nonnull String tagsetId) throws GitTagsetHandlerException;

	TagsetDefinition open(@Nonnull String projectId, @Nonnull String tagsetId) throws GitTagsetHandlerException;

	String createTagDefinition(@Nonnull String projectId, @Nonnull String tagsetId,
							   @Nonnull TagDefinition tagDefinition)
			throws GitTagsetHandlerException;
}
