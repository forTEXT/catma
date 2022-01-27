package de.catma.repository.git;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.tika.mime.MediaType;
import org.eclipse.jgit.api.Status;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.vaadin.ui.UI;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.contenthandler.StandardContentHandler;
import de.catma.indexer.IndexedProject;
import de.catma.indexer.Indexer;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.project.CommitInfo;
import de.catma.project.OpenProjectListener;
import de.catma.project.ProjectReference;
import de.catma.project.event.ChangeType;
import de.catma.project.event.CollectionChangeEvent;
import de.catma.project.event.CommentChangeEvent;
import de.catma.project.event.DocumentChangeEvent;
import de.catma.project.event.ReplyChangeEvent;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.graph.CommentProvider;
import de.catma.repository.git.graph.FileInfoProvider;
import de.catma.repository.git.graph.GraphProjectHandler;
import de.catma.repository.git.graph.tp.TPGraphProjectHandler;
import de.catma.repository.git.managers.StatusPrinter;
import de.catma.serialization.TagLibrarySerializationHandler;
import de.catma.serialization.TagsetDefinitionImportStatus;
import de.catma.serialization.tei.TeiSerializationHandlerFactory;
import de.catma.serialization.tei.TeiTagLibrarySerializationHandler;
import de.catma.serialization.tei.TeiUserMarkupCollectionDeserializer;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class GraphWorktreeProject implements IndexedProject {
	
	private static final String UTF8_CONVERSION_FILE_EXTENSION = "txt";
	private static final String ORIG_INFIX = "_orig";
	private static final String TOKENIZED_FILE_EXTENSION = "json";

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private PropertyChangeSupport propertyChangeSupport;

	private User user;
	private final GitProjectHandler gitProjectHandler;
	private ProjectReference projectReference;
	private String rootRevisionHash;
	private GraphProjectHandler graphProjectHandler;
	private final String tempDir;
	private BackgroundService backgroundService;

	private boolean tagManagerListenersEnabled = true;

	private IDGenerator idGenerator = new IDGenerator();
	private final TagManager tagManager;
	private PropertyChangeListener tagsetDefinitionChangedListener;
	private PropertyChangeListener tagDefinitionChangedListener;
	private PropertyChangeListener userDefinedPropertyChangedListener;
	private Indexer indexer;
	private EventBus eventBus;
	
	private LoadingCache<String, SourceDocument> documentCache;

	public GraphWorktreeProject(User user,
								GitProjectHandler gitProjectHandler,
								ProjectReference projectReference,
								TagManager tagManager,
								BackgroundService backgroundService,
								EventBus eventBus) {
		this.user = user;
		this.gitProjectHandler = gitProjectHandler;
		this.projectReference = projectReference;
		this.tagManager = tagManager;
		this.backgroundService = backgroundService;
		this.eventBus = eventBus;
		this.propertyChangeSupport = new PropertyChangeSupport(this);
		this.graphProjectHandler = 
			new TPGraphProjectHandler(
				this.projectReference, 
				this.user,
				new FileInfoProvider() {
					
					@Override
					public Path getTokenizedSourceDocumentPath(String documentId) throws Exception {
						return GraphWorktreeProject.this.getTokenizedSourceDocumentPath(documentId);
					}
					
					@Override
					public URI getSourceDocumentFileURI(String documentId) throws Exception {
						return getSourceDocumentURI(documentId);
					}
				},
				new CommentProvider() {
					@Override
					public List<Comment> getComments(List<String> documentIdList) throws Exception {
						return GraphWorktreeProject.this.gitProjectHandler.getCommentsWithReplies(documentIdList);
					}
				});
		this.tempDir = CATMAPropertyKey.TempDir.getValue();
		this.indexer = ((TPGraphProjectHandler)this.graphProjectHandler).createIndexer();
    	this.documentCache = 
			CacheBuilder.newBuilder()
			.maximumSize(10)
			.removalListener(new RemovalListener<String, SourceDocument>() {
				@Override
				public void onRemoval(RemovalNotification<String, SourceDocument> notification) {
					notification.getValue().unload();
				}
			})
			.build(new CacheLoader<String, SourceDocument>() {
				@Override
				public SourceDocument load(String key) throws Exception {
					return getUncachedSourceDocument(key);
				}
			});
	}
	
	public SourceDocument getUncachedSourceDocument(String sourceDocumentId) throws Exception {
		return graphProjectHandler.getSourceDocument(this.rootRevisionHash, sourceDocumentId);
	}

	private Path getTokenizedSourceDocumentPath(String documentId) {
		return Paths
			.get(new File(CATMAPropertyKey.GraphDbGitMountBasePath.getValue()).toURI())
			.resolve(gitProjectHandler.getSourceDocumentSubmodulePath(documentId))
			.resolve(documentId + "." + TOKENIZED_FILE_EXTENSION);
	}

	@Override
	public Indexer getIndexer() {
		return indexer;
	}
	
	@Override
	public void open(OpenProjectListener openProjectListener) {
		try {
			logger.info(String.format(
				"Opening Project %1$s with ID %2$s", projectReference.getName(), projectReference.getProjectId()));
			this.rootRevisionHash = gitProjectHandler.getRootRevisionHash();
			logger.info(
				String.format("Revision Hash for Project %1$s is %2$s", projectReference.getProjectId(), this.rootRevisionHash));
						
			logger.info(
				String.format("Checking for conflicts in Project %1$s", projectReference.getProjectId()));
			if (gitProjectHandler.hasConflicts()) {
				gitProjectHandler.initAndUpdateSubmodules();

				openProjectListener.conflictResolutionNeeded(
						new GitConflictedProject(
							projectReference,
							gitProjectHandler, documentId -> getSourceDocumentURI(documentId)));
			}
			else {
				
				gitProjectHandler.initAndUpdateSubmodules();

				gitProjectHandler.removeStaleSubmoduleDirectories();
				
				gitProjectHandler.ensureDevBranches();
				
				gitProjectHandler.verifyCollections();
				
				ProgressListener progressListener = 
					new ProgressListener() {
						
						@Override
						public void setProgress(String value, Object... args) {
							logger.info(String.format(value, args));
							openProjectListener.progress(value, args);
						}
					};
				graphProjectHandler.ensureProjectRevisionIsLoaded(
						new ExecutionListener<TagManager>() {
							
							@Override
							public void error(Throwable t) {
								openProjectListener.failure(t);
							}
							
							@Override
							public void done(TagManager result) {
								logger.info(
									String.format("Loading Tag library for Project %1$s", 
											projectReference.getProjectId()));								
								tagManager.load(result.getTagLibrary());
								
								initTagManagerListeners();
								
								logger.info(
										String.format("Project %1$s is loaded.", 
												projectReference.getProjectId()));
								openProjectListener.ready(GraphWorktreeProject.this);
							}
						},
						progressListener,
						rootRevisionHash,
						tagManager,
						() -> gitProjectHandler.getTagsets(),
						() -> gitProjectHandler.getDocuments(),
						(tagLibrary) -> gitProjectHandler.getCollections(tagLibrary, progressListener),
						false, //forceGraphReload
						backgroundService);
			}
		}
		catch(Exception e) {
			openProjectListener.failure(e);
		}
	}
	
	@Override
	public void printStatus() {
		try {
			Status status = gitProjectHandler.getStatus();
			
			StatusPrinter.print(projectReference.toString(), status, System.out);
			
			for (TagsetDefinition tagset : gitProjectHandler.getTagsets()) {
				status = gitProjectHandler.getStatus(tagset);
				StatusPrinter.print(
						"Tagset " + tagset.getName() + " #"+tagset.getUuid(), 
						status, System.out);
			}
			
			for (AnnotationCollectionReference collectionRef : gitProjectHandler.getDocuments().stream()
					.flatMap(doc -> doc.getUserMarkupCollectionRefs().stream())
					.collect(Collectors.toList())) {
				
				status = gitProjectHandler.getStatus(collectionRef);
				StatusPrinter.print(
					"Collection " + collectionRef.toString() + " #" + collectionRef.getId(), 
					status, System.out);
				
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initTagManagerListeners() {
		tagsetDefinitionChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(final PropertyChangeEvent evt) {
				
				if (!tagManagerListenersEnabled) {
					return;
				}
				try {
					if (evt.getOldValue() == null) { //insert
						final TagsetDefinition tagsetDefinition = 
								(TagsetDefinition)evt.getNewValue();
						
						addTagsetDefinition(tagsetDefinition);
					}
					else if (evt.getNewValue() == null) { //delete
						final TagsetDefinition tagsetDefinition = 
							(TagsetDefinition)evt.getOldValue();
						
						removeTagsetDefinition(tagsetDefinition);
					}
					else { //update
						final TagsetDefinition tagsetDefinition = 
								(TagsetDefinition)evt.getNewValue();
						
						updateTagsetDefinition(tagsetDefinition);

					}
				}
				catch (Exception e) {
					propertyChangeSupport.firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null, 
							e);	
				}
			}
		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagsetDefinitionChanged,
				tagsetDefinitionChangedListener);
		
		tagDefinitionChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(final PropertyChangeEvent evt) {
				
				if (!tagManagerListenersEnabled) {
					return;
				}
				try {
					if (evt.getOldValue() == null) {
						@SuppressWarnings("unchecked")
						final Pair<TagsetDefinition, TagDefinition> args = 
								(Pair<TagsetDefinition, TagDefinition>)evt.getNewValue();
						TagsetDefinition tagsetDefinition = args.getFirst();
						TagDefinition tagDefinition = args.getSecond();
						addTagDefinition(tagDefinition, tagsetDefinition);
					}
					else if (evt.getNewValue() == null) {
						@SuppressWarnings("unchecked")
						final Pair<TagsetDefinition, TagDefinition> args = 
							(Pair<TagsetDefinition, TagDefinition>)evt.getOldValue();
						TagsetDefinition tagsetDefinition = args.getFirst();
						TagDefinition tagDefinition = args.getSecond();
						removeTagDefinition(tagDefinition, tagsetDefinition);
					}
					else {
						TagDefinition tag = (TagDefinition) evt.getNewValue();
						TagsetDefinition tagset = (TagsetDefinition) evt.getOldValue();
						
						updateTagDefinition(tag, tagset);
					}
				}
				catch (Exception e) {
					propertyChangeSupport.firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null, 
							e);				
				}
			}

		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged,
				tagDefinitionChangedListener);	
		
		
		userDefinedPropertyChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				
				if (!tagManagerListenersEnabled) {
					return;
				}
				Object oldValue = evt.getOldValue();
				Object newValue = evt.getNewValue();
				try {
					if (oldValue == null) { // insert
						
						@SuppressWarnings("unchecked")
						Pair<PropertyDefinition, TagDefinition> newPair = 
								(Pair<PropertyDefinition, TagDefinition>)newValue;
						
						PropertyDefinition propertyDefinition = newPair.getFirst();
						TagDefinition tagDefinition = newPair.getSecond();
						
						addPropertyDefinition(propertyDefinition, tagDefinition);
					}
					else if (newValue == null) { // delete
						@SuppressWarnings("unchecked")
						Pair<PropertyDefinition, Pair<TagDefinition, TagsetDefinition>> oldPair = 
								(Pair<PropertyDefinition, Pair<TagDefinition, TagsetDefinition>>)oldValue;
						PropertyDefinition propertyDefinition = oldPair.getFirst();
						TagDefinition tagDefinition = oldPair.getSecond().getFirst();
						TagsetDefinition tagsetDefinition = oldPair.getSecond().getSecond();
						
						removePropertyDefinition(propertyDefinition, tagDefinition, tagsetDefinition);
					}
					else { // update
						PropertyDefinition propertyDefinition = (PropertyDefinition)evt.getNewValue();
						TagDefinition tagDefinition = (TagDefinition)evt.getOldValue();
						updatePropertyDefinition(propertyDefinition, tagDefinition);
					}
				}
				catch (Exception e) {
					propertyChangeSupport.firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null, 
							e);				
				}				
			}
		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.userPropertyDefinitionChanged,
				userDefinedPropertyChangedListener);
	}

	protected void updateTagsetDefinition(TagsetDefinition tagsetDefinition) throws Exception {
		String tagsetRevisionHash = gitProjectHandler.updateTagset(tagsetDefinition);
		
		String oldRootRevisionHash = this.rootRevisionHash;
		
		// project commit
		this.rootRevisionHash = gitProjectHandler.addTagsetSubmoduleToStagedAndCommit(
			tagsetDefinition.getUuid(), 
			String.format("Updated metadata of Tagset %1$s with ID %2$s", 
					tagsetDefinition.getName(), tagsetDefinition.getUuid()));
		
		graphProjectHandler.updateTagset(this.rootRevisionHash, tagsetDefinition, oldRootRevisionHash);
	}

	private void removeTagsetDefinition(TagsetDefinition tagsetDefinition) throws Exception {
		// remove Annotations
		Multimap<String, String> annotationIdsByCollectionId =
			graphProjectHandler.getAnnotationIdsByCollectionId(this.rootRevisionHash, tagsetDefinition);
		
		for (String collectionId : annotationIdsByCollectionId.keySet()) {
			// TODO: check permissions if commit is allowed, if that is not the case skip git removal
			String collectionRevisionHash = gitProjectHandler.removeTagInstancesAndCommit(
				collectionId, annotationIdsByCollectionId.get(collectionId), 
				String.format(
						"Annotations removed, "
						+ "caused by the removal of Tagset %1$s with ID %2$s", 
						tagsetDefinition.getName(),
						tagsetDefinition.getUuid()));
			
			gitProjectHandler.addCollectionToStaged(collectionId);
			
			graphProjectHandler.removeTagInstances(
				this.rootRevisionHash, collectionId,
				annotationIdsByCollectionId.get(collectionId), 
				collectionRevisionHash);
		}
		
		String oldRootRevisionHash = this.rootRevisionHash;
		gitProjectHandler.removeTagset(tagsetDefinition);
		this.rootRevisionHash = gitProjectHandler.getRootRevisionHash(); 
		graphProjectHandler.removeTagset(this.rootRevisionHash, tagsetDefinition, oldRootRevisionHash);
	}

	private void addPropertyDefinition(PropertyDefinition propertyDefinition, TagDefinition tagDefinition) throws Exception {
		
		TagsetDefinition tagsetDefinition = 
			tagManager.getTagLibrary().getTagsetDefinition(tagDefinition);
		
		String tagsetRevision = gitProjectHandler.createOrUpdateTag(
			tagsetDefinition.getUuid(), 
			tagDefinition,
			String.format("Added Property Definition %1$s with ID %2$s to Tag %3$s with ID %4$s",
				propertyDefinition.getName(),
				propertyDefinition.getUuid(),
				tagDefinition.getName(),
				tagDefinition.getUuid()));
		
		String oldRootRevisionHash = this.rootRevisionHash;

		// project commit
		this.rootRevisionHash = gitProjectHandler.addTagsetSubmoduleToStagedAndCommit(
			tagsetDefinition.getUuid(), 
			String.format(
				"Added Property Definition %1$s with ID %2$s to Tag %3$s with ID %4$s in Tagset %5$s with ID %6$s",
				propertyDefinition.getName(),
				propertyDefinition.getUuid(),
				tagDefinition.getName(),
				tagDefinition.getUuid(),
				tagsetDefinition.getName(),
				tagsetDefinition.getUuid()));
		
		graphProjectHandler.addPropertyDefinition(
			rootRevisionHash, propertyDefinition, tagDefinition, tagsetDefinition, oldRootRevisionHash);
	}

	private void removePropertyDefinition(
			PropertyDefinition propertyDefinition, 
			TagDefinition tagDefinition, TagsetDefinition tagsetDefinition) throws Exception {
		
		// remove AnnotationProperties
		Multimap<String, TagReference> annotationIdsByCollectionId =
			graphProjectHandler.getTagReferencesByCollectionId(
					this.rootRevisionHash, propertyDefinition, tagDefinition);
		
		for (String collectionId : annotationIdsByCollectionId.keySet()) {
			// TODO: check permissions if commit is allowed, if that is not the case skip git update
			
			gitProjectHandler.addCollectionToStagedAndCommit(
					collectionId,
					String.format(
						"Autocommitting changes before performing an update of Annotations "
						+ "as part of a Property Definition deletion operation",
						propertyDefinition.getName()),
					false);
			
			Collection<TagReference> tagReferences = 
					annotationIdsByCollectionId.get(collectionId);
			Set<TagInstance> tagInstances = 
			tagReferences
			.stream()
			.map(tagReference -> tagReference.getTagInstance())
			.collect(Collectors.toSet());
			
			tagInstances.forEach(
				tagInstance -> tagInstance.removeUserDefinedProperty(propertyDefinition.getUuid()));
			
			for (TagInstance tagInstance : tagInstances) {
				gitProjectHandler.addOrUpdate(
					collectionId, 
					tagReferences.stream()
						.filter(tagRef -> tagRef.getTagInstanceId().equals(tagInstance.getUuid()))
						.collect(Collectors.toList()), 
					tagManager.getTagLibrary());
			}
			
			String collectionRevisionHash = 
				gitProjectHandler.addCollectionToStagedAndCommit(
					collectionId,
					String.format(
						"Annotation Properties removed, caused by the removal of Tag Property %1$s ", 
						propertyDefinition.getName()),
					false);
			
			graphProjectHandler.removeProperties(
				this.rootRevisionHash, 
				collectionId, collectionRevisionHash, 
				propertyDefinition.getUuid());
		}
		
		String tagsetRevision = gitProjectHandler.removePropertyDefinition(
				propertyDefinition, tagDefinition, tagsetDefinition);
		
		String oldRootRevisionHash = this.rootRevisionHash;
		
		this.rootRevisionHash = 
				gitProjectHandler.commitProject(
					String.format(
						"Removed Property Definition %1$s with ID %2$s "
						+ "from Tag %3$s with ID %4$s in Tagset %5$s with ID %6$s", 
						propertyDefinition.getName(),
						propertyDefinition.getUuid(),
						tagDefinition.getName(),
						tagDefinition.getUuid(),
						tagsetDefinition.getName(),
						tagsetDefinition.getUuid()));
		
		graphProjectHandler.removePropertyDefinition(
			rootRevisionHash, propertyDefinition, tagDefinition, tagsetDefinition, oldRootRevisionHash);
	}

	private void updatePropertyDefinition(PropertyDefinition propertyDefinition, TagDefinition tagDefinition) throws Exception {
		
		TagsetDefinition tagsetDefinition = 
			tagManager.getTagLibrary().getTagsetDefinition(tagDefinition);
		
		String tagsetRevision = gitProjectHandler.createOrUpdateTag(
				tagsetDefinition.getUuid(), tagDefinition,
				String.format(
					"Updated Property Definition %1$s with ID %2$s in Tag %3$s with ID %4$s",
					propertyDefinition.getName(),
					propertyDefinition.getUuid(),
					tagDefinition.getName(),
					tagDefinition.getUuid()));
		
		String oldRootRevisionHash = this.rootRevisionHash;

		// project commit
		this.rootRevisionHash = gitProjectHandler.addTagsetSubmoduleToStagedAndCommit(
			tagsetDefinition.getUuid(), 
			String.format(
				"Updated Property Definition %1$s with ID %2$s in Tag %3$s with ID %4$s in Tagset %5$s with ID %6$s",
				propertyDefinition.getName(),
				propertyDefinition.getUuid(),
				tagDefinition.getName(),
				tagDefinition.getUuid(),
				tagsetDefinition.getName(),
				tagsetDefinition.getUuid()));
		
		graphProjectHandler.createOrUpdatePropertyDefinition(
			rootRevisionHash, propertyDefinition, tagDefinition, tagsetDefinition, oldRootRevisionHash);
	}

	private void addTagDefinition(TagDefinition tagDefinition, TagsetDefinition tagsetDefinition) throws Exception {
		String tagsetRevision = 
			gitProjectHandler.createOrUpdateTag(
					tagsetDefinition.getUuid(), tagDefinition,
					String.format(
						"Added Tag %1$s with ID %2$s",
						tagDefinition.getName(),
						tagDefinition.getUuid()));
		String oldRootRevisionHash = this.rootRevisionHash;
		
		// project commit
		this.rootRevisionHash = gitProjectHandler.addTagsetSubmoduleToStagedAndCommit(
			tagsetDefinition.getUuid(), 
			String.format(
				"Added Tag %1$s with ID %2$s to Tagset %3$s with ID %4$s",
				tagDefinition.getName(),
				tagDefinition.getUuid(),
				tagsetDefinition.getName(),
				tagsetDefinition.getUuid()));
		
		graphProjectHandler.addTagDefinition(
				rootRevisionHash, tagDefinition, tagsetDefinition, oldRootRevisionHash);
	}

	private void updateTagDefinition(TagDefinition tagDefinition, TagsetDefinition tagsetDefinition) throws Exception {
		String tagsetRevision = gitProjectHandler.createOrUpdateTag(
			tagsetDefinition.getUuid(), 
			tagDefinition,
			String.format(
				"Updated Tag %1$s with ID %2$s",
				tagDefinition.getName(),
				tagDefinition.getUuid()));
		
		String oldRootRevisionHash = this.rootRevisionHash;
		
		// project commit
		this.rootRevisionHash = gitProjectHandler.addTagsetSubmoduleToStagedAndCommit(
			tagsetDefinition.getUuid(), 
			String.format(
				"Updated Tag %1$s with ID %2$s in Tagset %3$s with ID %4$s",
				tagDefinition.getName(),
				tagDefinition.getUuid(),
				tagsetDefinition.getName(),
				tagsetDefinition.getUuid()));
		
		graphProjectHandler.updateTagDefinition(
				rootRevisionHash, tagDefinition, tagsetDefinition, oldRootRevisionHash);
	}
	
	private void removeTagDefinition(TagDefinition tagDefinition, TagsetDefinition tagsetDefinition) throws Exception {

		// remove Annotations
		Multimap<String, String> annotationIdsByCollectionId =
			graphProjectHandler.getAnnotationIdsByCollectionId(this.rootRevisionHash, tagDefinition);
		
		for (String collectionId : annotationIdsByCollectionId.keySet()) {
			// TODO: check permissions if commit is allowed, if that is not the case skip git removal
			String collectionRevisionHash = gitProjectHandler.removeTagInstancesAndCommit(
				collectionId, annotationIdsByCollectionId.get(collectionId), 
				String.format(
						"Annotations removed, "
						+ "caused by the removal of Tag %1$s with ID %2$s "
						+ "from Tagset %3$s with ID %4$s", 
						tagDefinition.getName(),
						tagDefinition.getUuid(),
						tagsetDefinition.getName(),
						tagsetDefinition.getUuid()));
			
			gitProjectHandler.addCollectionToStaged(collectionId);
			
			graphProjectHandler.removeTagInstances(
				this.rootRevisionHash, collectionId,
				annotationIdsByCollectionId.get(collectionId), 
				collectionRevisionHash);
		}
		
		// remove Tag
		String tagsetRevision = gitProjectHandler.removeTag(tagDefinition);

		// commit Project
		String oldRootRevisionHash = this.rootRevisionHash;
		this.rootRevisionHash = gitProjectHandler.addTagsetSubmoduleToStagedAndCommit(
				tagsetDefinition.getUuid(),
				String.format(
						"Removed Tag %1$s with ID %2$s "
								+ "from Tagset %3$s with ID %4$s "
								+ "and corresponding Annotations",
								tagDefinition.getName(),
								tagDefinition.getUuid(),
								tagsetDefinition.getName(),
								tagsetDefinition.getUuid()));
		
		graphProjectHandler.removeTagDefinition(
				rootRevisionHash, tagDefinition, tagsetDefinition, oldRootRevisionHash);
			
		for (String collectionId : annotationIdsByCollectionId.keySet()) {
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.tagReferencesChanged.name(), 
					new Pair<>(collectionId, annotationIdsByCollectionId.get(collectionId)), null);
		}

	}

	private void addTagsetDefinition(TagsetDefinition tagsetDefinition) throws Exception {
		String projectRevisionHash = gitProjectHandler.createTagset(
				tagsetDefinition.getUuid(), 
				tagsetDefinition.getName(), 
				tagsetDefinition.getDescription(),
				tagsetDefinition.getForkedFromCommitURL()
		);
		
		tagsetDefinition.setResponsableUser(this.user.getIdentifier());

		String oldRootRevisionHash = this.rootRevisionHash;
		this.rootRevisionHash = gitProjectHandler.getRootRevisionHash();

		graphProjectHandler.addTagset(
				rootRevisionHash, tagsetDefinition, oldRootRevisionHash
		);
	}

	@Override
	public void close() {
		try {
			if (!gitProjectHandler.hasConflicts()) {
				commitAllChanges(
					collectionRef -> String.format(
							"Auto-committing Collection %1$s with ID %2$s on Project close",
							collectionRef.getName(),
							collectionRef.getId()),
					String.format(
							"Auto-committing Project %1$s with ID %2$s on close", 
							projectReference.getName(),
							projectReference.getProjectId()));
			}
		} catch (Exception e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("Error closing Project", e);
		}
		try {
			for (PropertyChangeListener listener : this.propertyChangeSupport.getPropertyChangeListeners()) {
				this.propertyChangeSupport.removePropertyChangeListener(listener);
			}
			this.propertyChangeSupport = null;
			this.eventBus = null;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Error closing Project", e);
		}
	}

	@Override
	public void addPropertyChangeListener(
			RepositoryChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		this.propertyChangeSupport.addPropertyChangeListener(
				propertyChangeEvent.name(), propertyChangeListener);
	}
	
	@Override
	public void removePropertyChangeListener(
			RepositoryChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		if (this.propertyChangeSupport != null) {
			this.propertyChangeSupport.removePropertyChangeListener(
					propertyChangeEvent.name(), propertyChangeListener);
		}
	}

	@Override
	public String getName() {
		return projectReference.getName();
	}
	
	@Override
	public String getDescription() {
		return projectReference.getDescription();
	}

	@Override
	public String getProjectId() {
		return projectReference.getProjectId();
	}

	@Override
	public String getIdFromURI(URI uri) {
		// TODO: is this really necessary?
		if (uri.getScheme().toLowerCase().equals("file")) {
			File file = new File(uri);
			return file.getName();
		}
		else {
			return new IDGenerator().generate();
		}
	}

	/**
	 * @deprecated still necessary for Source Document Wizard 
	 */
	@Override
	@Deprecated
	public String getFileURL(String sourceDocumentID, String... path) {
		StringBuilder builder = new StringBuilder("file://");
		for (String folder : path) {
			builder.append(folder);
		}
		builder.append(sourceDocumentID);
		return builder.toString();
	}
	
	private URI getSourceDocumentURI(String sourceDocumentId) {
		return Paths
		.get(new File(CATMAPropertyKey.GitBasedRepositoryBasePath.getValue()).toURI())
		.resolve(gitProjectHandler.getSourceDocumentSubmodulePath(sourceDocumentId))
		.resolve(sourceDocumentId + "." + UTF8_CONVERSION_FILE_EXTENSION)
		.toUri();
	}

	@Override
	public void insert(SourceDocument sourceDocument) throws IOException {
		insert(sourceDocument, true);
	}
	
	@Override
	public void insert(SourceDocument sourceDocument, boolean deleteTempFile) throws IOException {
		try {
			File sourceTempFile = Paths.get(new File(this.tempDir).toURI()).resolve(sourceDocument.getUuid()).toFile();
	
			String convertedFilename = 
					sourceDocument.getUuid() + "." + UTF8_CONVERSION_FILE_EXTENSION;

			logger.info("start tokenizing sourcedocument");
			
			List<String> unseparableCharacterSequences = 
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
					.getIndexInfoSet().getUnseparableCharacterSequences();
			List<Character> userDefinedSeparatingCharacters = 
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
					.getIndexInfoSet().getUserDefinedSeparatingCharacters();
			Locale locale = 
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo()
					.getIndexInfoSet().getLocale();
			
			TermExtractor termExtractor = 
					new TermExtractor(
							sourceDocument.getContent(), 
							unseparableCharacterSequences, 
							userDefinedSeparatingCharacters, 
							locale);
			
			final Map<String, List<TermInfo>> terms = termExtractor.getTerms();
			
			logger.info("tokenization finished");
			
			try (FileInputStream originalFileInputStream = new FileInputStream(sourceTempFile)) {
				MediaType mediaType = 
					MediaType.parse(sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet().getMimeType());
				String extension = mediaType.getBaseType().getType();
				if (extension == null || extension.isEmpty()) {
					extension = "unknown";
				}
				String sourceDocRevisionHash = gitProjectHandler.createSourceDocument(
					sourceDocument.getUuid(), 
					originalFileInputStream,
					sourceDocument.getUuid() 
						+ ORIG_INFIX 
						+ "." 
						+ extension,
					new ByteArrayInputStream(
						sourceDocument.getContent().getBytes(Charset.forName("UTF-8"))), 
					convertedFilename, 
					terms,
					sourceDocument.getUuid() + "." + TOKENIZED_FILE_EXTENSION,
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo());
	
				sourceDocument.unload();
				StandardContentHandler contentHandler = new StandardContentHandler();
				contentHandler.setSourceDocumentInfo(
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo());
				sourceDocument.setSourceContentHandler(contentHandler);
				sourceDocument.setRevisionHash(sourceDocRevisionHash);
			}
			
			if (deleteTempFile) {
				sourceTempFile.delete();
			}

			String oldRootRevisionHash = this.rootRevisionHash;
			this.rootRevisionHash = gitProjectHandler.getRootRevisionHash();
	
			graphProjectHandler.addSourceDocument(
				oldRootRevisionHash, this.rootRevisionHash,
				sourceDocument,
				getTokenizedSourceDocumentPath(sourceDocument.getUuid()));

	        eventBus.post(new DocumentChangeEvent(sourceDocument, ChangeType.CREATED));
		}
		catch (Exception e) {
			e.printStackTrace();
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, e);
		}
	}

	@Override
	public void update(SourceDocument sourceDocument, ContentInfoSet contentInfoSet) throws Exception {
		String sourceDocumentRevision = gitProjectHandler.updateSourceDocument(sourceDocument);
		sourceDocument.setRevisionHash(sourceDocumentRevision);

		String oldRootRevisionHash = this.rootRevisionHash;

		// project commit
		this.rootRevisionHash = gitProjectHandler.addSourceDocumentSubmoduleToStagedAndCommit(
				sourceDocument.getUuid(),
				String.format(
						"Updated metadata of document \"%s\" with ID %s",
						sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet().getTitle(),
						sourceDocument.getUuid()
				),
				false
		);

		graphProjectHandler.updateSourceDocument(this.rootRevisionHash, sourceDocument, oldRootRevisionHash);

		eventBus.post(new DocumentChangeEvent(sourceDocument, ChangeType.UPDATED));
	}

	@Override
	public Collection<TagsetDefinition> getTagsets() throws Exception {
		return tagManager.getTagLibrary().getTagsetDefinitions();
	}

	@Override
	public Collection<SourceDocument> getSourceDocuments() throws Exception {
		return graphProjectHandler.getDocuments(this.rootRevisionHash);
	}
	
	@Override
	public boolean hasDocument(String documentId) throws Exception {
		return graphProjectHandler.hasDocument(this.rootRevisionHash, documentId);
	}

	@Override
	public SourceDocument getSourceDocument(String sourceDocumentId) throws Exception {
		return documentCache.get(sourceDocumentId);
	}

	@Override
	public void delete(SourceDocument sourceDocument) throws Exception {
		for (AnnotationCollectionReference collectionRef : new HashSet<>(sourceDocument.getUserMarkupCollectionRefs())) {
			delete(collectionRef);
		}
		documentCache.invalidate(sourceDocument.getUuid());
		String oldRootRevisionHash = this.rootRevisionHash;
		gitProjectHandler.removeDocument(sourceDocument);
		this.rootRevisionHash = gitProjectHandler.getRootRevisionHash(); 
		graphProjectHandler.removeDocument(this.rootRevisionHash, sourceDocument, oldRootRevisionHash);	
        eventBus.post(new DocumentChangeEvent(sourceDocument, ChangeType.DELETED));
	}

	@Override
	public SourceDocument getSourceDocument(AnnotationCollectionReference umcRef) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createUserMarkupCollectionWithAssignment(
			String name, SourceDocument sourceDocument, Integer userId, RBACRole role) {
		try {
			String collectionId = idGenerator.generateCollectionId();
			
			String umcRevisionHash = gitProjectHandler.createMarkupCollection(
						collectionId, 
						name, 
						null, //description
						sourceDocument.getUuid(), 
						sourceDocument.getRevisionHash());
			
			String oldRootRevisionHash = this.rootRevisionHash;
			this.rootRevisionHash = gitProjectHandler.getRootRevisionHash();

			graphProjectHandler.addCollection(
				rootRevisionHash, 
				collectionId, name, umcRevisionHash, 
				sourceDocument,
				tagManager.getTagLibrary(),
				oldRootRevisionHash);
			
			if ((userId != null) && !role.equals(RBACRole.OWNER)) {
				assignOnResource(() -> userId, 
						role, collectionId);		
			}
			
			eventBus.post(
				new CollectionChangeEvent(
					sourceDocument.getUserMarkupCollectionReference(collectionId), 
					sourceDocument, 
					ChangeType.CREATED));
		}
		catch (Exception e) {
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, e);
		}
		
	}

	@Override
	public void createUserMarkupCollection(String name, SourceDocument sourceDocument) {
		createUserMarkupCollectionWithAssignment(
				name, 
				sourceDocument, 
				null, // no assignment
				null // no assignment
		);
	}

	@Override
	public AnnotationCollection getUserMarkupCollection(AnnotationCollectionReference userMarkupCollectionReference)
			throws IOException {
		try {
			return graphProjectHandler.getCollection(rootRevisionHash, userMarkupCollectionReference);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void update(AnnotationCollection userMarkupCollection, List<TagReference> tagReferences) {
		try {
			if (userMarkupCollection.getTagReferences().containsAll(
					tagReferences)) {
				gitProjectHandler.addOrUpdate(
						userMarkupCollection.getUuid(), tagReferences, tagManager.getTagLibrary());
				//TODO: check update
				graphProjectHandler.addTagReferences(
						GraphWorktreeProject.this.rootRevisionHash, userMarkupCollection, tagReferences);
				propertyChangeSupport.firePropertyChange(
						RepositoryChangeEvent.tagReferencesChanged.name(), 
						null, new Pair<>(userMarkupCollection, tagReferences));
			}
			else {
				graphProjectHandler.removeTagReferences(
					GraphWorktreeProject.this.rootRevisionHash, userMarkupCollection, tagReferences);

				Collection<String> tagInstanceIds = tagReferences
						.stream()
						.map(tr -> tr.getTagInstanceId())
						.collect(Collectors.toSet());
				
				for (String tagInstanceId : tagInstanceIds) {
					gitProjectHandler.removeTagInstance(
							userMarkupCollection.getUuid(), tagInstanceId);
				}
				propertyChangeSupport.firePropertyChange(
						RepositoryChangeEvent.tagReferencesChanged.name(), 
						new Pair<>(userMarkupCollection.getUuid(), tagInstanceIds), null);
			}
		}
		catch (Exception e) {
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					e);				
		}
	}

	@Override
	public void update(
			AnnotationCollection collection, 
			TagInstance tagInstance, Collection<Property> properties) throws IOException {
		try {
			for (Property property : properties) {
				tagInstance.addUserDefinedProperty(property);
			}
			gitProjectHandler.addOrUpdate(
					collection.getUuid(), 
					collection.getTagReferences(tagInstance), 
					tagManager.getTagLibrary());
			graphProjectHandler.updateProperties(
					GraphWorktreeProject.this.rootRevisionHash, collection, tagInstance, properties);
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.propertyValueChanged.name(),
					tagInstance, properties);
		}
		catch (Exception e) {
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					e);				
		}

	}

	@Override
	public void update(
			AnnotationCollectionReference collectionReference, 
			ContentInfoSet contentInfoSet) throws Exception {
		String collectionRevision = 
			gitProjectHandler.updateCollection(collectionReference);
		collectionReference.setRevisionHash(collectionRevision);
		
		String oldRootRevisionHash = this.rootRevisionHash;
		
		// project commit
		this.rootRevisionHash = gitProjectHandler.addCollectionSubmoduleToStagedAndCommit(
			collectionReference.getId(), 
			String.format("Updated metadata of Collection %1$s with ID %2$s", 
					collectionReference.getName(), collectionReference.getId()),
			false);
		
		graphProjectHandler.updateCollection(
			this.rootRevisionHash, collectionReference, oldRootRevisionHash);		
		
		SourceDocument document = getSourceDocument(collectionReference.getSourceDocumentId());
		eventBus.post(
			new CollectionChangeEvent(
				collectionReference, document, ChangeType.UPDATED));
	}

	@Override
	public void delete(AnnotationCollectionReference collectionReference) throws Exception {
		String oldRootRevisionHash = this.rootRevisionHash;
		SourceDocument document = getSourceDocument(collectionReference.getSourceDocumentId());
		
		this.rootRevisionHash = gitProjectHandler.removeCollection(collectionReference);
		
		graphProjectHandler.removeCollection(this.rootRevisionHash, collectionReference, oldRootRevisionHash);	
		document.removeUserMarkupCollectionReference(collectionReference);
		
		eventBus.post(
			new CollectionChangeEvent(
				collectionReference, document, ChangeType.DELETED));
	}
	
	public Pair<AnnotationCollection, List<TagsetDefinitionImportStatus>> loadAnnotationCollection(
			InputStream inputStream, SourceDocument document) throws IOException {
		TagManager tagManager = new TagManager(new TagLibrary());
		
		TeiTagLibrarySerializationHandler tagLibrarySerializationHandler = 
				new TeiTagLibrarySerializationHandler(tagManager, this.rootRevisionHash);

		TagLibrary importedLibrary =
			tagLibrarySerializationHandler.deserialize(null, inputStream);
		
		List<String> resourceIds = gitProjectHandler.getResourceIds();
		List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList = new ArrayList<>();
		for (TagsetDefinition tagset : importedLibrary) {
			boolean inProjectHistory = resourceIds.contains(tagset.getUuid());
			boolean current = 
				inProjectHistory && (getTagManager().getTagLibrary().getTagsetDefinition(tagset.getUuid()) != null);
			tagsetDefinitionImportStatusList.add(
				new TagsetDefinitionImportStatus(tagset, inProjectHistory, current));
		}
		
		String collectionId = idGenerator.generate();
		
		TeiUserMarkupCollectionDeserializer deserializer = 
				new TeiUserMarkupCollectionDeserializer(
						tagLibrarySerializationHandler.getTeiDocument(), tagManager.getTagLibrary(),
						collectionId);
		
		AnnotationCollection annotationCollection = new AnnotationCollection(
				collectionId, tagLibrarySerializationHandler.getTeiDocument().getContentInfoSet(),
				tagManager.getTagLibrary(), deserializer.getTagReferences(),
				document.getUuid(),
				document.getRevisionHash());
		
		return new Pair<>(annotationCollection, tagsetDefinitionImportStatusList);
	}
	
	public void importCollection(
		List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList, 
		AnnotationCollection importAnnotationCollection) throws IOException {
	
		// if updates to existing Tagsets are needed, update only the Tags
		// that are actually referenced in the Collection
		Set<String> tagDefinitionIds = 
			importAnnotationCollection.getTagReferences().stream().map(
					tagRef -> tagRef.getTagDefinitionId()).collect(Collectors.toSet());
		
	
		for (TagsetDefinitionImportStatus tagsetDefinitionImportStatus : tagsetDefinitionImportStatusList) {
			tagsetDefinitionImportStatus.setUpdateFilter(tagDefinitionIds);
		}
		
		importTagsets(tagsetDefinitionImportStatusList);
		
		importAnnotationCollection.setTagLibrary(tagManager.getTagLibrary());
		
		try {
			SourceDocument sourceDocument = 
					getSourceDocument(importAnnotationCollection.getSourceDocumentId());
			String umcRevisionHash = gitProjectHandler.createMarkupCollection(
					importAnnotationCollection.getId(), 
					importAnnotationCollection.getName(), 
					importAnnotationCollection.getContentInfoSet().getDescription(), //description
					importAnnotationCollection.getSourceDocumentId(), 
					importAnnotationCollection.getSourceDocumentRevisionHash());
		
			String oldRootRevisionHash = this.rootRevisionHash;
			this.rootRevisionHash = gitProjectHandler.getRootRevisionHash();
	
			
			graphProjectHandler.addCollection(
				rootRevisionHash, 
				importAnnotationCollection.getId(), 
				importAnnotationCollection.getName(), umcRevisionHash, 
				sourceDocument,
				tagManager.getTagLibrary(),
				oldRootRevisionHash);
		
			AnnotationCollectionReference annotationCollectionReference =
					sourceDocument.getUserMarkupCollectionReference(importAnnotationCollection.getId());
			eventBus.post(
				new CollectionChangeEvent(
					annotationCollectionReference, 
					sourceDocument, 
					ChangeType.CREATED));		
			
			AnnotationCollection createdAnnotationCollection = getUserMarkupCollection(annotationCollectionReference);
			createdAnnotationCollection.addTagReferences(importAnnotationCollection.getTagReferences());
			ArrayListMultimap<String, TagReference> tagReferencesByTagInstanceId = ArrayListMultimap.create();
			
			importAnnotationCollection.getTagReferences().stream().forEach(
				tagReference -> tagReferencesByTagInstanceId.put(tagReference.getTagInstanceId(), tagReference));
			for (String tagInstanceId : tagReferencesByTagInstanceId.keySet()) {
				update(
						createdAnnotationCollection, 
						tagReferencesByTagInstanceId.get(tagInstanceId));
			}
			
			commitChanges(
				String.format(
					"Imported Annotations from Collection %1$s with ID %2$s", 
					createdAnnotationCollection.getName(), 
					createdAnnotationCollection.getId()));
		}
		catch (Exception e) {
			throw new IOException(
				String.format(
					"Import of Collection %1$s failed! The import has been aborted.",
					importAnnotationCollection.getName()), 
				e);		
		}
	}
	
	@Override
	public boolean inProjectHistory(String resourceId) throws IOException {
		return gitProjectHandler.getResourceIds().contains(resourceId);
	}

	@Override
	public List<TagsetDefinitionImportStatus> loadTagLibrary(InputStream inputStream) throws IOException {
		TeiSerializationHandlerFactory factory = new TeiSerializationHandlerFactory(this.rootRevisionHash);
		factory.setTagManager(new TagManager(new TagLibrary()));
		TagLibrarySerializationHandler tagLibrarySerializationHandler = 
				factory.getTagLibrarySerializationHandler();
		TagLibrary importedLibrary =
			tagLibrarySerializationHandler.deserialize(null, inputStream);
		
		List<String> resourceIds = gitProjectHandler.getResourceIds();
		List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList = new ArrayList<>();
		for (TagsetDefinition tagset : importedLibrary) {
			boolean inProjectHistory = resourceIds.contains(tagset.getUuid());
			boolean current = 
				inProjectHistory && (getTagManager().getTagLibrary().getTagsetDefinition(tagset.getUuid()) != null);
			tagsetDefinitionImportStatusList.add(
				new TagsetDefinitionImportStatus(tagset, inProjectHistory, current));
		}
		return tagsetDefinitionImportStatusList;
	}
	
	@Override
	public void importTagsets(List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList) throws IOException {
		for (TagsetDefinitionImportStatus tagsetDefinitionImportStatus : tagsetDefinitionImportStatusList) {
			
			if (tagsetDefinitionImportStatus.isDoImport()) {
				TagsetDefinition tagset = tagsetDefinitionImportStatus.getTagset();
				
				// new Tagset
				if (!tagsetDefinitionImportStatus.isInProjectHistory()) {
					try {
						tagManagerListenersEnabled = false;
						try {
							addTagsetDefinition(tagset);
							tagManager.addTagsetDefinition(tagset);
						} catch (Exception e) {
							throw new IOException(
								String.format(
									"Import of Tagset %1$s failed! The import has been aborted.",
									tagset.getName()), 
								e);
						}
					}
					finally {
						tagManagerListenersEnabled = true;
					}
					for (TagDefinition tag : tagset.getRootTagDefinitions()) {
						tagManager.addTagDefinition(tagset, tag);
						
						importTagHierarchy(tag, tagset, tagset);
					}					
				}
				// removed, but exists in version history
				else if (!tagsetDefinitionImportStatus.isCurrent()) {
					
					String oldRootRevisionHash = this.rootRevisionHash;
					
					Pair<TagsetDefinition, String> result = 
							gitProjectHandler.cloneAndAddTagset(
								tagset.getUuid(), 
								tagset.getName(),
								String.format(
										"Re-Added Tagset %1$s with ID %2$s", 
										tagset.getName(), tagset.getUuid()));
					
					TagsetDefinition oldTagset = result.getFirst();
					this.rootRevisionHash = result.getSecond();
					
					// remove old Tags
					for (TagDefinition tagDefinition : oldTagset.getRootTagDefinitions()) {
						gitProjectHandler.removeTag(tagDefinition);
						oldTagset.remove(tagDefinition);
					}
					
					try {
						// add empty Tagset
						graphProjectHandler.addTagset(
								this.rootRevisionHash, oldTagset, oldRootRevisionHash);
					
						
						try {
							tagManagerListenersEnabled = false;
							tagManager.addTagsetDefinition(tagset);
						}
						finally {
							tagManagerListenersEnabled = true;
						}						
						
						// add imported Tags
						for (TagDefinition tag : tagset.getRootTagDefinitions()) {
							tagManager.addTagDefinition(oldTagset, tag);
							
							importTagHierarchy(tag, tagset, oldTagset);
						}						
						
						// update meta data
						oldTagset.setName(tagset.getName());
						
						updateTagsetDefinition(oldTagset);
					} catch (Exception e) {
						throw new IOException(
								String.format(
									"Import of Tagset %1$s failed! The import has been aborted.",
									tagset.getName()), 
								e);
					}
				}
				// exists already in project
				else {
					try {
						TagsetDefinition existingTagset = 
							getTagManager().getTagLibrary().getTagsetDefinition(tagset.getUuid());
						
						for (TagDefinition incomingTag : tagset) {
							if (existingTagset.hasTagDefinition(incomingTag.getUuid())) {
								if (tagsetDefinitionImportStatus.passesUpdateFilter(incomingTag.getUuid())) {
									TagDefinition existingTag = existingTagset.getTagDefinition(incomingTag.getUuid());
									for (PropertyDefinition incomingPropertyDef : incomingTag.getUserDefinedPropertyDefinitions()) {
										PropertyDefinition existingPropertyDef = 
												existingTag.getPropertyDefinitionByUuid(incomingPropertyDef.getUuid());
										if (existingPropertyDef != null) {
											for (String value : incomingPropertyDef.getPossibleValueList()) {
												if (!existingPropertyDef.getPossibleValueList().contains(value)) {
													existingPropertyDef.addValue(value);
												}
											}
											existingPropertyDef.setName(incomingPropertyDef.getName());
											
											updatePropertyDefinition(existingPropertyDef, existingTag);
										}
										else {
											existingTag.addUserDefinedPropertyDefinition(incomingPropertyDef);
										}
										
										existingTag.setName(incomingTag.getName());
										existingTag.setColor(incomingTag.getColor());
										updateTagDefinition(existingTag, existingTagset);
									}
		
								}
							}
							else {
								getTagManager().addTagDefinition(existingTagset, incomingTag);
							}
						}
						
						existingTagset.setName(tagset.getName());
						updateTagsetDefinition(existingTagset);
					}
					catch (Exception e) {
						throw new IOException(
								String.format(
									"Import of Tagset %1$s failed! The import has been aborted.",
									tagset.getName()), 
								e);
					}
				}
			}
		}
	}

	private void importTagHierarchy(TagDefinition tag, TagsetDefinition tagset, TagsetDefinition targetTagset) {
		for (TagDefinition childTag : tagset.getDirectChildren(tag)) {
			tagManager.addTagDefinition(targetTagset, childTag);
			importTagHierarchy(childTag, tagset, targetTagset);
		}
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public TagManager getTagManager() {
		return tagManager;
	}

	@Override
	public File getFile(SourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Member> getProjectMembers() throws IOException {
		return gitProjectHandler.getProjectMembers();
	}
	
	@Override
	public boolean hasUncommittedChanges() throws Exception {
		return gitProjectHandler.hasUncommittedChanges();
	}
	
	@Override
	public List<CommitInfo> getUnsynchronizedCommits() throws Exception {
		return gitProjectHandler.getUnsynchronizedChanges();
	}

	@Override
	public void commitChanges(String commitMsg) throws Exception {
		commitAllChanges(collectionRef -> commitMsg, commitMsg);
	}

	private void commitAllChanges(
		Function<AnnotationCollectionReference, String> collectionCcommitMsgProvider, 
		String projectCommitMsg) throws Exception {
		List<AnnotationCollectionReference> collectionRefs = 
				getSourceDocuments().stream()
				.flatMap(doc -> doc.getUserMarkupCollectionRefs().stream())
				.collect(Collectors.toList());
		
		for (AnnotationCollectionReference collectionRef : collectionRefs) {
			
			gitProjectHandler.addCollectionToStagedAndCommit(
				collectionRef.getId(), 
				collectionCcommitMsgProvider.apply(collectionRef),
				false);
		}
		
		gitProjectHandler.commitProject(projectCommitMsg);
		
		printStatus();
	}
	
	@Override
	public void synchronizeWithRemote(OpenProjectListener openProjectListener) throws Exception {
		if (hasUncommittedChanges()) {
			throw new IllegalStateException("There are uncommitted changes that need to be committed first!");
		}

		for (TagsetDefinition tagset : getTagsets()) {
			gitProjectHandler.synchronizeTagsetWithRemote(tagset.getUuid());
		}

		for (SourceDocument document : getSourceDocuments()) {
			gitProjectHandler.synchronizeSourceDocumentWithRemote(document.getUuid());

			for (AnnotationCollectionReference collectionReference : document.getUserMarkupCollectionRefs()) {
				gitProjectHandler.synchronizeCollectionWithRemote(collectionReference.getId());
			}
		}

		gitProjectHandler.synchronizeWithRemote();

		if (gitProjectHandler.hasConflicts()) {
			gitProjectHandler.initAndUpdateSubmodules();
			openProjectListener.conflictResolutionNeeded(new GitConflictedProject(
					projectReference,
					gitProjectHandler,
					documentId -> getSourceDocumentURI(documentId)
			));
		}
		else {
			gitProjectHandler.initAndUpdateSubmodules();
			gitProjectHandler.removeStaleSubmoduleDirectories();
			gitProjectHandler.ensureDevBranches();
			rootRevisionHash = gitProjectHandler.getRootRevisionHash();
			ProgressListener progressListener = new ProgressListener() {
				@Override
				public void setProgress(String value, Object... args) {
					openProjectListener.progress(value, args);
				}
			};

			graphProjectHandler.ensureProjectRevisionIsLoaded(
					new ExecutionListener<TagManager>() {
						@Override
						public void error(Throwable t) {
							openProjectListener.failure(t);
						}

						@Override
						public void done(TagManager result) {
							tagManager.load(result.getTagLibrary());
							openProjectListener.ready(GraphWorktreeProject.this);
						}
					},
					progressListener,
					rootRevisionHash,
					tagManager,
					() -> gitProjectHandler.getTagsets(),
					() -> gitProjectHandler.getDocuments(),
					(tagLibrary) -> gitProjectHandler.getCollections(tagLibrary, progressListener),
					false,
					backgroundService
			);
		}
	}

	@Override
	public RBACRole getRoleForDocument(String documentId) {
		return gitProjectHandler.getRoleForDocument(documentId);
	}
	
	@Override
	public RBACRole getRoleForCollection(String collectionId) {
		return gitProjectHandler.getRoleForCollection(collectionId);
	}
	
	@Override
	public RBACRole getRoleForTagset(String tagsetId) {	
		return gitProjectHandler.getRoleForTagset(tagsetId);
	}

	@Override
	public boolean hasPermission(RBACRole role, RBACPermission permission) {
		return gitProjectHandler.hasPermission(role, permission);
	}

	@Override
	public boolean isAuthorizedOnProject(RBACPermission permission) {
		return gitProjectHandler.isAuthorizedOnProject(permission);
	}

	@Override
	public RBACSubject assignOnProject(RBACSubject subject, RBACRole role) throws IOException {
		return gitProjectHandler.assignOnProject(subject, role);
	}

	@Override
	public void unassignFromProject(RBACSubject subject) throws IOException {
		gitProjectHandler.unassignFromProject(subject);
	}

	@Override
	public RBACSubject assignOnResource(RBACSubject subject, RBACRole role, String resourceId) throws IOException {
		return gitProjectHandler.assignOnResource(subject, role, resourceId);
	}

	@Override
	public void unassignFromResource(RBACSubject subject, String resourceId) throws IOException {
		gitProjectHandler.unassignFromResource(subject, resourceId);
	}

	@Override
	public List<User> findUser(String usernameOrEmail, int offset, int limit) throws IOException {
		return gitProjectHandler.findUser(usernameOrEmail, offset, limit);
	}

	@Override
	public Set<Member> getResourceMembers(String resourceId) throws IOException {
		return gitProjectHandler.getResourceMembers(resourceId);
	}

	@Override
	public RBACRole getRoleOnProject() throws IOException {
		return gitProjectHandler.getRoleOnProject(user);
	}
	
	@Override
	public void addComment(Comment comment) throws IOException {
		gitProjectHandler.addComment(comment);
		eventBus.post(new CommentChangeEvent(ChangeType.CREATED, comment));
	}
	
	@Override
	public void removeComment(Comment comment) throws IOException {
		gitProjectHandler.removeComment(comment);
		eventBus.post(new CommentChangeEvent(ChangeType.DELETED, comment));
	}
	
	@Override
	public void updateComment(Comment comment) throws IOException {
		gitProjectHandler.updateComment(comment);
		eventBus.post(new CommentChangeEvent(ChangeType.UPDATED, comment));
	}
	
	@Override
	public List<Comment> getComments(String documentId) throws IOException {
		return gitProjectHandler.getComments(documentId);
	}
	
	@Override
	public void addReply(Comment comment, Reply reply) throws IOException {
		gitProjectHandler.addReply(comment, reply);
		
		eventBus.post(new ReplyChangeEvent(ChangeType.CREATED, comment, reply));
	}
	
	@Override
	public List<Reply> getCommentReplies(Comment comment) throws IOException {
		return gitProjectHandler.getCommentReplies(comment);
	}

	@Override
	public void updateReply(Comment comment, Reply reply) throws IOException {
		gitProjectHandler.updateReply(comment, reply);
		
		eventBus.post(new ReplyChangeEvent(ChangeType.UPDATED, comment, reply));
	}

	@Override
	public void removeReply(Comment comment, Reply reply) throws IOException {
		gitProjectHandler.removeReply(comment, reply);
		
		eventBus.post(new ReplyChangeEvent(ChangeType.DELETED, comment, reply));

	}
	
	@Override
	public String getVersion() {
		return rootRevisionHash;
	}
	
}
