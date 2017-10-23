package de.catma.repository.git.interfaces;

import de.catma.repository.git.exceptions.MarkupCollectionHandlerException;
import de.catma.repository.jsonld.TagReferenceJsonLd;

import javax.annotation.Nullable;

public interface IMarkupCollectionHandler {
	String create(String name, String description, String sourceDocumentId, String projectId,
				  @Nullable String markupCollectionId)
			throws MarkupCollectionHandlerException;

	void delete(String markupCollectionId) throws MarkupCollectionHandlerException;

	void addTagset(String markupCollectionId, String tagsetId) throws MarkupCollectionHandlerException;

	void removeTagset(String markupCollectionId, String tagsetId) throws MarkupCollectionHandlerException;

	void addTagInstance(String markupCollectionId, TagReferenceJsonLd tagReference)
			throws MarkupCollectionHandlerException;
}
