package de.catma.repository.git.resource.provider.interfaces;

import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

import java.io.IOException;
import java.util.List;

public interface GitProjectResourceProvider {
	boolean isReadOnly();
	List<TagsetDefinition> getTagsets();
	List<AnnotationCollectionReference> getCollectionReferences();
	List<AnnotationCollection> getCollections(TagLibrary tagLibrary, ProgressListener progressListener, boolean withOrphansHandling) throws IOException;
	AnnotationCollection getCollection(String collectionId, TagLibrary tagLibrary) throws IOException;
	List<SourceDocument> getDocuments();
	SourceDocument getDocument(String documentId) throws IOException;
}
