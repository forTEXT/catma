package de.catma.fsrepository;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import de.catma.core.document.Corpus;
import de.catma.core.document.repository.Repository;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollection;
import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagLibraryReference;
import de.catma.serialization.SerializationHandlerFactory;

class FSRepository implements Repository {
	
	static final String CONTAINER_FOLDER = "container";
	
	private String name;
	private String repoFolderPath;
	private Set<Corpus> corpora;
	private FSCorpusHandler corpusHandler;
	private FSSourceDocumentHandler sourceDocumentHandler;
	private Map<String,SourceDocument> sourceDocumentsByID;
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
				serializationHandlerFactory.getUserMarkupCollectionSerializationHandler());

	}
	
	public String getName() {
		return name;
	}
	public void open() throws Exception {
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

	public Collection<SourceDocument> getSourceDocuments() {
		return Collections.unmodifiableCollection(this.sourceDocumentsByID.values());
	}

	public Set<Corpus> getCorpora() {
		return Collections.unmodifiableSet(this.corpora);
	}
	
	public Set<TagLibraryReference> getTagLibraryReferences() {
		return Collections.unmodifiableSet(this.tagLibraryReferences);
	}

	public TagLibrary getTagLibrary(TagLibraryReference tagLibraryReference) {
		try {
			return tagLibraryHandler.loadTagLibrary(tagLibraryReference);
		}
		catch(IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	public UserMarkupCollection getUserMarkupCollection(
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

	public void delete(SourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		
	}

	public void delete(UserMarkupCollection userMarkupCollection) {
		// TODO Auto-generated method stub
		
	}

	public void delete(StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub
		
	}

	public void update(SourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		
	}

	public void update(UserMarkupCollection userMarkupCollection) {
		// TODO Auto-generated method stub
		
	}

	public void update(StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub
		
	}

	public void insert(SourceDocument sourceDocument) throws IOException {
		sourceDocumentHandler.insert(sourceDocument);
		sourceDocumentsByID.put(sourceDocument.getID(), sourceDocument);
		this.propertyChangeSupport.firePropertyChange(
				PropertyChangeEvent.sourceDocumentAdded.name(),
				null, sourceDocument.getID());
	}

	public void createUserMarkupCollection(
			String name, SourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		// hier gehts weiter erzeugen, speichern und event
		
	}

	public StaticMarkupCollectionReference insert(
			StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub
		return null;
	}

	public SourceDocument getSourceDocument(String id) {
		return this.sourceDocumentsByID.get(id);
	}

	public void addPropertyChangeListener(
			PropertyChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		this.propertyChangeSupport.addPropertyChangeListener(
				propertyChangeEvent.name(), propertyChangeListener);
	}
	
	public void removePropertyChangeListener(
			PropertyChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		this.propertyChangeSupport.removePropertyChangeListener(
				propertyChangeEvent.name(), propertyChangeListener);
	}
}
