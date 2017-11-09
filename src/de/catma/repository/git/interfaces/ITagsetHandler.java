package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.TagsetHandlerException;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagsetDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public interface ITagsetHandler {
	String create(
			@Nonnull String projectId,
			@Nullable String tagsetId,
			@Nonnull String name,
			@Nullable String description
	) throws TagsetHandlerException;

	void delete(String tagsetId) throws TagsetHandlerException;

	String addTagDefinition(@Nonnull String projectId, @Nonnull String tagsetId, @Nonnull TagDefinition tagDefinition)
			throws TagsetHandlerException;

	TagsetDefinition open(String tagsetId, String projectId) throws TagsetHandlerException, IOException;
}
