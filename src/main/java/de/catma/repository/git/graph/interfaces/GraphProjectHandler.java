package de.catma.repository.git.graph.interfaces;

import com.google.common.collect.Multimap;
import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.indexer.Indexer;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

import javax.lang.model.type.NullType;
import java.util.Collection;

public interface GraphProjectHandler {
	Indexer createIndexer();

	void ensureProjectRevisionIsLoaded(
			ExecutionListener<NullType> openProjectListener,
			ProgressListener progressListener,
			String revisionHash,
			CollectionsProvider collectionsProvider,
			boolean forceGraphReload,
			BackgroundService backgroundService
	);

	void updateProjectRevision(String oldRevisionHash, String newRevisionHash);

	// collection operations
	AnnotationCollection getAnnotationCollection(AnnotationCollectionReference annotationCollectionRef) throws Exception;

	void addAnnotationCollection(
			String annotationCollectionId,
			String name,
			SourceDocumentReference sourceDocumentRef,
			TagLibrary tagLibrary,
			String oldRevisionHash,
			String newRevisionHash
	);

	void removeAnnotationCollection(AnnotationCollectionReference annotationCollectionRef, String oldRevisionHash, String newRevisionHash);

	Multimap<String, TagReference> getTagReferencesByCollectionId(TagsetDefinition tagsetDefinition) throws Exception;

	Multimap<String, TagReference> getTagReferencesByCollectionId(TagDefinition tag) throws Exception;

	// document operations
	boolean hasSourceDocument(String sourceDocumentId);

	SourceDocumentReference getSourceDocumentReference(String sourceDocumentId);

	Collection<SourceDocumentReference> getSourceDocumentReferences();

	SourceDocument getSourceDocument(String sourceDocumentId) throws Exception;

	void addSourceDocument(SourceDocument sourceDocument, String oldRevisionHash, String newRevisionHash) throws Exception;

	void removeSourceDocument(SourceDocumentReference sourceDocumentRef, String oldRevisionHash, String newRevisionHash);
}
