package de.catma.repository.git.graph.lazy;

import com.google.common.cache.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
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
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.indexer.Indexer;
import de.catma.project.ProjectReference;
import de.catma.repository.git.graph.interfaces.*;
import de.catma.tag.*;
import de.catma.user.User;
import de.catma.util.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LazyGraphProjectHandler implements GraphProjectHandler {
	private final Logger logger = Logger.getLogger(LazyGraphProjectHandler.class.getName());

	private final ProjectReference projectReference;
	private final User user;
	private final DocumentFileURIProvider documentFileURIProvider;
	private final CommentsProvider commentsProvider;
	private final DocumentIndexProvider documentIndexProvider;
	private final CollectionProvider collectionProvider;

	private final LoadingCache<String, SourceDocument> documentCache;
	private final LoadingCache<String, AnnotationCollection> collectionCache;

	private final Map<String, SourceDocumentReference> docRefsById = Maps.newHashMap();
	private TagManager tagManager;
	private String revisionHash = "";

	public LazyGraphProjectHandler(
			ProjectReference projectReference, 
			User user, 
			DocumentFileURIProvider documentFileURIProvider,
			CommentsProvider commentsProvider,
			DocumentProvider documentProvider,
			DocumentIndexProvider documentIndexProvider,
			CollectionProvider collectionProvider
	) {
		this.projectReference = projectReference;
		this.user = user;
		this.documentFileURIProvider = documentFileURIProvider;
		this.commentsProvider = commentsProvider;
		this.documentIndexProvider = documentIndexProvider;
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
								StandardContentHandler standardContentHandler = new StandardContentHandler();
								standardContentHandler.setSourceDocumentInfo(
										LazyGraphProjectHandler.this.docRefsById.get(key).getSourceDocumentInfo()
								);
								return new SourceDocument(key, standardContentHandler);
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
	public void ensureProjectRevisionIsLoaded(
			ExecutionListener<TagManager> openProjectListener,
			ProgressListener progressListener,
			String revisionHash,
			TagManager tagManager,
			TagsetsProvider tagsetsProvider,
			DocumentsProvider documentsProvider,
			CollectionsProvider collectionsProvider,
			boolean forceGraphReload,
			BackgroundService backgroundService
	) {
		if (this.revisionHash.equals(revisionHash) && !forceGraphReload) {
			openProjectListener.done(this.tagManager);
			return;
		}

		LoadJob loadJob = new LoadJob(
				projectReference,
				revisionHash,
				tagManager,
				tagsetsProvider,
				documentsProvider,
				collectionsProvider,
				documentFileURIProvider
		);

		backgroundService.submit(
				loadJob,
				new ExecutionListener<Pair<TagManager, Map<String, SourceDocumentReference>>>() {
					@Override
					public void done(Pair<TagManager, Map<String, SourceDocumentReference>> result) {
						logger.info(
								String.format(
										"LoadJob has finished for project \"%s\" with ID %s",
										projectReference.getName(),
										projectReference.getProjectId()
								)
						);
						LazyGraphProjectHandler.this.docRefsById.putAll(result.getSecond());
						LazyGraphProjectHandler.this.tagManager = result.getFirst();
						LazyGraphProjectHandler.this.revisionHash = revisionHash;
						openProjectListener.done(result.getFirst());
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
	public void addSourceDocument(
			String oldRootRevisionHash,
			String rootRevisionHash,
			SourceDocument document,
			Path tokenizedSourceDocumentPath
	) throws Exception {
		document.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet().setURI(
				documentFileURIProvider.getDocumentFileURI(document.getUuid())
		);
		docRefsById.put(document.getUuid(), new SourceDocumentReference(document.getUuid(), document.getSourceContentHandler()));
		documentCache.put(document.getUuid(), document);
		revisionHash = rootRevisionHash;
	}

	@Override
	public void updateSourceDocument(String rootRevisionHash, SourceDocumentReference sourceDocument, String oldRootRevisionHash)
			throws Exception {
		this.revisionHash = rootRevisionHash;
	}

	@Override
	public Collection<SourceDocumentReference> getDocuments(String rootRevisionHash) throws Exception {
		return Collections.unmodifiableCollection(docRefsById.values());
	}

	@Override
	public SourceDocument getSourceDocument(String rootRevisionHash, String sourceDocumentId) throws Exception {
		return documentCache.get(sourceDocumentId);
	}

	@Override
	public void addCollection(String rootRevisionHash, String collectionId, String name, SourceDocumentReference document,
			TagLibrary tagLibrary, String oldRootRevisionHash) throws Exception {
		
		AnnotationCollection collection = 
				new AnnotationCollection(
						collectionId, new ContentInfoSet(name), tagLibrary, 
						document.getUuid(), null, user.getIdentifier());
		this.collectionCache.put(collectionId, collection);
				
		document.addUserMarkupCollectionReference(
				new AnnotationCollectionReference(
						collection.getUuid(),  
						collection.getContentInfoSet(),  
						collection.getSourceDocumentId(), 
						collection.getForkedFromCommitURL(),
						collection.getResponsibleUser()));
		this.revisionHash = rootRevisionHash;
	}

	@Override
	public void addTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		this.revisionHash = rootRevisionHash;
	}

	@Override
	public void addTagDefinition(String rootRevisionHash, TagDefinition tag, TagsetDefinition tagset,
			String oldRootRevisionHash) throws Exception {
		this.revisionHash = rootRevisionHash;
	}

	@Override
	public void updateTagDefinition(String rootRevisionHash, TagDefinition tag, TagsetDefinition tagset,
			String oldRootRevisionHash) throws Exception {
		this.revisionHash = rootRevisionHash;
	}

	@Override
	public Collection<TagsetDefinition> getTagsets(String rootRevisionHash) throws Exception {
		return this.tagManager.getTagLibrary().getTagsetDefinitions();
	}

	@Override
	public void addPropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition, TagDefinition tag,
			TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
		this.revisionHash = rootRevisionHash;
	}

	@Override
	public void createOrUpdatePropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
		this.revisionHash = rootRevisionHash;
	}

	@Override
	public AnnotationCollection getCollection(String rootRevisionHash,
			AnnotationCollectionReference collectionReference) throws Exception {
		return collectionCache.get(collectionReference.getId());
	}

	@Override
	public void addTagReferences(String rootRevisionHash, AnnotationCollection collection,
			List<TagReference> tagReferences) throws Exception {
		// noop
	}

	@Override
	public void removeTagReferences(String rootRevisionHash, AnnotationCollection collection,
			List<TagReference> tagReferences) throws Exception {
		// noop

	}

	@Override
	public void removeProperties(String rootRevisionHash, String collectionId, String propertyDefId) throws Exception {
		// noop

	}

	@Override
	public void updateProperties(String rootRevisionHash, AnnotationCollection collection, TagInstance tagInstance,
			Collection<Property> properties) throws Exception {
		// noop

	}

	@Override
	public Multimap<String, String> getAnnotationIdsByCollectionId(TagDefinition tagDefinition) throws Exception {
		Multimap<String, String> result = HashMultimap.create();
		Set<AnnotationCollectionReference> collectionReferences = docRefsById.values()
				.stream()
				.map(SourceDocumentReference::getUserMarkupCollectionRefs)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());

		for (AnnotationCollectionReference collectionReference : collectionReferences) {
			AnnotationCollection collection = collectionCache.get(collectionReference.getId());
			collection.getTagReferences(tagDefinition)
					.stream()
					.map(TagReference::getTagInstanceId)
					.collect(Collectors.toSet())
					.forEach(tagInstanceId -> result.put(collectionReference.getId(), tagInstanceId));
		}

		return result;
	}

	@Override
	public Multimap<String, TagReference> getTagReferencesByCollectionId(TagDefinition tag) throws Exception {
		Multimap<String, TagReference> result = ArrayListMultimap.create();
		Set<AnnotationCollectionReference> collectionReferences = docRefsById.values()
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

	@Override
	public void removeTagInstances(String rootRevisionHash, String collectionId, Collection<String> tagInstanceIds)
			throws Exception {
		// noop
	}

	@Override
	public void removeTagDefinition(String rootRevisionHash, TagDefinition tag, TagsetDefinition tagset,
			String oldRootRevisionHash) throws Exception {
		this.revisionHash = rootRevisionHash;
	}

	@Override
	public void removePropertyDefinition(String rootRevisionHash, PropertyDefinition propertyDefinition,
			TagDefinition tag, TagsetDefinition tagset, String oldRootRevisionHash) throws Exception {
		this.revisionHash = rootRevisionHash;
	}

	@Override
	public void removeTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		this.revisionHash = rootRevisionHash;

	}

	@Override
	public void updateTagset(String rootRevisionHash, TagsetDefinition tagset, String oldRootRevisionHash)
			throws Exception {
		this.revisionHash = rootRevisionHash;
	}

	@Override
	public void updateCollection(String rootRevisionHash, AnnotationCollectionReference collectionRef,
			String oldRootRevisionHash) throws Exception {
		this.revisionHash = rootRevisionHash;
	}

	@Override
	public void removeCollection(String rootRevisionHash, AnnotationCollectionReference collectionReference,
			String oldRootRevisionHash) throws Exception {
		this.collectionCache.invalidate(collectionReference.getId());
		this.revisionHash = rootRevisionHash;
	}

	@Override
	public void removeDocument(String rootRevisionHash, SourceDocumentReference document, String oldRootRevisionHash)
			throws Exception {
		
		this.collectionCache.invalidateAll(
				document.getUserMarkupCollectionRefs()
				.stream()
				.map(AnnotationCollectionReference::getId)
				.collect(Collectors.toSet()));
		this.documentCache.invalidate(document.getUuid());
		this.docRefsById.remove(document.getUuid());
		this.revisionHash = rootRevisionHash;
	}

	@Override
	public boolean hasDocument(String rootRevisionHash, String documentId) {
		
		return this.docRefsById.containsKey(documentId);
	}

	@Override
	public Multimap<String, String> getAnnotationIdsByCollectionId(TagsetDefinition tagsetDefinition) throws Exception {
		Multimap<String, String> result = HashMultimap.create();
		Set<AnnotationCollectionReference> collectionReferences = docRefsById.values()
				.stream()
				.map(SourceDocumentReference::getUserMarkupCollectionRefs)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());

		for (AnnotationCollectionReference collectionReference : collectionReferences) {
			AnnotationCollection collection = collectionCache.get(collectionReference.getId());
			collection.getTagReferences(tagsetDefinition)
					.stream()
					.map(TagReference::getTagInstanceId)
					.collect(Collectors.toSet())
					.forEach(tagInstanceId -> result.put(collectionReference.getId(), tagInstanceId));
		}

		return result;
	}

	@Override
	public Multimap<String, TagReference> getTagReferencesByCollectionId(TagsetDefinition tagsetDefinition) throws Exception {
		Multimap<String, TagReference> result = ArrayListMultimap.create();
		Set<AnnotationCollectionReference> collectionReferences = docRefsById.values()
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
	public void updateProject(String oldRootRevisionHash, String rootRevisionHash) throws IOException {
		this.revisionHash = rootRevisionHash;
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
	public SourceDocumentReference getSourceDocumentReference(String sourceDocumentID) {
		return docRefsById.get(sourceDocumentID);
	}
}
