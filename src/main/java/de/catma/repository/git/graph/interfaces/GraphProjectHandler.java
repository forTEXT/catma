package de.catma.repository.git.graph.interfaces;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;

public interface GraphProjectHandler {
	void ensureProjectRevisionIsLoaded(
			ExecutionListener<TagManager> openProjectListener,
			ProgressListener progressListener,
			String revisionHash, 
			TagManager tagManager,
			Supplier<List<TagsetDefinition>> tagsetsSupplier,
			Supplier<List<SourceDocument>> documentsSupplier,
			CollectionsSupplier collectionsSupplier, 
			boolean forceGraphReload, BackgroundService backgroundService
	) throws Exception;

	void addSourceDocument(String oldRootRevisionHash, String rootRevisionHash, SourceDocument document, Path tokenizedSourceDocumentPath) throws Exception;

	void updateSourceDocument(String rootRevisionHash, SourceDocumentReference sourceDocument, String oldRootRevisionHash) throws Exception;

	Collection<SourceDocumentReference> getDocuments(String rootRevisionHash) throws Exception;

	SourceDocument getSourceDocument(String rootRevisionHash, String sourceDocumentId) throws Exception;

	void addCollection(
			String rootRevisionHash,
			String collectionId,
			String name,
			SourceDocumentReference document,
			TagLibrary tagLibrary,
			String oldRootRevisionHash
	) throws Exception;

	void addTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception;

	void addTagDefinition(String rootRevisionHash, TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception;

	void updateTagDefinition(String rootRevisionHash, TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception;

	Collection<TagsetDefinition> getTagsets(String rootRevisionHash) throws Exception;

	void addPropertyDefinition(
			String rootRevisionHash, PropertyDefinition propertyDefinition, TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash
	) throws Exception;

	void createOrUpdatePropertyDefinition(
			String rootRevisionHash, PropertyDefinition propertyDefinition, TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash
	) throws Exception;

	AnnotationCollection getCollection(String rootRevisionHash, AnnotationCollectionReference collectionReference) throws Exception;

	void addTagReferences(String rootRevisionHash, AnnotationCollection collection, List<TagReference> tagReferences) throws Exception;

	void removeTagReferences(String rootRevisionHash, AnnotationCollection collection, List<TagReference> tagReferences) throws Exception;

	void removeProperties(String rootRevisionHash, String collectionId, String propertyDefId) throws Exception;

	void updateProperties(String rootRevisionHash, AnnotationCollection collection, TagInstance tagInstance, Collection<Property> properties) throws Exception;

	Multimap<String, String> getAnnotationIdsByCollectionId(TagDefinition tag) throws Exception;

	Multimap<String, TagReference> getTagReferencesByCollectionId(TagDefinition tag) throws Exception;

	void removeTagInstances(String rootRevisionHash, String collectionId, Collection<String> tagInstanceIds) throws Exception;

	void removeTagDefinition(String rootRevisionHash, TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception;

	void removePropertyDefinition(
			String rootRevisionHash, PropertyDefinition propertyDefinition, TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash
	) throws Exception;

	void removeTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception;

	void updateTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception;

	void updateCollection(String rootRevisionHash, AnnotationCollectionReference collectionRef, String oldRootRevisionHash) throws Exception;

	void removeCollection(String rootRevisionHash, AnnotationCollectionReference collectionReference, String oldRootRevisionHash) throws Exception;

	void removeDocument(String rootRevisionHash, SourceDocumentReference document, String oldRootRevisionHash) throws Exception;

	boolean hasDocument(String rootRevisionHash, String documentId);

	Multimap<String, String> getAnnotationIdsByCollectionId(TagsetDefinition tagsetDefinition) throws Exception;

	Multimap<String, TagReference> getTagReferencesByCollectionId(TagsetDefinition tagsetDefinition) throws Exception;

	void updateProject(String oldRootRevisionHash, String rootRevisionHash) throws IOException;

	Indexer createIndexer();

	SourceDocumentReference getSourceDocumentReference(String sourceDocumentID);
}