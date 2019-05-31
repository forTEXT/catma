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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Status;

import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;

import de.catma.backgroundservice.BackgroundService;
import de.catma.document.AccessMode;
import de.catma.document.Corpus;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.repository.event.ChangeType;
import de.catma.document.repository.event.CollectionChangeEvent;
import de.catma.document.repository.event.DocumentChangeEvent;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexBufferManagerName;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.Indexer;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.indexer.indexbuffer.IndexBufferManager;
import de.catma.project.OpenProjectListener;
import de.catma.project.ProjectReference;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.graph.FileInfoProvider;
import de.catma.repository.git.graph.GraphProjectHandler;
import de.catma.repository.git.graph.tp.TPGraphProjectHandler;
import de.catma.repository.git.managers.StatusPrinter;
import de.catma.serialization.TagLibrarySerializationHandler;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;
import de.catma.serialization.tei.TeiSerializationHandlerFactory;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class GraphWorktreeProject implements IndexedRepository {
	
	private static final String UTF8_CONVERSION_FILE_EXTENSION = "txt";
	private static final String ORIG_INFIX = "_orig";
	private static final String TOKENIZED_FILE_EXTENSION = "json";

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private PropertyChangeSupport propertyChangeSupport;

	private User user;
	private GitProjectHandler gitProjectHandler;
	private ProjectReference projectReference;
	private String rootRevisionHash;
	private GraphProjectHandler graphProjectHandler;
	private String tempDir;
	private BackgroundService backgroundService;

	private boolean tagManagerListenersEnabled = true;

	private IDGenerator idGenerator = new IDGenerator();
	private TagManager tagManager;
	private PropertyChangeListener tagsetDefinitionChangedListener;
	private PropertyChangeListener tagDefinitionChangedListener;
	private PropertyChangeListener userDefinedPropertyChangedListener;
	private Indexer indexer;
	private EventBus eventBus;

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
				});
		this.tempDir = RepositoryPropertyKey.TempDir.getValue();
		this.indexer = ((TPGraphProjectHandler)this.graphProjectHandler).createIndexer();
	}
	
	private Path getTokenizedSourceDocumentPath(String documentId) {
		return Paths
			.get(new File(RepositoryPropertyKey.GraphDbGitMountBasePath.getValue()).toURI())
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
			
			this.rootRevisionHash = gitProjectHandler.getRootRevisionHash();
			this.gitProjectHandler.loadRolesPerResource();
			
			if (gitProjectHandler.hasConflicts()) {
				openProjectListener.conflictResolutionNeeded(
						new GitConflictedProject(
							projectReference,
							gitProjectHandler, documentId -> getSourceDocumentURI(documentId)));
			}
			else {
				printStatus();
				gitProjectHandler.initAndUpdateSubmodules();
				gitProjectHandler.removeStaleSubmoduleDirectories();
				gitProjectHandler.ensureDevBranches();			
				graphProjectHandler.ensureProjectRevisionIsLoaded(
						rootRevisionHash,
						tagManager,
						() -> gitProjectHandler.getTagsets(),
						() -> gitProjectHandler.getDocuments(),
						(tagLibrary) -> gitProjectHandler.getCollections(tagLibrary));
				
				initTagManagerListeners();
				openProjectListener.ready(this);
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
			
			for (UserMarkupCollectionReference collectionRef : gitProjectHandler.getDocuments().stream()
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
		tagsetDefinition.setRevisionHash(tagsetRevisionHash);
		
		String oldRootRevisionHash = this.rootRevisionHash;
		
		// project commit
		this.rootRevisionHash = gitProjectHandler.addTagsetSubmoduleToStagedAndCommit(
			tagsetDefinition.getUuid(), 
			String.format("Updated metadata of Tagset %1$s with ID %2$s", 
					tagsetDefinition.getName(), tagsetDefinition.getUuid()));
		
		graphProjectHandler.updateTagset(this.rootRevisionHash, tagsetDefinition, oldRootRevisionHash);
	}

	private void removeTagsetDefinition(TagsetDefinition tagsetDefinition) throws Exception {
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
			String.format("Added Property Definition %1$s with %2$s to Tag %3$s with ID %4$s",
				propertyDefinition.getName(),
				propertyDefinition.getUuid(),
				tagDefinition.getName(),
				tagDefinition.getUuid()));
		
		tagsetDefinition.setRevisionHash(tagsetRevision);
		String oldRootRevisionHash = this.rootRevisionHash;

		// project commit
		this.rootRevisionHash = gitProjectHandler.addTagsetSubmoduleToStagedAndCommit(
			tagsetDefinition.getUuid(), 
			String.format(
				"Added Property Definition %1$s with %2$s to Tag %3$s with ID %4$s in Tagset %5$s with ID %6$s",
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
					this.rootRevisionHash, propertyDefinition, tagDefinition, tagManager.getTagLibrary());
		
		for (String collectionId : annotationIdsByCollectionId.keySet()) {
			// TODO: check permissions if commit is allowed, if that is not the case skip git update
			
			gitProjectHandler.addAndCommitCollection(
					collectionId,
					String.format(
						"Autocommitting changes before performing an update of Annotations "
						+ "as part of a Property Definition deletion operation",
						propertyDefinition.getName()));
			
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
						.filter(tagRef -> tagRef.getTagInstanceID().equals(tagInstance.getUuid()))
						.collect(Collectors.toList()), 
					tagManager.getTagLibrary());
			}
			
			String collectionRevisionHash = 
				gitProjectHandler.addAndCommitCollection(
					collectionId,
					String.format(
						"Annotation Properties removed, caused by the removal of Tag Property %1$s ", 
						propertyDefinition.getName()));
			
			graphProjectHandler.removeProperties(
				this.rootRevisionHash, 
				collectionId, collectionRevisionHash, 
				propertyDefinition.getUuid());
		}
		
		String tagsetRevision = gitProjectHandler.removePropertyDefinition(
				propertyDefinition, tagDefinition, tagsetDefinition);
		
		tagsetDefinition.setRevisionHash(tagsetRevision);
		
		
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
						tagsetDefinition.getUuid()), true);
		
		graphProjectHandler.removePropertyDefinition(
			rootRevisionHash, propertyDefinition, tagDefinition, tagsetDefinition, oldRootRevisionHash);
	}

	private void updatePropertyDefinition(PropertyDefinition propertyDefinition, TagDefinition tagDefinition) throws Exception {
		
		TagsetDefinition tagsetDefinition = 
			tagManager.getTagLibrary().getTagsetDefinition(tagDefinition);
		
		String tagsetRevision = gitProjectHandler.createOrUpdateTag(
				tagsetDefinition.getUuid(), tagDefinition,
				String.format(
					"Updated Property Definition %1$s with %2$s in Tag %3$s with ID %4$s",
					propertyDefinition.getName(),
					propertyDefinition.getUuid(),
					tagDefinition.getName(),
					tagDefinition.getUuid()));
		
		tagsetDefinition.setRevisionHash(tagsetRevision);
		String oldRootRevisionHash = this.rootRevisionHash;

		// project commit
		this.rootRevisionHash = gitProjectHandler.addTagsetSubmoduleToStagedAndCommit(
			tagsetDefinition.getUuid(), 
			String.format(
				"Updated Property Definition %1$s with %2$s in Tag %3$s with ID %4$s in Tagset %5$s with ID %6$s",
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
		tagsetDefinition.setRevisionHash(tagsetRevision);
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
		tagsetDefinition.setRevisionHash(tagsetRevision);
		
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
		String tagsetRevision = gitProjectHandler.removeTag(tagsetDefinition, tagDefinition);
		tagsetDefinition.setRevisionHash(tagsetRevision);

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
		String tagsetRevisionHash = 
			gitProjectHandler.createTagset(
				tagsetDefinition.getUuid(), tagsetDefinition.getName(), "");//TODO:
		
		tagsetDefinition.setRevisionHash(tagsetRevisionHash);
		
		String oldRootRevisionHash = this.rootRevisionHash;
		this.rootRevisionHash = gitProjectHandler.getRootRevisionHash();
		
		graphProjectHandler.addTagset(
			rootRevisionHash, 
			tagsetDefinition,
			oldRootRevisionHash); 
	}

	@Override
	public void reload() throws IOException {
		// TODO Auto-generated method stub 

	}

	@Override
	public void close() {
		try {
			commitAllChanges(
				collectionRef -> String.format(
						"Auto-committing Collection %1$s with ID %2$s on Project close",
						collectionRef.getName(),
						collectionRef.getId()),
				String.format(
						"Auto-committing Project %1$s with ID %2$s on close", 
						projectReference.getName(),
						projectReference.getProjectId()));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			for (PropertyChangeListener listener : this.propertyChangeSupport.getPropertyChangeListeners()) {
				this.propertyChangeSupport.removePropertyChangeListener(listener);
			}
			this.propertyChangeSupport = null;
			this.eventBus = null;
		}
		catch (Exception e) {
			e.printStackTrace(); //TODO:
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
		this.propertyChangeSupport.removePropertyChangeListener(
				propertyChangeEvent.name(), propertyChangeListener);
	}

	@Override
	public String getName() {
		return projectReference.getName();
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

	@Override
	public String getFileURL(String sourceDocumentID, String... path) {
		return null; //TODO
	}
	
	private URI getSourceDocumentURI(String sourceDocumentId) {
		return Paths
		.get(new File(RepositoryPropertyKey.GitBasedRepositoryBasePath.getValue()).toURI())
		.resolve(gitProjectHandler.getSourceDocumentSubmodulePath(sourceDocumentId))
		.resolve(sourceDocumentId + "." + UTF8_CONVERSION_FILE_EXTENSION)
		.toUri();
	}

	@Override
	public void insert(SourceDocument sourceDocument) throws IOException {
		try {
			File sourceTempFile = Paths.get(new File(this.tempDir).toURI()).resolve(sourceDocument.getID()).toFile();
	
			String convertedFilename = 
					sourceDocument.getID() + "." + UTF8_CONVERSION_FILE_EXTENSION;
			
			final IndexBufferManager indexBufferManager = 
					IndexBufferManagerName.INDEXBUFFERMANAGER.getIndeBufferManager();
	
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
			
			
			indexBufferManager.add(sourceDocument, terms);
			
			logger.info("buffering tokens finished");	
			
			try (FileInputStream originalFileInputStream = new FileInputStream(sourceTempFile)) {
				
				String sourceDocRevisionHash = gitProjectHandler.createSourceDocument(
					sourceDocument.getID(), 
					originalFileInputStream,
					sourceDocument.getID() 
						+ ORIG_INFIX 
						+ "." 
						+ sourceDocument
							.getSourceContentHandler()
							.getSourceDocumentInfo()
							.getTechInfoSet()
							.getFileType()
							.getDefaultExtension(),
					new ByteArrayInputStream(
						sourceDocument.getContent().getBytes(Charset.forName("UTF-8"))), 
					convertedFilename, 
					terms,
					sourceDocument.getID() + "." + TOKENIZED_FILE_EXTENSION,
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo());
	
				sourceDocument.unload();
				sourceDocument.setRevisionHash(sourceDocRevisionHash);
			}
			
			sourceTempFile.delete();

			String oldRootRevisionHash = this.rootRevisionHash;
			this.rootRevisionHash = gitProjectHandler.getRootRevisionHash();
	
			graphProjectHandler.addSourceDocument(
				oldRootRevisionHash, this.rootRevisionHash,
				sourceDocument,
				getTokenizedSourceDocumentPath(sourceDocument.getID()));

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
	public void update(SourceDocument sourceDocument, ContentInfoSet contentInfoSet) {
		// TODO Auto-generated method stub

        eventBus.post(new DocumentChangeEvent(sourceDocument, ChangeType.UPDATED));
	}

	@Override
	public Collection<TagsetDefinition> getTagsets() throws Exception {
		return tagManager.getTagLibrary().getTagsetDefinitions();
	}

	@Override
	public Collection<SourceDocument> getSourceDocuments() throws Exception {
		return graphProjectHandler.getDocuments( this.rootRevisionHash);
	}

	@Override
	public SourceDocument getSourceDocument(String sourceDocumentId) throws Exception {
		return graphProjectHandler.getSourceDocument(this.rootRevisionHash, sourceDocumentId);
	}

	@Override
	public void delete(SourceDocument sourceDocument) throws Exception {
		for (UserMarkupCollectionReference collectionRef : sourceDocument.getUserMarkupCollectionRefs()) {
			delete(collectionRef);
		}
		
		String oldRootRevisionHash = this.rootRevisionHash;
		gitProjectHandler.removeDocument(sourceDocument);
		this.rootRevisionHash = gitProjectHandler.getRootRevisionHash(); 
		graphProjectHandler.removeDocument(this.rootRevisionHash, sourceDocument, oldRootRevisionHash);	
        eventBus.post(new DocumentChangeEvent(sourceDocument, ChangeType.DELETED));
	}

	@Override
	public SourceDocument getSourceDocument(UserMarkupCollectionReference umcRef) {
		// TODO Auto-generated method stub
		return null;
	}

	@Deprecated
	@Override
	public void share(SourceDocument sourceDocument, String userIdentification, AccessMode accessMode)
			throws IOException {
		// TODO Auto-generated method stub

	}

	@Deprecated
	@Override
	public Collection<Corpus> getCorpora() {
		return null;
	}

	@Deprecated
	@Override
	public void createCorpus(String name) throws IOException {
	}

	@Deprecated
	@Override
	public void update(Corpus corpus, SourceDocument sourceDocument) throws IOException {
	}

	@Deprecated
	@Override
	public void update(Corpus corpus, UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
	}

	@Deprecated
	@Override
	public void delete(Corpus corpus) throws IOException {
	}

	@Deprecated
	@Override
	public void update(Corpus corpus, String name) throws IOException {
	}

	@Deprecated
	@Override
	public void share(Corpus corpus, String userIdentification, AccessMode accessMode) throws IOException {
	}

	@Override
	public void createUserMarkupCollection(String name, SourceDocument sourceDocument) {
		try {
			String collectionId = idGenerator.generate();
			
			String umcRevisionHash = gitProjectHandler.createMarkupCollection(
						collectionId, 
						name, 
						null, //description
						sourceDocument.getID(), 
						sourceDocument.getRevisionHash());
			
			String oldRootRevisionHash = this.rootRevisionHash;
			this.rootRevisionHash = gitProjectHandler.getRootRevisionHash();

			graphProjectHandler.addCollection(
				rootRevisionHash, 
				collectionId, name, umcRevisionHash, 
				sourceDocument, oldRootRevisionHash);

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
	public void importUserMarkupCollection(InputStream inputStream, SourceDocument sourceDocument) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void importUserMarkupCollection(InputStream inputStream, SourceDocument sourceDocument,
			UserMarkupCollectionSerializationHandler userMarkupCollectionSerializationHandler) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public UserMarkupCollection getUserMarkupCollection(UserMarkupCollectionReference userMarkupCollectionReference)
			throws IOException {
		try {
			return graphProjectHandler.getCollection(rootRevisionHash, getTagLibrary(), userMarkupCollectionReference);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public UserMarkupCollection getUserMarkupCollection(UserMarkupCollectionReference userMarkupCollectionReference,
			boolean refresh) throws IOException {
		return getUserMarkupCollection(userMarkupCollectionReference);
	}

	@Override
	public void update(UserMarkupCollection userMarkupCollection, List<TagReference> tagReferences) {
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
						.map(tr -> tr.getTagInstanceID())
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
			UserMarkupCollection collection, 
			TagInstance tagInstance, Collection<Property> properties) throws IOException {
		try {
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
	@Deprecated
	public void update(List<UserMarkupCollection> userMarkupCollections, TagsetDefinition tagsetDefinition) {
	}

	@Override
	public void update(
			UserMarkupCollectionReference collectionReference, 
			ContentInfoSet contentInfoSet) throws Exception {
		String collectionRevision = 
			gitProjectHandler.updateCollection(collectionReference);
		collectionReference.setRevisionHash(collectionRevision);
		
		String oldRootRevisionHash = this.rootRevisionHash;
		
		// project commit
		this.rootRevisionHash = gitProjectHandler.addToStagedAndCommit(
			collectionReference, 
			String.format("Updated metadata of Collection %1$s with ID %2$s", 
					collectionReference.getName(), collectionReference.getId()));
		
		graphProjectHandler.updateCollection(
			this.rootRevisionHash, collectionReference, oldRootRevisionHash);		
		
		SourceDocument document = getSourceDocument(collectionReference.getSourceDocumentId());
		eventBus.post(
			new CollectionChangeEvent(
				collectionReference, document, ChangeType.UPDATED));
	}

	@Override
	public void delete(UserMarkupCollectionReference collectionReference) throws Exception {
		String oldRootRevisionHash = this.rootRevisionHash;
		SourceDocument document = getSourceDocument(collectionReference.getSourceDocumentId());
		
		this.rootRevisionHash = gitProjectHandler.removeCollection(collectionReference);
		
		graphProjectHandler.removeCollection(this.rootRevisionHash, collectionReference, oldRootRevisionHash);	
		eventBus.post(
			new CollectionChangeEvent(
				collectionReference, document, ChangeType.DELETED));
	}

	@Deprecated
	@Override
	public void share(UserMarkupCollectionReference userMarkupCollectionRef, String userIdentification,
			AccessMode accessMode) throws IOException {
	}

	@Override
	public List<UserMarkupCollectionReference> getWritableUserMarkupCollectionRefs(SourceDocument sd)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Deprecated
	public void createTagLibrary(String name) throws IOException {

	}

	@Override
	public void importTagLibrary(InputStream inputStream) throws IOException {
		TeiSerializationHandlerFactory factory = new TeiSerializationHandlerFactory();
		factory.setTagManager(new TagManager(new TagLibrary(null, "import")));
		TagLibrarySerializationHandler tagLibrarySerializationHandler = 
				factory.getTagLibrarySerializationHandler();
		TagLibrary importedLibrary =
			tagLibrarySerializationHandler.deserialize(null, inputStream);
		
		for (TagsetDefinition tagset : importedLibrary) {
			tagManager.addTagsetDefinition(tagset);
			
			
			for (TagDefinition tag : tagset.getRootTagDefinitions()) {
				tagManager.addTagDefinition(tagset, tag);
				
				importTagHierarchy(tag, tagset);
			}
		}
	}

	private void importTagHierarchy(TagDefinition tag, TagsetDefinition tagset) {
		for (TagDefinition childTag : tagset.getDirectChildren(tag)) {
			tagManager.addTagDefinition(tagset, childTag);
			importTagHierarchy(childTag, tagset);
		}
	}

	@Override
	@Deprecated
	public Collection<TagLibraryReference> getTagLibraryReferences() {

		return Collections.emptyList();
	}

	@Deprecated
	private TagLibrary getTagLibrary() {
		return tagManager.getTagLibrary();
	}
	
	@Override
	@Deprecated
	public TagLibrary getTagLibrary(TagLibraryReference tagLibraryReference) throws IOException {
		return getTagLibrary();
	}

	@Override
	@Deprecated
	public void delete(TagLibraryReference tagLibraryReference) throws IOException {
	}

	@Deprecated
	@Override
	public void share(TagLibraryReference tagLibraryReference, String userIdentification, AccessMode accessMode)
			throws IOException {
	}

	@Override
	@Deprecated
	public boolean isAuthenticationRequired() {
		return false;
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
	@Deprecated
	public int getNewUserMarkupCollectionRefs(Corpus corpus) {
		return 0;
	}

	@Override
	public void spawnContentFrom(String userIdentifier, boolean copyCorpora, boolean copyTagLibs) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	@Deprecated
	public TagLibrary getTagLibraryFor(String uuid, Version version) throws IOException {
		return null;
	}

	@Override
	@Deprecated
	public User createIfAbsent(Map<String, String> userIdentification) throws IOException {
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
	public void commitChanges(String commitMsg) {
		commitAllChanges(collectionRef -> commitMsg, commitMsg);
	}

	private void commitAllChanges(
		Function<UserMarkupCollectionReference, String> collectionCcommitMsgProvider, 
		String projectCommitMsg) {
		try {
			List<UserMarkupCollectionReference> collectionRefs = 
					getSourceDocuments().stream()
					.flatMap(doc -> doc.getUserMarkupCollectionRefs().stream())
					.collect(Collectors.toList());
			
			for (UserMarkupCollectionReference collectionRef : collectionRefs) {
				
				gitProjectHandler.addAndCommitCollection(
					collectionRef.getId(), 
					collectionCcommitMsgProvider.apply(collectionRef));
			}
			
			gitProjectHandler.commitProject(projectCommitMsg, true);
			
			printStatus();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void synchronizeWithRemote(OpenProjectListener openProjectListener) throws Exception {
		if (hasUncommittedChanges()) {
			throw new IllegalStateException("There are uncommitted changes that need to be committed first!");
		}
		
		for (TagsetDefinition tagset : getTagsets()) {
			gitProjectHandler.synchronizeWithRemote(tagset);
		}
		
		for (SourceDocument document : getSourceDocuments()) {
			for (UserMarkupCollectionReference collectionReference : document.getUserMarkupCollectionRefs()) {
				gitProjectHandler.synchronizeWithRemote(collectionReference);
			}
		}
		
		gitProjectHandler.synchronizeWithRemote();
		
		if (gitProjectHandler.hasConflicts()) {
			openProjectListener.conflictResolutionNeeded(
					new GitConflictedProject(
						projectReference,
						gitProjectHandler, 
						documentId -> getSourceDocumentURI(documentId)));
		}
		else {
			gitProjectHandler.loadRolesPerResource();
			gitProjectHandler.initAndUpdateSubmodules();
			gitProjectHandler.removeStaleSubmoduleDirectories();
			gitProjectHandler.ensureDevBranches();		
			rootRevisionHash = gitProjectHandler.getRootRevisionHash();
			graphProjectHandler.ensureProjectRevisionIsLoaded(
					rootRevisionHash,
					tagManager,
					() -> gitProjectHandler.getTagsets(),
					() -> gitProjectHandler.getDocuments(),
					(tagLibrary) -> gitProjectHandler.getCollections(tagLibrary));
			openProjectListener.ready(this);
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
}
