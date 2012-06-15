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
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.Indexer;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.user.User;
import de.catma.util.ContentInfoSet;

public class DummyRepository implements IndexedRepository {
	
	private Indexer indexer;
	
	

	public DummyRepository(Indexer indexer) {
		super();
		this.indexer = indexer;
	}

	public void addPropertyChangeListener(
			RepositoryChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		// TODO Auto-generated method stub

	}

	public void removePropertyChangeListener(
			RepositoryChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		// TODO Auto-generated method stub

	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public void open(Map<String, String> userIdentification) throws Exception {
		// TODO Auto-generated method stub

	}

	public Collection<SourceDocument> getSourceDocuments() {
		// TODO Auto-generated method stub
		return null;
	}

	public SourceDocument getSourceDocument(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<Corpus> getCorpora() {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<TagLibraryReference> getTagLibraryReferences() {
		// TODO Auto-generated method stub
		return null;
	}

	public UserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference) {
		// TODO Auto-generated method stub
		return null;
	}

	public StaticMarkupCollection getStaticMarkupCollection(
			StaticMarkupCollectionReference staticMarkupCollectionReference) {
		// TODO Auto-generated method stub
		return null;
	}

	public TagLibrary getTagLibrary(TagLibraryReference tagLibraryReference) {
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

	public void update(UserMarkupCollection userMarkupCollection,
			List<TagReference> tagReferences) {
		// TODO Auto-generated method stub
		
	}
	
	public void update(List<UserMarkupCollection> userMarkupCollections,
			TagsetDefinition tagsetDefinition) {
		// TODO Auto-generated method stub
		
	}


	public void update(StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub

	}

	public void insert(SourceDocument sourceDocument) throws IOException {
		// TODO Auto-generated method stub

	}

	public void createUserMarkupCollection(String name,
			SourceDocument sourceDocument) throws IOException {
		// TODO Auto-generated method stub

	}

	public StaticMarkupCollectionReference insert(
			StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getIdFromURI(URI uri) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isAuthenticationRequired() {
		// TODO Auto-generated method stub
		return false;
	}

	public User getUser() {
		// TODO Auto-generated method stub
		return null;
	}

	public void close() {
		// TODO Auto-generated method stub

	}

	public Indexer getIndexer() {

		return indexer;
	}

	public void createTagLibrary(String name) throws IOException {
		// TODO Auto-generated method stub
		
	}
	public void importTagLibrary(InputStream inputStream) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	public void delete(TagLibraryReference tagLibraryReference)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

	public void importUserMarkupCollection(InputStream inputStream,
			SourceDocument sourceDocument) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	public void delete(
			UserMarkupCollectionReference userMarkupCollectionReference) {
		// TODO Auto-generated method stub
		
	}
	

	public void update(SourceDocument sourceDocument,
			ContentInfoSet contentInfoSet) {
		// TODO Auto-generated method stub
		
	}
	
	public void update(
			UserMarkupCollectionReference userMarkupCollectionReference,
			ContentInfoSet contentInfoSet) {
		// TODO Auto-generated method stub
		
	}
	public TagManager getTagManager() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void createCorpus(String name) throws IOException {
		// TODO Auto-generated method stub
		
		
	}
	
	public void update(Corpus corpus, SourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		
	}
	
	public void update(Corpus corpus,
			StaticMarkupCollectionReference staticMarkupCollectionReference) {
		// TODO Auto-generated method stub
		
	}
	
	public void update(Corpus corpus,
			UserMarkupCollectionReference userMarkupCollectionReference) {
		// TODO Auto-generated method stub
		
	}
	public void delete(Corpus corpus) throws IOException {
		// TODO Auto-generated method stub
		
	}
	public SourceDocument getSourceDocument(UserMarkupCollectionReference umcRef) {
		// TODO Auto-generated method stub
		return null;
	}
	public void update(Corpus corpus, String name) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
