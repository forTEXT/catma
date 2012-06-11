package de.catma.repository.db;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.db.CloseableSession;
import de.catma.document.Corpus;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollection;
import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.Indexer;
import de.catma.repository.db.model.DBUser;
import de.catma.serialization.SerializationHandlerFactory;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagManager;
import de.catma.tag.TagManager.TagManagerEvent;
import de.catma.tag.TagsetDefinition;
import de.catma.user.User;
import de.catma.util.CloseSafe;
import de.catma.util.ContentInfoSet;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

public class DBRepository implements IndexedRepository {
	//TODO: handle sqlstate 40001 deadlock
	
	private String name;
	
	private DBCorpusHandler dbCorpusHandler;
	private DBSourceDocumentHandler dbSourceDocumentHandler;
	private DBTagLibraryHandler dbTagLibraryHandler;
	private DBUserMarkupCollectionHandler dbUserMarkupCollectionHandler;
	
	private Indexer indexer;
	private SerializationHandlerFactory serializationHandlerFactory;
	private boolean authenticationRequired;
	private BackgroundServiceProvider backgroundServiceProvider;
	private TagManager tagManager;

	private SessionFactory sessionFactory; 
	private Configuration hibernateConfig;
	
	private DBUser currentUser;
	private PropertyChangeSupport propertyChangeSupport;
	private ServiceRegistry serviceRegistry;
	private IDGenerator idGenerator;
	
	private boolean tagManagerListenersEnabled = true;

	private PropertyChangeListener tagsetDefinitionChangedListener;
	private PropertyChangeListener tagDefinitionChangedListener;
	private PropertyChangeListener tagLibraryChangedListener;


	public DBRepository(
			String name, 
			String repoFolderPath, 
			boolean authenticationRequired, 
			TagManager tagManager, 
			BackgroundServiceProvider backgroundServiceProvider,
			Indexer indexer, 
			SerializationHandlerFactory serializationHandlerFactory,
			String url, String user, String pass) {
		

		this.name = name;
		this.authenticationRequired = authenticationRequired;
		this.tagManager = tagManager;
		this.backgroundServiceProvider = backgroundServiceProvider;
		this.indexer = indexer;
		this.serializationHandlerFactory = serializationHandlerFactory;
		
		this.propertyChangeSupport = new PropertyChangeSupport(this);
		this.idGenerator = new IDGenerator();
		
		this.dbSourceDocumentHandler = 
				new DBSourceDocumentHandler(this, repoFolderPath);
		this.dbTagLibraryHandler = new DBTagLibraryHandler(this, idGenerator);
		this.dbUserMarkupCollectionHandler = 
				new DBUserMarkupCollectionHandler(this);
		this.dbCorpusHandler = new DBCorpusHandler(this);
		
		hibernateConfig = new Configuration();
		hibernateConfig.configure(
				this.getClass().getPackage().getName().replace('.', '/') 
				+ "/hibernate.cfg.xml");
		
		hibernateConfig.setProperty("hibernate.connection.username", user);
		hibernateConfig.setProperty("hibernate.connection.url",url);
		if ((pass != null) && (!pass.isEmpty())) {
			hibernateConfig.setProperty("hibernate.connection.password", pass);
		}

		ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder();
		serviceRegistryBuilder.applySettings(hibernateConfig.getProperties());
		serviceRegistry = 
				serviceRegistryBuilder.buildServiceRegistry();
		
		initTagManagerListeners();
	}
	
	private void initTagManagerListeners() {
		tagsetDefinitionChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				
				if (!tagManagerListenersEnabled) {
					return;
				}
				
				if (evt.getOldValue() == null) {
					@SuppressWarnings("unchecked")
					Pair<TagLibrary, TagsetDefinition> args = 
							(Pair<TagLibrary, TagsetDefinition>)evt.getNewValue();
					dbTagLibraryHandler.saveTagsetDefinition(
							args.getFirst(), args.getSecond());
				}
				else if (evt.getNewValue() == null) {
					@SuppressWarnings("unchecked")
					Pair<TagLibrary, TagsetDefinition> args = 
							(Pair<TagLibrary, TagsetDefinition>)evt.getOldValue();
					dbTagLibraryHandler.removeTagsetDefinition(args.getSecond());
				}
				else {
					dbTagLibraryHandler.updateTagsetDefinition(
							(TagsetDefinition)evt.getNewValue());
				}
			}
		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagsetDefinitionChanged,
				tagsetDefinitionChangedListener);
		
		tagDefinitionChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				
				if (!tagManagerListenersEnabled) {
					return;
				}

				if (evt.getOldValue() == null) {
					@SuppressWarnings("unchecked")
					Pair<TagsetDefinition, TagDefinition> args = 
							(Pair<TagsetDefinition, TagDefinition>)evt.getNewValue();
					dbTagLibraryHandler.saveTagDefinition(
							args.getFirst(), args.getSecond());
				}
				else if (evt.getNewValue() == null) {
					@SuppressWarnings("unchecked")
					Pair<TagsetDefinition, TagDefinition> args = 
							(Pair<TagsetDefinition, TagDefinition>)evt.getOldValue();
					dbTagLibraryHandler.removeTagDefinition(args.getSecond());
				}
				else {
					dbTagLibraryHandler.updateTagDefinition(
							(TagDefinition)evt.getNewValue());
				}
			}

		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged,
				tagDefinitionChangedListener);	
		
		tagLibraryChangedListener = new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				if (!tagManagerListenersEnabled) {
					return;
				}
				
				if ((evt.getNewValue() != null) && (evt.getOldValue() != null)) { //update
					dbTagLibraryHandler.update(
							(TagLibraryReference)evt.getNewValue());
				}
			}
		};
		
		tagManager.addPropertyChangeListener(
				TagManagerEvent.tagLibraryChanged,
				tagLibraryChangedListener);
	}

	public void open(Map<String, String> userIdentification) throws Exception {
		sessionFactory = hibernateConfig.buildSessionFactory(serviceRegistry);
		
		Session session = sessionFactory.openSession();
		
		try {
			loadCurrentUser(session, userIdentification);
			loadContent(session);
		}
		catch (Exception e) {
			try {
				if (session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			}
			catch(Exception notOfInterest){}
			throw new Exception(e);
		}
		finally {
			CloseSafe.close(new CloseableSession(session));
		}
	}
	
	private void loadCurrentUser(Session session,
			Map<String, String> userIdentification) {
		
		Query query = session.createQuery(
				"from " + DBUser.class.getSimpleName() +
				" where identifier=:curIdentifier");
		query.setString(
				"curIdentifier", userIdentification.get("user.ident"));
		
		@SuppressWarnings("unchecked")
		List<DBUser> result = query.list();
		
		if (result.isEmpty()) {
			currentUser = createUser(session, userIdentification);
		}
		else {
			currentUser = result.get(0);
			if (result.size() > 1) {
				throw new IllegalStateException(
						"the repository returned more than one user " +
						"for the same identification");
			}
		}
	}

	private DBUser createUser(Session session,
			Map<String, String> userIdentification) {
		DBUser user = new DBUser(userIdentification.get("user.ident"));
		session.beginTransaction();
		session.save(user);
		session.getTransaction().commit();
		return user;
	}


	private void loadContent(Session session) 
			throws URISyntaxException, IOException, 
			InstantiationException, IllegalAccessException {
		dbSourceDocumentHandler.loadSourceDocuments(session);
		dbTagLibraryHandler.loadTagLibraryReferences(session);
		dbCorpusHandler.loadCorpora(session);
	}
	
	public void close() {
		
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagsetDefinitionChanged,
				tagsetDefinitionChangedListener);
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagDefinitionChanged,
				tagDefinitionChangedListener);	
		tagManager.removePropertyChangeListener(
				TagManagerEvent.tagLibraryChanged,
				tagLibraryChangedListener);

		dbTagLibraryHandler.close();
		dbSourceDocumentHandler.close();
		
		try {
			indexer.close();
		}
		finally {
			sessionFactory.close();
		}
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

	
	public String getName() {
		return name;
	}

	public String getIdFromURI(URI uri) {
		return dbSourceDocumentHandler.getIDFromURI(uri);
	}
		
	
	public void insert(SourceDocument sourceDocument) throws IOException {
		dbSourceDocumentHandler.insert(sourceDocument);
	}
	
	public void update(
			SourceDocument sourceDocument, ContentInfoSet contentInfoSet) {
		dbSourceDocumentHandler.update(sourceDocument, contentInfoSet);
	}
	
	public Collection<SourceDocument> getSourceDocuments() {
		return dbSourceDocumentHandler.getSourceDocuments();
	}
	
	public SourceDocument getSourceDocument(String id) {
		return dbSourceDocumentHandler.getSourceDocument(id);
	}
	
	public void delete(SourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		
	}

	
	public Set<Corpus> getCorpora() {
		return dbCorpusHandler.getCorpora();
	}

	public void createCorpus(String name) throws IOException {
		dbCorpusHandler.createCorpus(name);
	}
	
	public void update(Corpus corpus, SourceDocument sourceDocument) throws IOException {
		dbCorpusHandler.addSourceDocument(corpus, sourceDocument);
	}
	
	public void update(Corpus corpus,
			StaticMarkupCollectionReference staticMarkupCollectionReference) {
		// TODO Auto-generated method stub
		
	}
	
	public void update(Corpus corpus,
			UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
		dbCorpusHandler.addUserMarkupCollectionRef(
				corpus, userMarkupCollectionReference);
		
	}
	
	public void delete(Corpus corpus) throws IOException {
		dbCorpusHandler.delete(corpus);
	}
	
	public void createUserMarkupCollection(String name,
			SourceDocument sourceDocument) throws IOException {
		dbUserMarkupCollectionHandler.createUserMarkupCollection(
				name, sourceDocument);
	}
	
	public void importUserMarkupCollection(
			InputStream inputStream, SourceDocument sourceDocument)
			throws IOException {
		dbUserMarkupCollectionHandler.importUserMarkupCollection(
				inputStream, sourceDocument);
	}
	
	public UserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
		return dbUserMarkupCollectionHandler.getUserMarkupCollection(userMarkupCollectionReference);
	}


	public void update(final UserMarkupCollection userMarkupCollection,
			final List<TagReference> tagReferences) {
		backgroundServiceProvider.submit(
				"Saving User Markup Collection "
						+ userMarkupCollection.getName() + "... ",
				new DefaultProgressCallable<Void>() {
				public Void call() throws Exception {
					if (userMarkupCollection.getTagReferences().containsAll(tagReferences)) {
						dbUserMarkupCollectionHandler.addTagReferences(
							userMarkupCollection, tagReferences);
					}
					else {
						dbUserMarkupCollectionHandler.removeTagReferences(
							tagReferences);
					}
					return null;
				}
			}, 
			new ExecutionListener<Void>() {
				public void done(Void nothing) { /* noop */ }
				public void error(Throwable t) {
					propertyChangeSupport.firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null, 
							t);				
				}
			});
	}
	
	public void update(final List<UserMarkupCollection> userMarkupCollections,
			final TagsetDefinition tagsetDefinition) {
		backgroundServiceProvider.submit(
				"Updating Tagset " + tagsetDefinition.getName() + "... ",
				new DefaultProgressCallable<Void>() {
				public Void call() throws Exception {
					dbUserMarkupCollectionHandler.updateTagsetDefinitionInUserMarkupCollections(
							userMarkupCollections, tagsetDefinition);
					return null;
				}
			}, 
			new ExecutionListener<Void>() {
				public void done(Void nothing) { /* noop */ }
				public void error(Throwable t) {
					propertyChangeSupport.firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null, 
							t);				
				}
			});
	}
	
	
	public void update(
			UserMarkupCollectionReference userMarkupCollectionReference,
			ContentInfoSet contentInfoSet) {
		dbUserMarkupCollectionHandler.update(
				userMarkupCollectionReference, contentInfoSet);
	}

	public void delete(
			UserMarkupCollectionReference userMarkupCollectionReference)
			throws IOException {
		dbUserMarkupCollectionHandler.delete(userMarkupCollectionReference);
	}


	public StaticMarkupCollectionReference insert(
			StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public StaticMarkupCollection getStaticMarkupCollection(
			StaticMarkupCollectionReference staticMarkupCollectionReference) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void update(StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub
		
	}

	public void delete(StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub

	}

	public void createTagLibrary(String name) throws IOException {
		dbTagLibraryHandler.createTagLibrary(name);
	}
	
	public void importTagLibrary(InputStream inputStream) throws IOException {
		dbTagLibraryHandler.importTagLibrary(inputStream);
	}
	
	public Set<TagLibraryReference> getTagLibraryReferences() {
		return dbTagLibraryHandler.getTagLibraryReferences();
	}
	
	public TagLibrary getTagLibrary(TagLibraryReference tagLibraryReference) 
			throws IOException{
		return dbTagLibraryHandler.getTagLibrary(tagLibraryReference);
	}

	public void delete(TagLibraryReference tagLibraryReference) throws IOException {
		dbTagLibraryHandler.delete(tagManager, tagLibraryReference);
	}

	public boolean isAuthenticationRequired() {
		return authenticationRequired;
	}
	
	public User getUser() {
		return currentUser;
	}
	
	public Indexer getIndexer() {
		return indexer;
	}
	
	SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	DBUser getCurrentUser() {
		return currentUser;
	}
	
	PropertyChangeSupport getPropertyChangeSupport() {
		return propertyChangeSupport;
	}
	
	void setTagManagerListenersEnabled(boolean tagManagerListenersEnabled) {
		this.tagManagerListenersEnabled = tagManagerListenersEnabled;
	}
	
	SerializationHandlerFactory getSerializationHandlerFactory() {
		return serializationHandlerFactory;
	}
	
	BackgroundServiceProvider getBackgroundServiceProvider() {
		return backgroundServiceProvider;
	}
	
	DBTagLibraryHandler getDbTagLibraryHandler() {
		return dbTagLibraryHandler;
	}
	
	DBSourceDocumentHandler getDbSourceDocumentHandler() {
		return dbSourceDocumentHandler;
	}
	
	public TagManager getTagManager() {
		return tagManager;
	}
}
