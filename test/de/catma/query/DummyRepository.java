package de.catma.query;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.catma.document.Corpus;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollection;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.Indexer;
import de.catma.tag.Property;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.user.User;
import de.catma.util.ContentInfoSet;

public class DummyRepository implements IndexedRepository {
	
	private Indexer indexer;
	private TagManager tagManager;
	

	public DummyRepository(Indexer indexer) {
		super();
		this.indexer = indexer;
	}

	public DummyRepository(TagManager tagManager) {
		this.tagManager = tagManager;
	}

	public void addPropertyChangeListener(
			RepositoryChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		

	}

	public void removePropertyChangeListener(
			RepositoryChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		

	}

	public String getName() {
		
		return null;
	}

	public void open(Map<String, String> userIdentification) throws Exception {
		

	}

	public Collection<SourceDocument> getSourceDocuments() {
		
		return null;
	}

	public SourceDocument getSourceDocument(String id) {
		
		return null;
	}

	public Set<Corpus> getCorpora() {
		
		return null;
	}

	public Set<TagLibraryReference> getTagLibraryReferences() {
		
		return null;
	}

	public UserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference) {
		
		return null;
	}

	public StaticMarkupCollection getStaticMarkupCollection(
			StaticMarkupCollectionReference staticMarkupCollectionReference) {
		
		return null;
	}

	public TagLibrary getTagLibrary(TagLibraryReference tagLibraryReference) {
		
		return null;
	}

	public void delete(SourceDocument sourceDocument) {
		

	}

	public void delete(UserMarkupCollection userMarkupCollection) {
		

	}

	public void delete(StaticMarkupCollection staticMarkupCollection) {
		

	}

	public void update(SourceDocument sourceDocument) {
		

	}

	public void update(UserMarkupCollection userMarkupCollection,
			List<TagReference> tagReferences) {
		
		
	}
	
	public void update(List<UserMarkupCollection> userMarkupCollections,
			TagsetDefinition tagsetDefinition) {
		
		
	}


	public void update(StaticMarkupCollection staticMarkupCollection) {
		

	}

	public void insert(SourceDocument sourceDocument) throws IOException {
		

	}

	public void createUserMarkupCollection(String name,
			SourceDocument sourceDocument) throws IOException {
		

	}

	public StaticMarkupCollectionReference insert(
			StaticMarkupCollection staticMarkupCollection) {
		
		return null;
	}

	public String getIdFromURI(URI uri) {
		
		return null;
	}

	public boolean isAuthenticationRequired() {
		
		return false;
	}

	public User getUser() {
		
		return null;
	}

	public void close() {
		

	}

	public Indexer getIndexer() {

		return indexer;
	}

	public void createTagLibrary(String name) throws IOException {
		
		
	}
	public void importTagLibrary(InputStream inputStream) throws IOException {
		
		
	}
	
	public void delete(TagLibraryReference tagLibraryReference)
			throws IOException {
		
		
	}

	public void importUserMarkupCollection(InputStream inputStream,
			SourceDocument sourceDocument) throws IOException {
		
		
	}
	
	public void delete(
			UserMarkupCollectionReference userMarkupCollectionReference) {
		
		
	}
	

	public void update(SourceDocument sourceDocument,
			ContentInfoSet contentInfoSet) {
		
		
	}
	
	public void update(
			UserMarkupCollectionReference userMarkupCollectionReference,
			ContentInfoSet contentInfoSet) {
		
		
	}
	public TagManager getTagManager() {
		
		return tagManager;
	}
	
	public void createCorpus(String name) throws IOException {
		
		
		
	}
	
	public void update(Corpus corpus, SourceDocument sourceDocument) {
		
		
	}
	
	public void update(Corpus corpus,
			StaticMarkupCollectionReference staticMarkupCollectionReference) {
		
		
	}
	
	public void update(Corpus corpus,
			UserMarkupCollectionReference userMarkupCollectionReference) {
		
		
	}
	public void delete(Corpus corpus) throws IOException {
		
		
	}
	public SourceDocument getSourceDocument(UserMarkupCollectionReference umcRef) {
		
		return null;
	}
	public void update(Corpus corpus, String name) throws IOException {
		
		
	}
	
	public List<UserMarkupCollectionReference> getWritableUserMarkupCollectionRefs(
			SourceDocument sd) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	public void update(TagInstance tagInstance, Property property)
			throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	public String getFileURL(String sourceDocumentID, String... path) {
		// TODO Auto-generated method stub
		return null;
	}
}
