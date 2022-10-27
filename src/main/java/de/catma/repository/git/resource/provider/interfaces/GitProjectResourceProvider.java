package de.catma.repository.git.resource.provider.interfaces;

import java.io.IOException;
import java.util.List;

import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

public interface GitProjectResourceProvider {
	public List<TagsetDefinition> getTagsets();
	public List<AnnotationCollectionReference> getCollectionReferences();
	public List<AnnotationCollection> getCollections(
			TagLibrary tagLibrary, ProgressListener progressListener, 
			boolean withOrphansHandling) throws IOException;
	public AnnotationCollection getCollection(
			String collectionId, 
			TagLibrary tagLibrary) throws IOException;
	
	public List<SourceDocument> getDocuments();
	public SourceDocument getDocument(String documentId) throws IOException;
	
	public boolean isReadOnly();
}
