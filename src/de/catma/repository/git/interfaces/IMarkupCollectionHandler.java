package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.MarkupCollectionHandlerException;

import javax.annotation.Nullable;

public interface IMarkupCollectionHandler {
	String create(String name, String description, String sourceDocumentId, String projectId,
				  @Nullable String markupCollectionId)
			throws MarkupCollectionHandlerException;

	void delete(String markupCollectionId) throws MarkupCollectionHandlerException;

	void addTagset(String tagsetId);
	void removeTagset(String tagsetId);
}
