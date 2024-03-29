package de.catma.repository.git.graph.lazy;

import com.google.common.cache.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.indexer.Indexer;
import de.catma.project.ProjectReference;
import de.catma.repository.git.graph.interfaces.*;
import de.catma.tag.*;
import de.catma.user.User;

import javax.lang.model.type.NullType;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LazyGraphProjectHandler implements GraphProjectHandler {
	private final Logger logger = Logger.getLogger(LazyGraphProjectHandler.class.getName());

	private final ProjectReference projectReference;
	private final User user;

	private final TagManager tagManager;
	private final TagsetsProvider tagsetsProvider;

	private final DocumentsProvider documentsProvider;
	private final DocumentProvider documentProvider;
	private final DocumentIndexProvider documentIndexProvider;

	private final CommentsProvider commentsProvider;

	private final CollectionProvider collectionProvider;

	private final LoadingCache<String, SourceDocument> documentCache;
	private final LoadingCache<String, AnnotationCollection> collectionCache;

	private Map<String, SourceDocumentReference> sourceDocumentRefsById = Maps.newHashMap();
	private String revisionHash = "";

	public LazyGraphProjectHandler(
			ProjectReference projectReference,
			User user,
			TagManager tagManager,
			TagsetsProvider tagsetsProvider,
			DocumentsProvider documentsProvider,
			DocumentProvider documentProvider,
			DocumentIndexProvider documentIndexProvider,
			CommentsProvider commentsProvider,
			CollectionProvider collectionProvider
	) {
		this.projectReference = projectReference;
		this.user = user;
		this.tagManager = tagManager;
		this.tagsetsProvider = tagsetsProvider;
		this.documentsProvider = documentsProvider;
		this.documentProvider = documentProvider;
		this.documentIndexProvider = documentIndexProvider;
		this.commentsProvider = commentsProvider;
		this.collectionProvider = collectionProvider;

		this.documentCache = CacheBuilder.newBuilder()
				.maximumSize(10)
				.removalListener(
						new RemovalListener<String, SourceDocument>() {
							@Override
							public void onRemoval(RemovalNotification<String, SourceDocument> notification) {
								notification.getValue().unload();
							}
						}
				)
				.build(
						new CacheLoader<String, SourceDocument>() {
							@Override
							public SourceDocument load(String key) throws Exception {
								return LazyGraphProjectHandler.this.documentProvider.getDocument(key);
							}
						}
				);

		this.collectionCache = CacheBuilder.newBuilder()
				.maximumSize(20)
				.build(
						new CacheLoader<String, AnnotationCollection>() {
							@Override
							public AnnotationCollection load(String key) throws Exception {
								return LazyGraphProjectHandler.this.collectionProvider.getCollection(
										key, LazyGraphProjectHandler.this.tagManager.getTagLibrary()
								);
							}
						}
				);
	}

	@Override
	public Indexer createIndexer() {
		return new LazyGraphProjectIndexer(
				commentsProvider,
				new DocumentProvider() {
					@Override
					public SourceDocument getDocument(String documentId) throws IOException {
						try {
							return documentCache.get(documentId);
						}
						catch (ExecutionException e) {
							return null;
						}
					}
				},
				documentIndexProvider,
				new CollectionProvider() {
					@Override
					public AnnotationCollection getCollection(String collectionId) {
						try {
							return collectionCache.get(collectionId);
						}
						catch (ExecutionException e) {
							return null;
						}
					}
				},
				new TagLibraryProvider() {
					@Override
					public TagLibrary getTagLibrary() {
						return tagManager.getTagLibrary();
					}
				}
		);
	}

	@Override
	public void ensureProjectRevisionIsLoaded(
			String revisionHash,
			boolean forceGraphReload,
			CollectionsProvider collectionsProvider,
			BackgroundService backgroundService,
			ExecutionListener<NullType> openProjectListener,
			ProgressListener progressListener
	) {
		if (this.revisionHash.equals(revisionHash) && !forceGraphReload) {
			openProjectListener.done(null);
			return;
		}

		LoadJob loadJob = new LoadJob(
				projectReference,
				tagManager,
				tagsetsProvider,
				documentsProvider,
				collectionsProvider
		);

		backgroundService.submit(
				loadJob,
				new ExecutionListener<Map<String, SourceDocumentReference>>() {
					@Override
					public void done(Map<String, SourceDocumentReference> sourceDocumentRefsById) {
						logger.info(
								String.format(
										"LoadJob has finished for project \"%s\" with ID %s",
										projectReference.getName(),
										projectReference.getProjectId()
								)
						);

						LazyGraphProjectHandler.this.sourceDocumentRefsById = sourceDocumentRefsById;
						LazyGraphProjectHandler.this.revisionHash = revisionHash;

						documentCache.invalidateAll();
						collectionCache.invalidateAll();

						openProjectListener.done(null);
					}
					@Override
					public void error(Throwable t) {
						openProjectListener.error(t);
					}
				},
				progressListener
		);
	}

	@Override
	public void updateProjectRevision(String oldRevisionHash, String newRevisionHash) {
		if (newRevisionHash.equals(oldRevisionHash)) {
			logger.info(
					String.format("No changes in project \"%s\" with ID %s", projectReference.getName(), projectReference.getProjectId())
			);
			return;
		}

		revisionHash = newRevisionHash;

		logger.info(
				String.format(
						"Updated revision of project \"%1$s\" with ID %2$s. Old: %3$s, New: %4$s",
						projectReference.getName(),
						projectReference.getProjectId(),
						oldRevisionHash,
						newRevisionHash
				)
		);
	}

	// collection operations
	@Override
	public AnnotationCollection getAnnotationCollection(AnnotationCollectionReference annotationCollectionRef) throws Exception {
		return collectionCache.get(annotationCollectionRef.getId());
	}

	@Override
	public void addAnnotationCollection(
			String annotationCollectionId,
			String name,
			SourceDocumentReference sourceDocumentRef,
			TagLibrary tagLibrary,
			String oldRevisionHash,
			String newRevisionHash
	) {
		AnnotationCollection collection = new AnnotationCollection(
				annotationCollectionId,
				new ContentInfoSet(name),
				tagLibrary,
				sourceDocumentRef.getUuid(),
				null,
				user.getIdentifier()
		);
		collectionCache.put(annotationCollectionId, collection);

		sourceDocumentRef.addUserMarkupCollectionReference(
				new AnnotationCollectionReference(
						collection.getUuid(),
						collection.getContentInfoSet(),
						collection.getSourceDocumentId(),
						collection.getForkedFromCommitURL(),
						collection.getResponsibleUser()
				)
		);

		updateProjectRevision(oldRevisionHash, newRevisionHash);
	}

	@Override
	public void removeAnnotationCollection(AnnotationCollectionReference annotationCollectionRef, String oldRevisionHash, String newRevisionHash) {
		collectionCache.invalidate(annotationCollectionRef.getId());
		updateProjectRevision(oldRevisionHash, newRevisionHash);
	}

	@Override
	public Multimap<String, TagReference> getTagReferencesByCollectionId(TagsetDefinition tagsetDefinition) throws Exception {
		Multimap<String, TagReference> result = ArrayListMultimap.create();

		Set<AnnotationCollectionReference> collectionReferences = sourceDocumentRefsById.values()
				.stream()
				.map(SourceDocumentReference::getUserMarkupCollectionRefs)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());

		for (AnnotationCollectionReference collectionReference : collectionReferences) {
			AnnotationCollection collection = collectionCache.get(collectionReference.getId());
			collection.getTagReferences(tagsetDefinition)
					.stream()
					.forEach(tagReference -> result.put(collectionReference.getId(), tagReference));
		}

		return result;
	}

	@Override
	public Multimap<String, TagReference> getTagReferencesByCollectionId(TagDefinition tag) throws Exception {
		Multimap<String, TagReference> result = ArrayListMultimap.create();

		Set<AnnotationCollectionReference> collectionReferences = sourceDocumentRefsById.values()
				.stream()
				.map(SourceDocumentReference::getUserMarkupCollectionRefs)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());

		for (AnnotationCollectionReference collectionReference : collectionReferences) {
			AnnotationCollection collection = collectionCache.get(collectionReference.getId());
			collection.getTagReferences(tag)
					.stream()
					.forEach(tagReference -> result.put(collectionReference.getId(), tagReference));
		}

		return result;
	}

	// document operations
	@Override
	public boolean hasSourceDocument(String sourceDocumentId) {
		return sourceDocumentRefsById.containsKey(sourceDocumentId);
	}

	@Override
	public SourceDocumentReference getSourceDocumentReference(String sourceDocumentId) {
		return sourceDocumentRefsById.get(sourceDocumentId);
	}

	@Override
	public Collection<SourceDocumentReference> getSourceDocumentReferences() {
		return Collections.unmodifiableCollection(sourceDocumentRefsById.values());
	}

	@Override
	public SourceDocument getSourceDocument(String sourceDocumentId) throws Exception {
		return documentCache.get(sourceDocumentId);
	}

	@Override
	public void addSourceDocument(SourceDocument sourceDocument, String oldRevisionHash, String newRevisionHash) throws Exception {
		sourceDocumentRefsById.put(sourceDocument.getUuid(), new SourceDocumentReference(sourceDocument.getUuid(), sourceDocument.getSourceContentHandler()));
		documentCache.put(sourceDocument.getUuid(), sourceDocument);

		updateProjectRevision(oldRevisionHash, newRevisionHash);
	}

	@Override
	public void removeSourceDocument(SourceDocumentReference sourceDocumentRef, String oldRevisionHash, String newRevisionHash) {
		collectionCache.invalidateAll(
				sourceDocumentRef.getUserMarkupCollectionRefs()
						.stream()
						.map(AnnotationCollectionReference::getId)
						.collect(Collectors.toSet())
		);
		documentCache.invalidate(sourceDocumentRef.getUuid());
		sourceDocumentRefsById.remove(sourceDocumentRef.getUuid());

		updateProjectRevision(oldRevisionHash, newRevisionHash);
	}
}
