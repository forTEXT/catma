package de.catma.repository.git.graph.interfaces;

import java.nio.file.Path;
import java.util.Collection;

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
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;

public interface GraphProjectHandler {
	Indexer createIndexer();

	void ensureProjectRevisionIsLoaded(
			ExecutionListener<TagManager> openProjectListener,
			ProgressListener progressListener,
			String revisionHash, 
			TagManager tagManager,
			TagsetsProvider tagsetsProvider,
			DocumentsProvider documentsProvider,
			CollectionsProvider collectionsProvider,
			boolean forceGraphReload,
			BackgroundService backgroundService
	);

	void updateProjectRevision(String oldRevisionHash, String newRevisionHash);

	// tagset & tag operations
	Collection<TagsetDefinition> getTagsets(String rootRevisionHash);

	// collection operations
	AnnotationCollection getCollection(String rootRevisionHash, AnnotationCollectionReference collectionReference) throws Exception;

	void addCollection(
			String rootRevisionHash,
			String collectionId,
			String name,
			SourceDocumentReference document,
			TagLibrary tagLibrary,
			String oldRootRevisionHash
	);

	void removeCollection(String rootRevisionHash, AnnotationCollectionReference collectionReference, String oldRootRevisionHash);

	Multimap<String, TagReference> getTagReferencesByCollectionId(TagsetDefinition tagsetDefinition) throws Exception;

	Multimap<String, TagReference> getTagReferencesByCollectionId(TagDefinition tag) throws Exception;

	// document operations
	boolean hasDocument(String rootRevisionHash, String documentId);

	SourceDocumentReference getSourceDocumentReference(String sourceDocumentID);

	Collection<SourceDocumentReference> getDocuments(String rootRevisionHash);

	SourceDocument getSourceDocument(String rootRevisionHash, String sourceDocumentId) throws Exception;

	void addSourceDocument(String oldRootRevisionHash, String rootRevisionHash, SourceDocument document, Path tokenizedSourceDocumentPath) throws Exception;

	void removeDocument(String rootRevisionHash, SourceDocumentReference document, String oldRootRevisionHash);
}
