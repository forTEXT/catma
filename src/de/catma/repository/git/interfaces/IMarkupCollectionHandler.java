package de.catma.repository.git.interfaces;

public interface IMarkupCollectionHandler {
	String create(String name, String description);
	void delete(String markupCollectionId);

	void addTagset(String tagsetId);
	void removeTagset(String tagsetId);

	void addSourceDocument(String sourceDocumentId);
	void removeSourceDocument(String sourceDocumentId);
}
