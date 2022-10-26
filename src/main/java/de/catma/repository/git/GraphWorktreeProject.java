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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.eventbus.EventBus;

import com.vaadin.ui.UI;

import org.apache.tika.mime.MediaType;
import org.eclipse.jgit.api.Status;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressListener;
import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
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
import de.catma.repository.git.graph.interfaces.*;
import de.catma.repository.git.graph.lazy.LazyGraphProjectHandler;
import de.catma.repository.git.resource.provider.LatestContribution;
import de.catma.repository.git.resource.provider.LatestContributionsResourceProvider;
import de.catma.repository.git.resource.provider.SynchronizedResourceProvider;
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

	private final Logger logger = Logger.getLogger(GraphWorktreeProject.class.getName());

	private final User user;
	private final GitProjectHandler gitProjectHandler;
	private final ProjectReference projectReference;
	private final TagManager tagManager;
	private final BackgroundService backgroundService;

	private final GraphProjectHandler graphProjectHandler;
	private final Indexer indexer;

	private final String tempDir;
	private final IDGenerator idGenerator;

	private EventBus eventBus;
	private PropertyChangeSupport propertyChangeSupport;

	private boolean tagManagerListenersEnabled = true;
	private String rootRevisionHash;


	public GraphWorktreeProject(
			User user,
			GitProjectHandler gitProjectHandler,
			ProjectReference projectReference,
			TagManager tagManager,
			BackgroundService backgroundService,
			EventBus eventBus
	) {
		this.user = user;
		this.gitProjectHandler = gitProjectHandler;
		this.projectReference = projectReference;
		this.tagManager = tagManager;
		this.backgroundService = backgroundService;
		this.eventBus = eventBus;

		this.graphProjectHandler = new LazyGraphProjectHandler(
				this.projectReference,
				this.user,
				new DocumentFileURIProvider() {
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
				},
				new DocumentSupplier() {
					@Override
					public SourceDocument get(String documentId) throws IOException {
						return gitProjectHandler.getDocument(documentId);
					}
				},
				new DocumentIndexSupplier() {
					@Override
					public Map get(String documentId) throws IOException {
						Path tokensPath = GraphWorktreeProject.this.getTokenizedSourceDocumentPath(documentId);
						return gitProjectHandler.getDocumentIndex(documentId, tokensPath);
					}
				},
				new CollectionSupplier() {
					@Override
					public AnnotationCollection get(String collectionId, TagLibrary tagLibrary) throws IOException {
						return gitProjectHandler.getCollection(collectionId, tagLibrary);
					}
				}
		);

		this.indexer = this.graphProjectHandler.createIndexer();

		this.tempDir = CATMAPropertyKey.TEMP_DIR.getValue();
		this.idGenerator = new IDGenerator();

		this.propertyChangeSupport = new PropertyChangeSupport(this);
	}

	private Path getTokenizedSourceDocumentPath(String documentId) {
		return Paths.get(new File(CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue()).toURI())
				.resolve(gitProjectHandler.getSourceDocumentPath(documentId))
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
				openProjectListener.failure(new IllegalStateException(
						String.format(
							"There are conflicts in Project %1$s with ID %2$s,"
							+ "please contact our support.",
							projectReference.getName(),
							projectReference.getProjectId())));						
			}
			else {
				gitProjectHandler.ensureUserBranch();
				
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
						(tagLibrary) -> gitProjectHandler.getCollections(tagLibrary, progressListener, true),
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
			StringBuilder builder = new StringBuilder();
			StatusPrinter.print(projectReference.toString(), status, builder);
			logger.info(builder.toString());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initTagManagerListeners() {
		PropertyChangeListener tagsetDefinitionChangedListener = new PropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent evt) {
				if (!tagManagerListenersEnabled) {
					return;
				}

				try {
					if (evt.getOldValue() == null) { // TagsetDefinition was added
						final TagsetDefinition tagsetDefinition = (TagsetDefinition) evt.getNewValue();
						addTagsetDefinition(tagsetDefinition);
					}
					else if (evt.getNewValue() == null) { // TagsetDefinition was deleted
						final TagsetDefinition tagsetDefinition = (TagsetDefinition) evt.getOldValue();
						removeTagsetDefinition(tagsetDefinition);
					}
					else { // TagsetDefinition was updated
						final TagsetDefinition tagsetDefinition = (TagsetDefinition) evt.getNewValue();
						updateTagsetDefinition(tagsetDefinition);
					}
				}
				catch (Exception e) {
					propertyChangeSupport.firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null,
							e
					);
				}
			}
		};
		tagManager.addPropertyChangeListener(TagManagerEvent.tagsetDefinitionChanged, tagsetDefinitionChangedListener);

		PropertyChangeListener tagDefinitionChangedListener = new PropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent evt) {
				if (!tagManagerListenersEnabled) {
					return;
				}

				try {
					if (evt.getOldValue() == null) { // TagDefinition was added
						@SuppressWarnings("unchecked")
						final Pair<TagsetDefinition, TagDefinition> args = (Pair<TagsetDefinition, TagDefinition>) evt.getNewValue();
						addTagDefinition(args.getSecond(), args.getFirst());
					}
					else if (evt.getNewValue() == null) { // TagDefinition was deleted
						@SuppressWarnings("unchecked")
						final Pair<TagsetDefinition, TagDefinition> args = (Pair<TagsetDefinition, TagDefinition>) evt.getOldValue();
						removeTagDefinition(args.getSecond(), args.getFirst());
					}
					else { // TagDefinition was updated
						final TagDefinition tag = (TagDefinition) evt.getNewValue();
						final TagsetDefinition tagset = (TagsetDefinition) evt.getOldValue();
						updateTagDefinition(tag, tagset);
					}
				}
				catch (Exception e) {
					propertyChangeSupport.firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null,
							e
					);
				}
			}
		};
		tagManager.addPropertyChangeListener(TagManagerEvent.tagDefinitionChanged, tagDefinitionChangedListener);

		PropertyChangeListener userDefinedPropertyChangedListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (!tagManagerListenersEnabled) {
					return;
				}

				try {
					if (evt.getOldValue() == null) { // PropertyDefinition was added
						@SuppressWarnings("unchecked")
						Pair<PropertyDefinition, TagDefinition> args = (Pair<PropertyDefinition, TagDefinition>) evt.getNewValue();
						addPropertyDefinition(args.getFirst(), args.getSecond());
					}
					else if (evt.getNewValue() == null) { // PropertyDefinition was deleted
						@SuppressWarnings("unchecked")
						Pair<PropertyDefinition, Pair<TagDefinition, TagsetDefinition>> args =
								(Pair<PropertyDefinition, Pair<TagDefinition, TagsetDefinition>>) evt.getOldValue();
						removePropertyDefinition(args.getFirst(), args.getSecond().getFirst(), args.getSecond().getSecond());
					}
					else { // PropertyDefinition was updated
						PropertyDefinition propertyDefinition = (PropertyDefinition) evt.getNewValue();
						TagDefinition tagDefinition = (TagDefinition) evt.getOldValue();
						updatePropertyDefinition(propertyDefinition, tagDefinition);
					}
				}
				catch (Exception e) {
					propertyChangeSupport.firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null,
							e
					);
				}
			}
		};
		tagManager.addPropertyChangeListener(TagManagerEvent.userPropertyDefinitionChanged, userDefinedPropertyChangedListener);
	}

	private void updateTagsetDefinition(TagsetDefinition tagsetDefinition) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot update %2$s", 
						this.projectReference, tagsetDefinition));
		}
		String oldRootRevisionHash = this.rootRevisionHash;

		this.rootRevisionHash = gitProjectHandler.updateTagset(tagsetDefinition);
		
		graphProjectHandler.updateTagset(this.rootRevisionHash, tagsetDefinition, oldRootRevisionHash);
	}

	private void removeTagsetDefinition(TagsetDefinition tagsetDefinition) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format(
						"Project \"%s\" is in read-only mode! Cannot remove tagset \"%s\".",
						projectReference,
						tagsetDefinition
				)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		// collect annotations
		Multimap<String, TagReference> tagReferencesByCollectionId = graphProjectHandler.getTagReferencesByCollectionId(tagsetDefinition);
		Multimap<String, TagInstance> tagInstancesByCollectionId = Multimaps.transformValues(
				tagReferencesByCollectionId, TagReference::getTagInstance
		);
		Multimap<String, String> annotationIdsByCollectionId = Multimaps.transformValues(
				tagReferencesByCollectionId, TagReference::getTagInstanceId
		);

		// remove tagset and affected annotations from repo and commit
		rootRevisionHash = gitProjectHandler.removeTagset(tagsetDefinition, tagInstancesByCollectionId);

		// save updates to index
		for (String collectionId : annotationIdsByCollectionId.keySet()) {
			graphProjectHandler.removeTagInstances(rootRevisionHash, collectionId, annotationIdsByCollectionId.get(collectionId));
		}

		graphProjectHandler.removeTagset(rootRevisionHash, tagsetDefinition, oldRootRevisionHash);
	}

	private void addPropertyDefinition(
			PropertyDefinition propertyDefinition, TagDefinition tagDefinition) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot update %2$s", 
						this.projectReference, tagDefinition));
		}

		String oldRootRevisionHash = this.rootRevisionHash;

		TagsetDefinition tagsetDefinition = 
			tagManager.getTagLibrary().getTagsetDefinition(tagDefinition);
		
		this.rootRevisionHash = gitProjectHandler.createOrUpdateTag(
			tagsetDefinition.getUuid(), 
			tagDefinition,
			String.format(
				"Added Property Definition %1$s with ID %2$s "
				+ "to Tag %3$s with ID %4$s "
				+ "in Tagset %5$s with ID %6$s",
				propertyDefinition.getName(),
				propertyDefinition.getUuid(),
				tagDefinition.getName(),
				tagDefinition.getUuid(),
				tagsetDefinition.getName(),
				tagsetDefinition.getUuid()));
		
		graphProjectHandler.addPropertyDefinition(
			rootRevisionHash, propertyDefinition, tagDefinition, tagsetDefinition, 
			oldRootRevisionHash);
	}

	private void removePropertyDefinition(
			PropertyDefinition propertyDefinition,
			TagDefinition tagDefinition,
			TagsetDefinition tagsetDefinition
	) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format(
						"Project \"%s\" is in read-only mode! Cannot update tag \"%s\".",
						projectReference,
						tagDefinition
				)
			);
		}

		// collect annotations
		Multimap<String, TagReference> tagReferencesByCollectionId = graphProjectHandler.getTagReferencesByCollectionId(tagDefinition);

		gitProjectHandler.addCollectionsToStagedAndCommit(
				tagReferencesByCollectionId.keySet(),
				String.format(
						"Auto-committing changes before performing an update of annotations "
								+ "as part of the deletion of the property \"%s\" with ID: %s",
						propertyDefinition.getName(),
						propertyDefinition.getUuid()
				),
				false, // don't force
				false // don't push now, we push everything when the removal happens below
		);

		// delete properties from affected annotations
		for (String collectionId : tagReferencesByCollectionId.keySet()) {
			Collection<TagReference> tagReferences = tagReferencesByCollectionId.get(collectionId);
			Set<TagInstance> tagInstances = tagReferences.stream()
					.map(TagReference::getTagInstance)
					.collect(Collectors.toSet());

			tagInstances.forEach(
				tagInstance -> tagInstance.removeUserDefinedProperty(propertyDefinition.getUuid())
			);

			// save updates to repo
			for (TagInstance tagInstance : tagInstances) {
				gitProjectHandler.addOrUpdate(
						collectionId,
						tagReferences.stream()
								.filter(tagRef -> tagRef.getTagInstanceId().equals(tagInstance.getUuid()))
								.collect(Collectors.toList()),
						tagManager.getTagLibrary()
				);
			}

			// save updates to index
			graphProjectHandler.removeProperties(rootRevisionHash, collectionId, propertyDefinition.getUuid());
		}

		String oldRootRevisionHash = rootRevisionHash;

		// remove property from repo and commit
		rootRevisionHash = gitProjectHandler.removePropertyDefinition(
				propertyDefinition,
				tagDefinition,
				tagsetDefinition,
				tagReferencesByCollectionId.keySet()
		);

		// save updates to index
		graphProjectHandler.removePropertyDefinition(
				rootRevisionHash,
				propertyDefinition,
				tagDefinition,
				tagsetDefinition,
				oldRootRevisionHash
		);
	}

	private void updatePropertyDefinition(PropertyDefinition propertyDefinition, TagDefinition tagDefinition) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot update %2$s", 
						this.projectReference, tagDefinition));
		}

		String oldRootRevisionHash = this.rootRevisionHash;

		TagsetDefinition tagsetDefinition = 
			tagManager.getTagLibrary().getTagsetDefinition(tagDefinition);
		
		this.rootRevisionHash = gitProjectHandler.createOrUpdateTag(
				tagsetDefinition.getUuid(), tagDefinition,
				String.format(
					"Updated Property Definition %1$s with ID %2$s "
					+ "in Tag %3$s with ID %4$s in Tagset %5$s with ID %6$s",
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
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot update %2$s", 
						this.projectReference, tagsetDefinition));
		}

		String oldRootRevisionHash = this.rootRevisionHash;

		this.rootRevisionHash = 
			gitProjectHandler.createOrUpdateTag(
				tagsetDefinition.getUuid(), tagDefinition,
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
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot update %2$s", 
						this.projectReference, tagsetDefinition));
		}

		String oldRootRevisionHash = this.rootRevisionHash;

		this.rootRevisionHash = gitProjectHandler.createOrUpdateTag(
			tagsetDefinition.getUuid(), 
			tagDefinition,
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
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format(
						"Project \"%s\" is in read-only mode! Cannot update tagset \"%s\".",
						projectReference,
						tagsetDefinition
				)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;

		// collect annotations
		Multimap<String, TagReference> tagReferencesByCollectionId =
				graphProjectHandler.getTagReferencesByCollectionId(tagDefinition);
		Multimap<String, TagInstance> tagInstancesByCollectionId = Multimaps.transformValues(
				tagReferencesByCollectionId, TagReference::getTagInstance
		);
		Multimap<String, String> annotationIdsByCollectionId = Multimaps.transformValues(
				tagReferencesByCollectionId, TagReference::getTagInstanceId
		);

		// remove tag and annotations from repo and commit
		rootRevisionHash = gitProjectHandler.removeTagAndAnnotations(tagDefinition, tagInstancesByCollectionId);

		// save updates to index
		for (String collectionId : annotationIdsByCollectionId.keySet()) {
			graphProjectHandler.removeTagInstances(rootRevisionHash, collectionId, annotationIdsByCollectionId.get(collectionId));
		}

		graphProjectHandler.removeTagDefinition(rootRevisionHash, tagDefinition, tagsetDefinition, oldRootRevisionHash);

		// fire annotation change events for each collection
		for (String collectionId : annotationIdsByCollectionId.keySet()) {
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.tagReferencesChanged.name(), 
					new Pair<>(collectionId, annotationIdsByCollectionId.get(collectionId)),
					null
			);
		}
	}

	private void addTagsetDefinition(TagsetDefinition tagsetDefinition) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot add %2$s", 
						this.projectReference, tagsetDefinition));
		}

		String oldRootRevisionHash = this.rootRevisionHash;
		this.rootRevisionHash = gitProjectHandler.createTagset(
				tagsetDefinition.getUuid(), 
				tagsetDefinition.getName(), 
				tagsetDefinition.getDescription(),
				tagsetDefinition.getForkedFromCommitURL()
		);
		
		tagsetDefinition.setResponsibleUser(this.user.getIdentifier());

		graphProjectHandler.addTagset(
				rootRevisionHash, tagsetDefinition, oldRootRevisionHash
		);
	}

	@Override
	public void close() {
		try {
			if (!gitProjectHandler.hasConflicts()) {
				commitAndPushChanges(
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

	private URI getSourceDocumentURI(String sourceDocumentId) {
		return Paths.get(new File(CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue()).toURI())
				.resolve(gitProjectHandler.getSourceDocumentPath(sourceDocumentId))
				.resolve(sourceDocumentId + "." + UTF8_CONVERSION_FILE_EXTENSION)
				.toUri();
	}

	@Override
	public void insert(SourceDocument sourceDocument) throws IOException {
		insert(sourceDocument, true);
	}
	
	@Override
	public void insert(SourceDocument sourceDocument, boolean deleteTempFile) throws IOException {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot add %2$s", 
						this.projectReference, sourceDocument));
		}

		try {
			File sourceTempFile = 
					Paths.get(new File(this.tempDir).toURI())
					.resolve(sourceDocument.getUuid())
					.toFile();
	
			String convertedFilename = 
					sourceDocument.getUuid() + "." + UTF8_CONVERSION_FILE_EXTENSION;

			logger.info(
				String.format("Start tokenizing Document %1$s with ID %2$s", 
						sourceDocument.toString(), sourceDocument.getUuid()));
			
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
			
			logger.info(
					String.format("Tokenizing finished: Document %1$s with ID %2$s", 
							sourceDocument.toString(), sourceDocument.getUuid()));
			
			String oldRootRevisionHash = this.rootRevisionHash;
			
			try (FileInputStream originalFileInputStream = new FileInputStream(sourceTempFile)) {
				MediaType mediaType = 
					MediaType.parse(sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getTechInfoSet().getMimeType());
				String extension = mediaType.getBaseType().getType();
				if (extension == null || extension.isEmpty()) {
					extension = "unknown";
				}
				this.rootRevisionHash = gitProjectHandler.createSourceDocument(
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

				graphProjectHandler.addSourceDocument(
						oldRootRevisionHash, this.rootRevisionHash,
						sourceDocument,
						getTokenizedSourceDocumentPath(sourceDocument.getUuid()));
			}
			
			if (deleteTempFile) {
				sourceTempFile.delete();
			}

	        eventBus.post(new DocumentChangeEvent(
	        	new SourceDocumentReference(
	        		sourceDocument.getUuid(), sourceDocument.getSourceContentHandler()), 
	        	ChangeType.CREATED));
		}
		catch (Exception e) {
			e.printStackTrace();
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, e);
		}
	}

	@Override
	public void update(SourceDocumentReference sourceDocumentRef, ContentInfoSet contentInfoSet, String responsibleUser) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format(
						"Project \"%s\" is in read-only mode! Cannot update document \"%s\".",
						projectReference,
						sourceDocumentRef
				)
			);
		}

		String oldRootRevisionHash = rootRevisionHash;
		sourceDocumentRef.getSourceDocumentInfo().getTechInfoSet().setResponsibleUser(responsibleUser);
		rootRevisionHash = gitProjectHandler.updateSourceDocument(sourceDocumentRef);

		graphProjectHandler.updateSourceDocument(rootRevisionHash, sourceDocumentRef, oldRootRevisionHash);

		eventBus.post(new DocumentChangeEvent(sourceDocumentRef, ChangeType.UPDATED));
	}

	@Override
	public Collection<TagsetDefinition> getTagsets() throws Exception {
		return tagManager.getTagLibrary().getTagsetDefinitions();
	}

	@Override
	public Collection<SourceDocumentReference> getSourceDocumentReferences() throws Exception {
		return graphProjectHandler.getDocuments(this.rootRevisionHash);
	}
	
	@Override
	public boolean hasDocument(String documentId) throws Exception {
		return graphProjectHandler.hasDocument(this.rootRevisionHash, documentId);
	}

	@Override
	public SourceDocument getSourceDocument(String sourceDocumentId) throws Exception {
		return graphProjectHandler.getSourceDocument(this.rootRevisionHash, sourceDocumentId);
	}
	
	@Override
	public SourceDocumentReference getSourceDocumentReference(String sourceDocumentID) throws Exception {
		return graphProjectHandler.getSourceDocumentReference(sourceDocumentID);
	}
	
	@Override
	public void delete(SourceDocumentReference sourceDocumentRef) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot delete %2$s", 
						this.projectReference, sourceDocumentRef));
		}

		String oldRootRevisionHash = this.rootRevisionHash;
		this.rootRevisionHash = gitProjectHandler.removeDocument(sourceDocumentRef);
		// TODO: remove/close all participating comments
		for (AnnotationCollectionReference collectionRef : new HashSet<>(sourceDocumentRef.getUserMarkupCollectionRefs())) {
			graphProjectHandler.removeCollection(this.rootRevisionHash, collectionRef, oldRootRevisionHash);	
			
			eventBus.post(
				new CollectionChangeEvent(
					collectionRef, sourceDocumentRef, ChangeType.DELETED));
		}
		
		
		graphProjectHandler.removeDocument(this.rootRevisionHash, sourceDocumentRef, oldRootRevisionHash);
		
        eventBus.post(new DocumentChangeEvent(sourceDocumentRef, ChangeType.DELETED));
	}

	@Override
	public void createUserMarkupCollection(
			String name, SourceDocumentReference sourceDocumentReference) {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot add Collection %2$s", 
						this.projectReference, name));
		}

		try {
			String collectionId = idGenerator.generateCollectionId();
			
			String oldRootRevisionHash = this.rootRevisionHash;
			this.rootRevisionHash = gitProjectHandler.createAnnotationCollection(
						collectionId, 
						name, 
						null, // description
						sourceDocumentReference.getUuid(), 
						null, // not originated from a fork
						true); // with push
			
			graphProjectHandler.addCollection(
				rootRevisionHash, 
				collectionId, name,  
				sourceDocumentReference,
				tagManager.getTagLibrary(),
				oldRootRevisionHash);
			
			eventBus.post(
				new CollectionChangeEvent(
					sourceDocumentReference.getUserMarkupCollectionReference(collectionId), 
					sourceDocumentReference, 
					ChangeType.CREATED));
		}
		catch (Exception e) {
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, e);
		}
		
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
	public void update(AnnotationCollection annotationCollection, List<TagReference> tagReferences) {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format(
						"Project \"%s\" is in read-only mode! Cannot update collection \"%s\".",
						projectReference,
						annotationCollection
				)
			);
		}

		try {
			URI collectionTarget = new URI(annotationCollection.getSourceDocumentId());
			Set<URI> annotationTargets = tagReferences.stream().map(TagReference::getTarget).collect(Collectors.toSet());

			if (!annotationTargets.stream().allMatch(at -> at == collectionTarget)) {
				throw new IllegalStateException("One or more annotations don't reference the same document as the collection");
			}

			// TODO: consider splitting this function up into two separate add and remove functions to avoid the potentially
			//       significant performance impact of this containsAll call
			if (annotationCollection.getTagReferences().containsAll(tagReferences)) {
				// add annotations to repo and commit
				gitProjectHandler.addOrUpdate(annotationCollection.getUuid(), tagReferences, tagManager.getTagLibrary());
				// save updates to index
				graphProjectHandler.addTagReferences(rootRevisionHash, annotationCollection, tagReferences);
				// fire annotation change event for the collection
				propertyChangeSupport.firePropertyChange(
						RepositoryChangeEvent.tagReferencesChanged.name(),
						null,
						new Pair<>(annotationCollection, tagReferences)
				);
			}
			else {
				// remove annotations from repo and commit
				Collection<TagInstance> tagInstances = tagReferences.stream().map(TagReference::getTagInstance).collect(Collectors.toSet());
				gitProjectHandler.removeTagInstances(annotationCollection.getUuid(), tagInstances);
				// save updates to index
				graphProjectHandler.removeTagReferences(rootRevisionHash, annotationCollection, tagReferences);
				// fire annotation change event for the collection
				Collection<String> tagInstanceIds = tagInstances.stream().map(TagInstance::getUuid).collect(Collectors.toList());
				propertyChangeSupport.firePropertyChange(
						RepositoryChangeEvent.tagReferencesChanged.name(), 
						new Pair<>(annotationCollection.getUuid(), tagInstanceIds),
						null
				);
			}
		}
		catch (Exception e) {
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					e
			);
		}
	}

	public void addAndCommitCollections(
			Collection<AnnotationCollectionReference> collectionReferences, String msg) throws IOException {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot add Collections", 
						this.projectReference));
		}

		String oldRootRevisionHash = this.rootRevisionHash;
		
		this.rootRevisionHash = 
			gitProjectHandler.addCollectionsToStagedAndCommit(
				collectionReferences.stream()
					.map(ref -> ref.getId())
					.collect(Collectors.toSet()), 
				msg, 
				false, // don't force
				true); // withCommit
		
		this.graphProjectHandler.updateProject(oldRootRevisionHash, this.rootRevisionHash);
	}

	@Override
	public void update(
			AnnotationCollection collection, 
			TagInstance tagInstance, Collection<Property> properties) throws IOException {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot update %2$s", 
						this.projectReference, collection));
		}

		try {
			for (Property property : properties) {
				tagInstance.addUserDefinedProperty(property);
			}
			gitProjectHandler.addOrUpdate(
					collection.getUuid(), 
					tagInstance,
					collection.getTagReferences(tagInstance), 
					tagManager.getTagLibrary());
			graphProjectHandler.updateProperties(
					GraphWorktreeProject.this.rootRevisionHash, 
					collection, tagInstance, properties);
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
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot update %2$s", 
						this.projectReference, collectionReference));
		}

		String oldRootRevisionHash = this.rootRevisionHash;

		this.rootRevisionHash = 
			gitProjectHandler.updateCollection(collectionReference);
		
		graphProjectHandler.updateCollection(
			this.rootRevisionHash, collectionReference, oldRootRevisionHash);		
		
		SourceDocumentReference documentRef = getSourceDocumentReference(collectionReference.getSourceDocumentId());
		eventBus.post(
			new CollectionChangeEvent(
				collectionReference, documentRef, ChangeType.UPDATED));
	}

	@Override
	public void delete(AnnotationCollectionReference collectionReference) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot delete %2$s", 
						this.projectReference, collectionReference));
		}

		String oldRootRevisionHash = this.rootRevisionHash;
		SourceDocumentReference documentRef = getSourceDocumentReference(collectionReference.getSourceDocumentId());
		
		this.rootRevisionHash = gitProjectHandler.removeCollection(collectionReference);
		
		graphProjectHandler.removeCollection(this.rootRevisionHash, collectionReference, oldRootRevisionHash);	
		documentRef.removeUserMarkupCollectionReference(collectionReference);
		
		eventBus.post(
			new CollectionChangeEvent(
				collectionReference, documentRef, ChangeType.DELETED));
	}
	
	public Pair<AnnotationCollection, List<TagsetDefinitionImportStatus>> loadAnnotationCollection(
			InputStream inputStream, SourceDocumentReference documentRef) throws IOException {
		TagManager tagManager = new TagManager(new TagLibrary());
		
		TeiTagLibrarySerializationHandler tagLibrarySerializationHandler = 
				new TeiTagLibrarySerializationHandler(tagManager, this.rootRevisionHash);

		TagLibrary importedLibrary =
			tagLibrarySerializationHandler.deserialize(null, inputStream);
		
		List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList = new ArrayList<>();
		for (TagsetDefinition tagset : importedLibrary) {
			boolean current = (getTagManager().getTagLibrary().getTagsetDefinition(tagset.getUuid()) != null);
			tagsetDefinitionImportStatusList.add(
				new TagsetDefinitionImportStatus(tagset, current));
		}
		
		String collectionId = idGenerator.generate();
		
		TeiUserMarkupCollectionDeserializer deserializer = 
				new TeiUserMarkupCollectionDeserializer(
						tagLibrarySerializationHandler.getTeiDocument(), tagManager.getTagLibrary(),
						collectionId);
		
		AnnotationCollection annotationCollection = new AnnotationCollection(
				collectionId, tagLibrarySerializationHandler.getTeiDocument().getContentInfoSet(),
				tagManager.getTagLibrary(), deserializer.getTagReferences(),
				documentRef.getUuid(),
				null,
				this.user.getIdentifier());
		
		return new Pair<>(annotationCollection, tagsetDefinitionImportStatusList);
	}
	
	public void importCollection(
		List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList, 
		AnnotationCollection importAnnotationCollection) throws IOException {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot import %2$s", 
						this.projectReference, importAnnotationCollection));
		}

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
			SourceDocumentReference sourceDocumentRef = 
					getSourceDocumentReference(importAnnotationCollection.getSourceDocumentId());
			
			String oldRootRevisionHash = this.rootRevisionHash;
			this.rootRevisionHash = gitProjectHandler.createAnnotationCollection(
					importAnnotationCollection.getId(), 
					importAnnotationCollection.getName(), 
					importAnnotationCollection.getContentInfoSet().getDescription(), //description
					importAnnotationCollection.getSourceDocumentId(), 
					null, // not originated from a fork
					false); // no push, because we push as part of the commit down the line after adding the Annotations
			
			graphProjectHandler.addCollection(
				rootRevisionHash, 
				importAnnotationCollection.getId(), 
				importAnnotationCollection.getName(),
				sourceDocumentRef,
				tagManager.getTagLibrary(),
				oldRootRevisionHash);
		
			AnnotationCollectionReference annotationCollectionReference =
					sourceDocumentRef.getUserMarkupCollectionReference(importAnnotationCollection.getId());
			eventBus.post(
				new CollectionChangeEvent(
					annotationCollectionReference, 
					sourceDocumentRef, 
					ChangeType.CREATED));		
			
			AnnotationCollection createdAnnotationCollection = getUserMarkupCollection(annotationCollectionReference);
			createdAnnotationCollection.addTagReferences(importAnnotationCollection.getTagReferences());

			update(
					createdAnnotationCollection, 
					importAnnotationCollection.getTagReferences());
			
			commitAndPushChanges(
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
	public List<TagsetDefinitionImportStatus> loadTagLibrary(InputStream inputStream) throws IOException {
		TeiSerializationHandlerFactory factory = new TeiSerializationHandlerFactory(this.rootRevisionHash);
		factory.setTagManager(new TagManager(new TagLibrary()));
		TagLibrarySerializationHandler tagLibrarySerializationHandler = 
				factory.getTagLibrarySerializationHandler();
		TagLibrary importedLibrary =
			tagLibrarySerializationHandler.deserialize(null, inputStream);
		
		List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList = new ArrayList<>();
		for (TagsetDefinition tagset : importedLibrary) {
			boolean current = (getTagManager().getTagLibrary().getTagsetDefinition(tagset.getUuid()) != null);
			tagsetDefinitionImportStatusList.add(
				new TagsetDefinitionImportStatus(tagset, current));
		}
		return tagsetDefinitionImportStatusList;
	}
	
	@Override
	public void importTagsets(List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList) throws IOException {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot import Tagsets", 
						this.projectReference));
		}

		for (TagsetDefinitionImportStatus tagsetDefinitionImportStatus : tagsetDefinitionImportStatusList) {
			
			if (tagsetDefinitionImportStatus.isDoImport()) {
				TagsetDefinition tagset = tagsetDefinitionImportStatus.getTagset();
				
				// new Tagset
				if (!tagsetDefinitionImportStatus.isCurrent()) {
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
	public boolean hasUntrackedChanges() throws IOException {
		return gitProjectHandler.hasUntrackedChanges();
	}
	
	public boolean hasChangesToCommitOrPush() throws Exception {
		return hasUncommittedChanges() 
				|| hasUntrackedChanges() || 
				!getUnsynchronizedCommits().isEmpty();
	}

	@Override
	public void commitAndPushChanges(String commitMsg) throws Exception {
		if (!isReadOnly()) {
			
			logger.info(
					String.format(
							"Commiting and pushing possible changes in Project %1$s.", 
							projectReference.toString()));
			String oldRootRevisionHash = this.rootRevisionHash;
			this.rootRevisionHash = gitProjectHandler.commitProject(commitMsg);
			if (oldRootRevisionHash.equals(this.rootRevisionHash)) {
				logger.info(
						String.format(
								"No changes in %1$s.", 
								projectReference.toString()));			
			}
			else {			
				logger.info(
						String.format(
								"New revision of %1$s is %2$s.", 
								projectReference.toString(), 
								this.rootRevisionHash));
			}
			this.graphProjectHandler.updateProject(oldRootRevisionHash, rootRevisionHash);
//		printStatus();
		}
	}
	
	@Override
	public void synchronizeWithRemote(OpenProjectListener openProjectListener) throws Exception {
		if (isReadOnly()) {
			throw new IllegalStateException(
				String.format("%1$s is in readonly mode! Cannot synchronize with remote!", 
						this.projectReference));
		}

		if (hasUncommittedChanges()) {
			throw new IllegalStateException("There are uncommitted changes that need to be committed first!");
		}
		
		final ProgressListener progressListener = new ProgressListener() {
			@Override
			public void setProgress(String value, Object... args) {
				openProjectListener.progress(value, args);
			}
		};

		backgroundService.submit(new DefaultProgressCallable<Boolean>() {
			
			@Override
			public Boolean call() throws Exception {
				try {
					progressListener.setProgress("Synchronizing...");
					boolean success = gitProjectHandler.synchronizeWithRemote();
					progressListener.setProgress("Finished synchronization!");
					return success;
				}
				catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
		}, new ExecutionListener<Boolean>() {
			@Override
			public void done(Boolean result) {
				if (result) {
					try {
						if (gitProjectHandler.hasConflicts()) {
							openProjectListener.failure(new IllegalStateException(
									String.format(
										"There are conflicts in Project %1$s with ID %2$s,"
										+ "please contact our support.",
										projectReference.getName(),
										projectReference.getProjectId())));						
						}
						else {
							gitProjectHandler.ensureUserBranch();
							
							rootRevisionHash = gitProjectHandler.getRootRevisionHash();
	
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
									(tagLibrary) -> gitProjectHandler.getCollections(tagLibrary, progressListener, true),
									false,
									backgroundService
							);
						}
					}
					catch (Exception e) {
						openProjectListener.failure(e);
					}
				}
				else {
					openProjectListener.ready(null);
				}
			}
			@Override
			public void error(Throwable t) {
				openProjectListener.failure(t);				
			}
		}, progressListener);
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
	public List<User> findUser(String usernameOrEmail, int offset, int limit) throws IOException {
		return gitProjectHandler.findUser(usernameOrEmail, offset, limit);
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
	
	@Override
	public String toString() {
		return this.projectReference.toString();
	}
	
	@Override
	public void setLatestContributionView(boolean enabled, OpenProjectListener openProjectListener) throws Exception {
		if (hasUncommittedChanges()) {
			throw new IllegalStateException("There are uncommitted changes that need to be committed first!");
		}

		if (gitProjectHandler.hasConflicts()) {
			openProjectListener.failure(new IllegalStateException(
					String.format(
						"There are conflicts in Project %1$s with ID %2$s,"
						+ "please contact our support.",
						projectReference.getName(),
						projectReference.getProjectId())));						
		}
		else {
			gitProjectHandler.ensureUserBranch();

			Set<Member> members = gitProjectHandler.getProjectMembers();
			List<String> possibleBranches =
					members.stream()
					.filter(member -> !member.getIdentifier().equals(getUser().getIdentifier()))
					.map(member -> "refs/remotes/origin/" + member.getIdentifier())
					.collect(Collectors.toList());
			
			if (enabled) {
				Set<LatestContribution> latestContributions =
						gitProjectHandler.getLatestContributions(possibleBranches);
	
				gitProjectHandler.setResourceProvider(
						(projectId, projectReference, projectPath, 
								localGitRepositoryManager, remoteGitServerManager, 
								credentialsProvider) -> new LatestContributionsResourceProvider(
						projectId, projectReference, 
						projectPath, 
						localGitRepositoryManager, 
						remoteGitServerManager, 
						credentialsProvider, 
						latestContributions));
			}
			else {
				gitProjectHandler.setResourceProvider(
						(projectId, projectReference,projectPath, 
								localGitRepositoryManager, remoteGitServerManager, 
								credentialsProvider) -> new SynchronizedResourceProvider(
							projectId, projectReference, 
							projectPath, 
							localGitRepositoryManager, 
							remoteGitServerManager, 
							credentialsProvider));
	
			}
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
					(tagLibrary) -> gitProjectHandler.getCollections(tagLibrary, progressListener, true),
					true,
					backgroundService
			);
		}		
	}
	
	@Override
	public boolean isReadOnly() {
		return this.gitProjectHandler.isReadOnly();
	}

}
