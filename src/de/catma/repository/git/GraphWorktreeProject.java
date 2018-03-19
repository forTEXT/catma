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
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.document.AccessMode;
import de.catma.document.Corpus;
import de.catma.document.repository.RepositoryPropertyKey;
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
import de.catma.repository.git.graph.GraphProjectHandler;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;
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
import de.catma.user.User;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class GraphWorktreeProject implements IndexedRepository {
	
	private static final String UTF8_CONVERSION_FILE_EXTENSION = "txt";
	private static final String ORIG_INFIX = "_orig";
	private static final String TOKENIZED_FILE_EXTENSION = "json";

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private PropertyChangeSupport propertyChangeSupport;

	private GitUser user;
	private GitProjectHandler gitProjectHandler;
	private ProjectReference projectReference;
	private String rootRevisionHash;
	private GraphProjectHandler graphProjectHandler;
	private String tempDir;
	private BackgroundServiceProvider backgroundServiceProvider;

	private boolean tagManagerListenersEnabled = true;

	private IDGenerator idGenerator = new IDGenerator();
	private TagManager tagManager;
	private PropertyChangeListener tagsetDefinitionChangedListener;
	private PropertyChangeListener tagDefinitionChangedListener;
	private PropertyChangeListener userDefinedPropertyChangedListener;
	private PropertyChangeListener tagLibraryChangedListener;

	public GraphWorktreeProject(GitUser user, 
			GitProjectHandler gitProjectHandler,
			ProjectReference projectReference,
			TagManager tagManager,
			BackgroundServiceProvider backgroundServiceProvider) {
		this.user = user;
		this.gitProjectHandler = gitProjectHandler;
		this.projectReference = projectReference;
		this.tagManager = tagManager;
		this.backgroundServiceProvider = backgroundServiceProvider;
		this.propertyChangeSupport = new PropertyChangeSupport(this);
		this.graphProjectHandler = new GraphProjectHandler(this.projectReference, this.user);
		this.tempDir = RepositoryPropertyKey.TempDir.getValue();
		
	}

	@Override
	public Indexer getIndexer() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void open(OpenProjectListener openProjectListener) {
		try {
			
			this.rootRevisionHash = gitProjectHandler.getRootRevisionHash(this.projectReference);
			graphProjectHandler.ensureProjectRevisionIsLoaded(rootRevisionHash);
			initTagManagerListeners();
			openProjectListener.ready(this);
		}
		catch(Exception e) {
			openProjectListener.failure(e);
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
						@SuppressWarnings("unchecked")
						final Pair<TagLibrary, TagsetDefinition> args = 
								(Pair<TagLibrary, TagsetDefinition>)evt.getNewValue();
						
						TagLibrary tagLibrary = args.getFirst(); //TODO: obsolete
						TagsetDefinition tagsetDefinition = args.getSecond();
						
						
						addTagsetDefinition(tagsetDefinition);
					}
					else if (evt.getNewValue() == null) { //delete
						@SuppressWarnings("unchecked")
						final Pair<TagLibrary, TagsetDefinition> args = 
								(Pair<TagLibrary, TagsetDefinition>)evt.getOldValue();
//						execShield.execute(new DBOperation<Void>() {
//							public Void execute() throws Exception {
//								dbTagLibraryHandler.removeTagsetDefinition(args.getSecond());
//								return null;
//							}
//						});
					}
					else { //update
//						execShield.execute(new DBOperation<Void>() {
//							public Void execute() throws Exception {
//								dbTagLibraryHandler.updateTagsetDefinition(
//										(TagsetDefinition)evt.getNewValue());
//								return null;
//							}
//						});
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
//						execShield.execute(new DBOperation<Void>() {
//							public Void execute() throws Exception {
//								dbTagLibraryHandler.removeTagDefinition(
//										args.getFirst(), args.getSecond());
//								return null;
//							}
//						});							
					}
					else {
//						execShield.execute(new DBOperation<Void>() {
//							public Void execute() throws Exception {
//								dbTagLibraryHandler.updateTagDefinition(
//										(TagDefinition)evt.getNewValue());
//								return null;
//							}
//						});
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

				if (oldValue == null) { // insert
					
					@SuppressWarnings("unchecked")
					Pair<PropertyDefinition, TagDefinition> newPair = 
							(Pair<PropertyDefinition, TagDefinition>)newValue;
					
//					dbTagLibraryHandler.savePropertyDefinition(
//							newPair.getFirst(), newPair.getSecond());
				}
				else if (newValue == null) { // delete
					@SuppressWarnings("unchecked")
					Pair<PropertyDefinition, TagDefinition> oldPair = 
							(Pair<PropertyDefinition, TagDefinition>)oldValue;
//					dbTagLibraryHandler.removePropertyDefinition(
//							oldPair.getFirst(), oldPair.getSecond());
					
				}
				else { // update
					PropertyDefinition pd = (PropertyDefinition)evt.getNewValue();
					TagDefinition td = (TagDefinition)evt.getOldValue();
//					dbTagLibraryHandler.updatePropertyDefinition(pd, td);
				}
				
			}
		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.userPropertyDefinitionChanged,
				userDefinedPropertyChangedListener);
		
		tagLibraryChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (!tagManagerListenersEnabled) {
					return;
				}
				
				if ((evt.getNewValue() != null) && (evt.getOldValue() != null)) { //update
//					dbTagLibraryHandler.update(
//							(TagLibraryReference)evt.getNewValue());
				}
			}
		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagLibraryChanged,
				tagLibraryChangedListener);
	}

	protected void addTagDefinition(TagDefinition tagDefinition, TagsetDefinition tagsetDefinition) throws Exception {
		graphProjectHandler.addTagDefinition(
				rootRevisionHash, tagDefinition, tagsetDefinition);
	}

	private void addTagsetDefinition(TagsetDefinition tagsetDefinition) throws Exception {
		String tagsetRevisionHash = 
			gitProjectHandler.createTagset(
				projectReference.getProjectId(), 
				tagsetDefinition.getUuid(), tagsetDefinition.getName(), "");
		
		tagsetDefinition.setRevisionHash(tagsetRevisionHash);
		
		graphProjectHandler.createTagset(
			rootRevisionHash, 
			tagsetDefinition.getUuid(), 
			tagsetDefinition.getRevisionHash(),
			tagsetDefinition.getName(),
			""); //TODO: description
	}

	@Override
	public void reload() throws IOException {
		// TODO Auto-generated method stub 

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

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
		return null; //TODO:
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
					this.projectReference.getProjectId(), 
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
			
			this.rootRevisionHash = gitProjectHandler.getRootRevisionHash(this.projectReference);
	
			graphProjectHandler.insertSourceDocument(
				oldRootRevisionHash, this.rootRevisionHash,
				sourceDocument,
				Paths
					.get(RepositoryPropertyKey.GraphDbGitMountBasePath.getValue())
					.resolve(gitProjectHandler.getSourceDocumentSubmodulePath(this.user, this.projectReference, sourceDocument.getID()))
					.resolve(sourceDocument.getID() + "." + TOKENIZED_FILE_EXTENSION),
				user.getIdentifier());
			
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.sourceDocumentChanged.name(),
					null, sourceDocument.getID());

		}
		catch (Exception e) {
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, e);
		}
	}

	@Override
	public void update(SourceDocument sourceDocument, ContentInfoSet contentInfoSet) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<SourceDocument> getSourceDocuments() throws Exception {
		return graphProjectHandler.getSourceDocuments( this.rootRevisionHash);
	}

	@Override
	public SourceDocument getSourceDocument(String sourceDocumentId) throws Exception {
		return graphProjectHandler.getSourceDocument(this.rootRevisionHash, sourceDocumentId);
	}

	@Override
	public void delete(SourceDocument sourceDocument) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public SourceDocument getSourceDocument(UserMarkupCollectionReference umcRef) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void share(SourceDocument sourceDocument, String userIdentification, AccessMode accessMode)
			throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<Corpus> getCorpora() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createCorpus(String name) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(Corpus corpus, SourceDocument sourceDocument) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(Corpus corpus, UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Corpus corpus) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(Corpus corpus, String name) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void share(Corpus corpus, String userIdentification, AccessMode accessMode) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void createUserMarkupCollection(String name, SourceDocument sourceDocument) throws IOException {
		try {
			String collectionId = idGenerator.generate();
			
			String umcRevisionHash = gitProjectHandler.createMarkupCollection(
						this.projectReference.getProjectId(), 
						collectionId, 
						name, 
						null, //description
						sourceDocument.getID(), 
						sourceDocument.getRevisionHash());
			
			graphProjectHandler.createUserMarkupCollection(
				rootRevisionHash, 
				collectionId, name, umcRevisionHash, 
				sourceDocument);
			
			UserMarkupCollectionReference reference = 
					new UserMarkupCollectionReference(
							collectionId, 
							umcRevisionHash,
							new ContentInfoSet(name));
			
			sourceDocument.addUserMarkupCollectionReference(reference);
			
			propertyChangeSupport.firePropertyChange(
					RepositoryChangeEvent.userMarkupCollectionChanged.name(),
					null, new Pair<UserMarkupCollectionReference, SourceDocument>(
							reference,sourceDocument));
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

		return null;
	}

	@Override
	public UserMarkupCollection getUserMarkupCollection(UserMarkupCollectionReference userMarkupCollectionReference,
			boolean refresh) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(UserMarkupCollection userMarkupCollection, List<TagReference> tagReferences) {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(TagInstance tagInstance, Collection<Property> properties) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(List<UserMarkupCollection> userMarkupCollections, TagsetDefinition tagsetDefinition) {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(UserMarkupCollectionReference userMarkupCollectionReference, ContentInfoSet contentInfoSet) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void share(UserMarkupCollectionReference userMarkupCollectionRef, String userIdentification,
			AccessMode accessMode) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<UserMarkupCollectionReference> getWritableUserMarkupCollectionRefs(SourceDocument sd)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createTagLibrary(String name) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void importTagLibrary(InputStream inputStream) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<TagLibraryReference> getTagLibraryReferences() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TagLibrary getTagLibrary(TagLibraryReference tagLibraryReference) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(TagLibraryReference tagLibraryReference) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void share(TagLibraryReference tagLibraryReference, String userIdentification, AccessMode accessMode)
			throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isAuthenticationRequired() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public User getUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TagManager getTagManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getFile(SourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNewUserMarkupCollectionRefs(Corpus corpus) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void spawnContentFrom(String userIdentifier, boolean copyCorpora, boolean copyTagLibs) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public TagLibrary getTagLibraryFor(String uuid, Version version) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User createIfAbsent(Map<String, String> userIdentification) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
