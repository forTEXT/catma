package de.catma.query;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.catma.core.document.Corpus;
import de.catma.core.document.source.ISourceDocument;
import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollection;
import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.usermarkup.IUserMarkupCollection;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.core.tag.ITagLibrary;
import de.catma.core.tag.TagLibraryReference;
import de.catma.core.user.User;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.Indexer;

public class DummyRepository implements IndexedRepository {
	
	private Indexer indexer;
	
	

	public DummyRepository(Indexer indexer) {
		super();
		this.indexer = indexer;
	}

	public void addPropertyChangeListener(
			PropertyChangeEvent propertyChangeEvent,
			PropertyChangeListener propertyChangeListener) {
		// TODO Auto-generated method stub

	}

	public void removePropertyChangeListener(
			PropertyChangeEvent propertyChangeEvent,
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

	public Collection<ISourceDocument> getSourceDocuments() {
		// TODO Auto-generated method stub
		return null;
	}

	public ISourceDocument getSourceDocument(String id) {
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

	public IUserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference) {
		// TODO Auto-generated method stub
		return null;
	}

	public StaticMarkupCollection getStaticMarkupCollection(
			StaticMarkupCollectionReference staticMarkupCollectionReference) {
		// TODO Auto-generated method stub
		return null;
	}

	public ITagLibrary getTagLibrary(TagLibraryReference tagLibraryReference) {
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

	public void update(IUserMarkupCollection userMarkupCollection,
			ISourceDocument sourceDocument) throws IOException {
		// TODO Auto-generated method stub

	}

	public void update(StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub

	}

	public void insert(ISourceDocument sourceDocument) throws IOException {
		// TODO Auto-generated method stub

	}

	public void createUserMarkupCollection(String name,
			ISourceDocument sourceDocument) throws IOException {
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
}
