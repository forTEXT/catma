package de.catma.repository.fs;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import de.catma.core.document.ContentInfoSet;
import de.catma.core.document.Corpus;
import de.catma.core.document.repository.Repository;
import de.catma.core.document.source.ISourceDocument;
import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollection;
import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.usermarkup.IUserMarkupCollection;
import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.core.tag.ITagLibrary;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagLibraryReference;
import de.catma.core.user.User;
import de.catma.core.util.Pair;
import de.catma.serialization.SerializationHandlerFactory;

class FSRepository implements Repository {
	
	static final String CONTAINER_FOLDER = "container";
	private static final String REPO_URI_SCHEME = "catma://";
	
	private String name;
	private String repoFolderPath;
	private Set<Corpus> corpora;
	private FSCorpusHandler corpusHandler;
	private FSSourceDocumentHandler sourceDocumentHandler;
	private Map<String,ISourceDocument> sourceDocumentsByID;
	private Set<TagLibraryReference> tagLibraryReferences;
	private FSTagLibraryHandler tagLibraryHandler;
	private FSUserMarkupCollectionHandler userMarkupCollectionHandler;
	
	private PropertyChangeSupport propertyChangeSupport;
	
	public FSRepository(
			String name,
			String repoFolderPath, 
			SerializationHandlerFactory serializationHandlerFactory) {
		super();
		this.propertyChangeSupport = new PropertyChangeSupport(this);
		this.name = name;
		this.repoFolderPath = repoFolderPath;
		this.corpusHandler = new FSCorpusHandler(repoFolderPath);
		this.sourceDocumentHandler = 
			new FSSourceDocumentHandler(
				repoFolderPath, 
				serializationHandlerFactory.getSourceDocumentInfoSerializationHandler());
		this.tagLibraryHandler = 
				new FSTagLibraryHandler(
						repoFolderPath, 
						serializationHandlerFactory.getTagLibrarySerializationHandler());
		
		this.userMarkupCollectionHandler = 
			new FSUserMarkupCollectionHandler(
				repoFolderPath,
				serializationHandlerFactory.getUserMarkupCollectionSerializationHandler());

	}
	
	public String getName() {
		return name;
	}
	public void open(Map<String,String> userIdentification) throws Exception {
		init();
	}
	
	public void init() throws IOException {
		
		File repoFolder = new File(repoFolderPath);
		
		if(!repoFolder.exists() && !repoFolder.mkdirs()) {
			throw new IOException("can not create repo folder " + repoFolderPath);
		}
		
		File digitalObjectsFolder = 
				new File(this.sourceDocumentHandler.getDigitalObjectsFolderPath());
		
		if(!digitalObjectsFolder.exists() && !digitalObjectsFolder.mkdirs()) {
			throw new IOException(
				"can not create repo folder " + 
				this.sourceDocumentHandler.getDigitalObjectsFolderPath());
		}
		
		File corpusFolder = new File(this.corpusHandler.getCorpusFolderPath());
		if(!corpusFolder.exists() && !corpusFolder.mkdirs()) {
			throw new IOException(
				"can not create repo folder " + 
				this.corpusHandler.getCorpusFolderPath());
		}
		
		this.sourceDocumentsByID = this.sourceDocumentHandler.loadSourceDocuments();
		this.corpora = corpusHandler.loadCorpora(this);
		
		this.tagLibraryReferences = this.tagLibraryHandler.loadTagLibraryReferences();
	}

	public Collection<ISourceDocument> getSourceDocuments() {
		return Collections.unmodifiableCollection(this.sourceDocumentsByID.values());
	}

	public Set<Corpus> getCorpora() {
		return Collections.unmodifiableSet(this.corpora);
	}
	
	public Set<TagLibraryReference> getTagLibraryReferences() {
		return Collections.unmodifiableSet(this.tagLibraryReferences);
	}

	public ITagLibrary getTagLibrary(TagLibraryReference tagLibraryReference) {
		try {
			return tagLibraryHandler.loadTagLibrary(tagLibraryReference);
		}
		catch(IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public IUserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference) {
		try {
			return userMarkupCollectionHandler.loadUserMarkupCollection(
					userMarkupCollectionReference);
		}
		catch(IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public StaticMarkupCollection getStaticMarkupCollection(
			StaticMarkupCollectionReference staticMarkupCollectionReference) {
		// TODO Auto-generated method stub
		return null;
	}

	public void delete(ISourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		
	}

	public void delete(IUserMarkupCollection userMarkupCollection) {
		// TODO Auto-generated method stub
		
	}

	public void delete(StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub
		
	}

	public void update(ISourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		
	}

	public void update(
			IUserMarkupCollection userMarkupCollection, 
			ISourceDocument sourceDocument) throws IOException {
		userMarkupCollectionHandler.saveUserMarkupCollection(
				userMarkupCollection, sourceDocument);
	}

	public void update(StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub
		
	}

	public void insert(ISourceDocument sourceDocument) throws IOException {
		sourceDocumentHandler.insert(sourceDocument);
		sourceDocumentsByID.put(sourceDocument.getID(), sourceDocument);
		this.propertyChangeSupport.firePropertyChange(
				RepositoryChangeEvent.sourceDocumentAdded.name(),
				null, sourceDocument.getID());
	}

	public void createUserMarkupCollection(
			String name, ISourceDocument sourceDocument) throws IOException {
		String id = buildCatmaUri(
				"/" + CONTAINER_FOLDER + "/" + name + ".xml");
		ContentInfoSet cis = new ContentInfoSet(name);
		IUserMarkupCollection umc = 
				new UserMarkupCollection(
						id, 
						cis, 
						new TagLibrary(id, cis.getTitle()),
						new ArrayList<TagReference>());
		
		UserMarkupCollectionReference ref = 
				userMarkupCollectionHandler.saveUserMarkupCollection(
						umc, sourceDocument);
		
		sourceDocumentHandler.addUserMarkupCollectionReference(
				ref, sourceDocument);
		
		this.propertyChangeSupport.firePropertyChange(
				RepositoryChangeEvent.userMarkupCollectionAdded.name(),
				null, new Pair<UserMarkupCollectionReference, ISourceDocument>(
						ref,sourceDocument));
	}

	public StaticMarkupCollectionReference insert(
			StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub
		return null;
	}

	public ISourceDocument getSourceDocument(String id) {
		return this.sourceDocumentsByID.get(id);
	}

	public void addPropertyChangeListener(
			RepositoryChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		this.propertyChangeSupport.addPropertyChangeListener(
				propertyChangeEvent.name(), propertyChangeListener);
	}
	
	public void removePropertyChangeListener(
			RepositoryChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		this.propertyChangeSupport.removePropertyChangeListener(
				propertyChangeEvent.name(), propertyChangeListener);
	}
	
	public static String getFileURL(String catmaUri, String... path) {
		StringBuilder builder = new StringBuilder("file://");
		for (String folder : path) {
			builder.append(folder);
		}
		builder.append(catmaUri.substring((REPO_URI_SCHEME).length()));
		return builder.toString();
	}
	
	public static String buildCatmaUri(String path) {
		return REPO_URI_SCHEME + path;
	}

	public static boolean isCatmaUri(String uri) {
		return uri.startsWith(REPO_URI_SCHEME);
	}

	public String getIdFromURI(URI uri) {
		return sourceDocumentHandler.getIDFromURI(uri);
	}
	
	public boolean isAuthenticationRequired() {
		return false;
	}
	
	public User getUser() {
		
		return new User() {
			
			public boolean isLocked() {
				return false;
			}

			public String getIdentifier() {
				return System.getProperty("user.name");
			}
			
			public String getName() {
				return System.getProperty("user.name");
			}
		};
	}
	
	public void close() {
		// TODO Auto-generated method stub
		
	}

	public void createTagLibrary(String name) throws IOException {
		// TODO Auto-generated method stub
		
	}
	

	public void importTagLibrary(InputStream inputStream) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
