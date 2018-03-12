package de.catma.repository.git;

import java.beans.PropertyChangeListener;
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
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexBufferManagerName;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.indexer.indexbuffer.IndexBufferManager;
import de.catma.project.OpenProjectListener;
import de.catma.project.ProjectReference;
import de.catma.repository.db.FileURLFactory;
import de.catma.repository.git.graph.GraphProjectHandler;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;
import de.catma.tag.Property;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.user.User;
import de.catma.util.IDGenerator;

public class GraphWorktreeProject implements Repository {
	
	private static final String UTF8_CONVERSION_FILE_EXTENSION = "txt";
	private static final String ORIG_INFIX = "_orig";
	private static final String TOKENIZED_FILE_EXTENSION = "json";

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private GitUser user;
	private GitProjectHandler gitProjectHandler;
	private ProjectReference projectReference;
	private String rootRevisionHash;
	private GraphProjectHandler graphProjectHandler;
	private String tempDir;
	private BackgroundServiceProvider backgroundServiceProvider;


	public GraphWorktreeProject(GitUser user, 
			GitProjectHandler gitProjectHandler,
			ProjectReference projectReference,
			BackgroundServiceProvider backgroundServiceProvider) {
		this.user = user;
		this.gitProjectHandler = gitProjectHandler;
		this.projectReference = projectReference;
		this.backgroundServiceProvider = backgroundServiceProvider;
		this.graphProjectHandler = new GraphProjectHandler();
		this.tempDir = RepositoryPropertyKey.TempDir.getValue();
	}

	@Override
	public void open(OpenProjectListener openProjectListener) {
		try {
			
			this.rootRevisionHash = gitProjectHandler.getRootRevisionHash(this.projectReference);
			graphProjectHandler.ensureProjectRevisionIsLoaded(this.projectReference, rootRevisionHash);
			
			openProjectListener.ready(this);
		}
		catch(Exception e) {
			openProjectListener.failure(e);
		}
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
	public void addPropertyChangeListener(RepositoryChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removePropertyChangeListener(RepositoryChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
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
	public void insert(SourceDocument sourceDocument) throws Exception {
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
			this.projectReference.getProjectId(), oldRootRevisionHash, this.rootRevisionHash,
			sourceDocument,
			Paths
				.get(RepositoryPropertyKey.GraphDbGitMountBasePath.getValue())
				.resolve(gitProjectHandler.getSourceDocumentSubmodulePath(this.user, this.projectReference, sourceDocument.getID()))
				.resolve(sourceDocument.getID() + "." + TOKENIZED_FILE_EXTENSION),
			indexBufferManager, 
			backgroundServiceProvider.getBackgroundService());
	}

	@Override
	public void update(SourceDocument sourceDocument, ContentInfoSet contentInfoSet) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<SourceDocument> getSourceDocuments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SourceDocument getSourceDocument(String id) {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub
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
