package de.catma.repository.db;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import de.catma.core.document.Corpus;
import de.catma.core.document.source.ISourceDocument;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollection;
import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.usermarkup.IUserMarkupCollection;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.core.tag.ITagLibrary;
import de.catma.core.tag.PropertyDefinition;
import de.catma.core.tag.PropertyPossibleValueList;
import de.catma.core.tag.TagDefinition;
import de.catma.core.tag.TagLibraryReference;
import de.catma.core.tag.TagsetDefinition;
import de.catma.core.tag.Version;
import de.catma.core.user.User;
import de.catma.core.util.CloseSafe;
import de.catma.core.util.IDGenerator;
import de.catma.core.util.Pair;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.Indexer;
import de.catma.repository.db.model.DBPropertyDefPossibleValue;
import de.catma.repository.db.model.DBPropertyDefinition;
import de.catma.repository.db.model.DBSourceDocument;
import de.catma.repository.db.model.DBTagDefinition;
import de.catma.repository.db.model.DBTagLibrary;
import de.catma.repository.db.model.DBTagsetDefinition;
import de.catma.repository.db.model.DBUser;
import de.catma.repository.db.model.DBUserMarkupCollection;
import de.catma.repository.db.model.DBUserSourceDocument;
import de.catma.repository.db.model.DBUserTagLibrary;
import de.catma.repository.db.model.DBUserUserMarkupCollection;
import de.catma.serialization.SerializationHandlerFactory;

public class DBRepository implements IndexedRepository {
	
	private String name;
	private DBSourceDocumentHandler dbSourceDocumentHandler;
	private Indexer indexer;
	private SerializationHandlerFactory serializationHandlerFactory;
	private boolean authenticationRequired;

	private SessionFactory sessionFactory; 
	private Configuration hibernateConfig;
	
	private Set<Corpus> corpora;
	private Map<String,ISourceDocument> sourceDocumentsByID;
	private Set<TagLibraryReference> tagLibraryReferences;
	private Map<String,DBTagLibrary> tagLibrariesByID;

	private DBUser currentUser;
	private PropertyChangeSupport propertyChangeSupport;
	private ServiceRegistry serviceRegistry;

	public DBRepository(
			String name, String repoFolderPath, boolean authenticationRequired, 
			Indexer indexer, SerializationHandlerFactory serializationHandlerFactory) {
		this.propertyChangeSupport = new PropertyChangeSupport(this);
		this.name = name;
		this.dbSourceDocumentHandler = 
				new DBSourceDocumentHandler(repoFolderPath);
		this.authenticationRequired = authenticationRequired;
		this.indexer = indexer;
		this.serializationHandlerFactory = serializationHandlerFactory;
		hibernateConfig = new Configuration();
		hibernateConfig.configure(
				this.getClass().getPackage().getName().replace('.', '/') 
				+ "/hibernate.cfg.xml");
		
		ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder();
		serviceRegistryBuilder.applySettings(hibernateConfig.getProperties());
		serviceRegistry = 
				serviceRegistryBuilder.buildServiceRegistry();
		
		corpora = new HashSet<Corpus>();
		sourceDocumentsByID = new HashMap<String, ISourceDocument>();
		tagLibraryReferences = new HashSet<TagLibraryReference>();
		tagLibrariesByID = new HashMap<String, DBTagLibrary>();
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

	public String getName() {
		return name;
	}

	public void open(Map<String, String> userIdentification) throws Exception {
		sessionFactory = hibernateConfig.buildSessionFactory(serviceRegistry);
		
		Session session = sessionFactory.openSession();
		
		try {
			loadCurrentUser(session, userIdentification);
			loadContent(session); //TODO: consider lazy loading
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
			CloseSafe.close(new ClosableSession(session));
		}
	}

	private void loadContent(Session session) {
		loadSourceDocuments(session);
		loadTagLibraryReferences(session);
	}

	@SuppressWarnings("unchecked")
	private void loadTagLibraryReferences(Session session) {
		if (!currentUser.isLocked()) {
			Query query = session.createQuery(
					"select tl from "
					+ DBTagLibrary.class.getSimpleName() + " as tl "
					+ " inner join tl.dbUserTagLibraries as utl "
					+ " inner join utl.dbUser as user "
					+ " where tl.independent = true and user.userId = " 
					+ currentUser.getUserId() );
			
			for (DBTagLibrary tagLibrary : (List<DBTagLibrary>)query.list()) {
				this.tagLibrariesByID.put(tagLibrary.getId(), tagLibrary);
				this.tagLibraryReferences.add(
					new TagLibraryReference(
						tagLibrary.getName(), tagLibrary.getId()));
			}
		}
		
	}

	@SuppressWarnings("unchecked")
	private void loadSourceDocuments(Session session) {
		if (!currentUser.isLocked()) {
			Query query = 
				session.createQuery(
					"select sd from " 
					+ DBSourceDocument.class.getSimpleName() + " as sd "
					+ " inner join sd.dbUserSourceDocuments as usd "
					+ " inner join usd.dbUser as user " 
					+ " left join fetch sd.dbUserMarkupCollections as usc "
					+ " left join fetch usc.dbUserUserMarkupCollections uumc "
					+ " where user.userId = " + currentUser.getUserId());
			
			for (DBSourceDocument sd : (List<DBSourceDocument>)query.list()) {
				for (DBUserMarkupCollection dbUmc : sd.getDbUserMarkupCollections()) {
					if (dbUmc.hasAccess(currentUser)) {
						sd.addUserMarkupCollectionReference(
							new UserMarkupCollectionReference(
									dbUmc.getId(), dbUmc.getContentInfoSet()));
					}
				}
				this.sourceDocumentsByID.put(sd.getID(), sd);
			}
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

	public Collection<ISourceDocument> getSourceDocuments() {
		return  Collections.unmodifiableCollection(sourceDocumentsByID.values());
	}

	public ISourceDocument getSourceDocument(String id) {
		return sourceDocumentsByID.get(id);
	}

	public Set<Corpus> getCorpora() {
		return Collections.unmodifiableSet(corpora);
	}

	public Set<TagLibraryReference> getTagLibraryReferences() {
		return Collections.unmodifiableSet(this.tagLibraryReferences);
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

	public ITagLibrary getTagLibrary(TagLibraryReference tagLibraryReference) 
			throws IOException{
		
		DBTagLibrary tagLibrary = 
				this.tagLibrariesByID.get(tagLibraryReference.getId());
		loadTagLibrayContent(tagLibrary);
		
		return tagLibrary;
	}

	@SuppressWarnings("unchecked")
	private void loadTagLibrayContent(DBTagLibrary tagLibrary) throws IOException {

		Session session = sessionFactory.openSession();
		
		try {
			Query query = session.createQuery(
					"select tsd from "
					+ DBTagsetDefinition.class.getSimpleName() + " as tsd "
					+ "inner join fetch tsd.dbTagDefinitions as td "
					+ "inner join fetch ts.dbPropertyDefinitions as pd "
					+ "inner join fetch pd.dbPropertyDefPossibleValues "
					+ "where tsd.tagLibraryID = " + tagLibrary.getTagLibraryId());
			IDGenerator idGenerator = new IDGenerator();
			
			for (DBTagsetDefinition dbTsDef : (List<DBTagsetDefinition>)query.list()) {
				TagsetDefinition tsDef = 
						new TagsetDefinition(
							idGenerator.uuidBytesToCatmaID(dbTsDef.getUuid()),
							dbTsDef.getName(),
							new Version(dbTsDef.getVersion()));
				for (DBTagDefinition dbTDef : dbTsDef.getDbTagDefinitions()) {
					TagDefinition tDef = 
						new TagDefinition(
							idGenerator.uuidBytesToCatmaID(dbTDef.getUuid()),
							dbTDef.getName(),
							new Version(dbTDef.getVersion()),
							idGenerator.uuidBytesToCatmaID(dbTDef.getParentUuid()));
					tsDef.addTagDefinition(tDef);
					
					for (DBPropertyDefinition dbPDef : dbTDef.getDbPropertyDefinitions()) {
						List<String> pValues = new ArrayList<String>();
						for (DBPropertyDefPossibleValue dbPVal : dbPDef.getDbPropertyDefPossibleValues()) {
							pValues.add(dbPVal.getValue());
						}
						PropertyPossibleValueList ppvList = 
								new PropertyPossibleValueList(pValues, true);
						PropertyDefinition pDef = 
								new PropertyDefinition(
									idGenerator.uuidBytesToCatmaID(dbPDef.getUuid()),
									dbPDef.getName(),
									ppvList);
						if (dbPDef.isSystemproperty()) {
							tDef.addSystemPropertyDefinition(pDef);
						}
						else {
							tDef.addUserDefinedPropertyDefinition(pDef);
						}
					}
				}
				tagLibrary.add(tsDef);
				
				//TODO: store IDs
			}
		}
		catch (Exception e) {
			try {
				if (session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			}
			catch(Exception notOfInterest){}
			throw new IOException(e);
		}
		finally {
			CloseSafe.close(new ClosableSession(session));
		}
		
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
		if (sourceDocument instanceof SourceDocument) {
			sourceDocument = 
					new DBSourceDocument((SourceDocument)sourceDocument);
		}
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			session.save(sourceDocument);
			
			DBUserSourceDocument dbUserSourceDocument = 
					new DBUserSourceDocument(
							currentUser, (DBSourceDocument)sourceDocument);
			
			session.save(dbUserSourceDocument);
			
			dbSourceDocumentHandler.insert(sourceDocument);
			
			indexer.index(sourceDocument);
			
			session.getTransaction().commit();
			
			this.sourceDocumentsByID.put(sourceDocument.getID(), sourceDocument);
			
			this.propertyChangeSupport.firePropertyChange(
					PropertyChangeEvent.sourceDocumentAdded.name(),
					null, sourceDocument.getID());
		}
		catch (Exception e) {
			try {
				if (session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			}
			catch(Exception notOfInterest){}
			throw new IOException(e);
		}
		finally {
			CloseSafe.close(new ClosableSession(session));
		}
	}
	
	public StaticMarkupCollectionReference insert(
			StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub
		return null;
	}

	public void createUserMarkupCollection(String name,
			ISourceDocument sourceDocument) throws IOException {
		
		DBUserMarkupCollection dbUserMarkupCollection = 
				new DBUserMarkupCollection(
						((DBSourceDocument)sourceDocument).getSourceDocumentId(), 
						name);
		DBUserUserMarkupCollection dbUserUserMarkupCollection =
				new DBUserUserMarkupCollection(
						currentUser, dbUserMarkupCollection);
		
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			
			session.save(dbUserMarkupCollection);
			session.save(dbUserUserMarkupCollection);
			
			session.getTransaction().commit();
			
			UserMarkupCollectionReference reference = 
					new UserMarkupCollectionReference(
							dbUserMarkupCollection.getId(), 
							dbUserMarkupCollection.getContentInfoSet());
			
			this.propertyChangeSupport.firePropertyChange(
					PropertyChangeEvent.userMarkupCollectionAdded.name(),
					null, new Pair<UserMarkupCollectionReference, ISourceDocument>(
							reference,sourceDocument));

		}
		catch (Exception e) {
			try {
				if (session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			}
			catch(Exception notOfInterest){}
			throw new IOException(e);
		}
		finally {
			CloseSafe.close(new ClosableSession(session));
		}
	}
	
	public void createTagLibrary(String name) throws IOException {
		
		DBTagLibrary tagLibrary = new DBTagLibrary(name, true);
		DBUserTagLibrary dbUserTagLibrary = 
				new DBUserTagLibrary(currentUser, tagLibrary);
				
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			
			session.save(tagLibrary);
			session.save(dbUserTagLibrary);
			
			session.getTransaction().commit();
			tagLibrariesByID.put(tagLibrary.getId(), tagLibrary);
			
			this.propertyChangeSupport.firePropertyChange(
					PropertyChangeEvent.tagLibraryAdded.name(),
					null, 
					new TagLibraryReference(
							tagLibrary.getId(), tagLibrary.getName()));

		}
		catch (Exception e) {
			try {
				if (session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			}
			catch(Exception notOfInterest){}
			throw new IOException(e);
		}
		finally {
			CloseSafe.close(new ClosableSession(session));
		}
	}

	public String getIdFromURI(URI uri) {
		return dbSourceDocumentHandler.getIDFromURI(uri);
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
	
	public void close() {
		try {
			indexer.close();
		}
		finally {
			sessionFactory.close();
		}
	}
}
