package de.catma.repository.db;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
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
import de.catma.core.document.repository.Repository;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollection;
import de.catma.core.document.standoffmarkup.staticmarkup.StaticMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagLibraryReference;
import de.catma.core.user.User;
import de.catma.core.util.CloseSafe;
import de.catma.repository.db.model.DBSourceDocument;
import de.catma.repository.db.model.DBUser;

public class DBRepository implements Repository {
	
	private SessionFactory sessionFactory; 
	private Configuration hibernateConfig;
	private boolean isLocal;
	private String name;
	private DBUser currentUser;
	
	private Set<Corpus> corpora;
	private Map<String,SourceDocument> sourceDocumentsByID;
	private Set<TagLibraryReference> tagLibraryReferences;


	public DBRepository(String name, boolean isLocal) {
		this.name = name;
		this.isLocal = isLocal;
		hibernateConfig = new Configuration();
		hibernateConfig.configure(
				this.getClass().getPackage().getName().replace('.', '/') 
				+ "/hibernate.cfg.xml");
		
		ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder();
		serviceRegistryBuilder.applySettings(hibernateConfig.getProperties());
		ServiceRegistry serviceRegistry = 
				serviceRegistryBuilder.buildServiceRegistry();
		
		sessionFactory = hibernateConfig.buildSessionFactory(serviceRegistry);
		corpora = new HashSet<Corpus>();
		sourceDocumentsByID = new HashMap<String, SourceDocument>();
		tagLibraryReferences = new HashSet<TagLibraryReference>();
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
		return name;
	}

	public void open(Map<String, String> userIdentification) throws Exception {
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
		
	}

	private void loadSourceDocuments(Session session) {
//		Query query = 
//				session.createQuery(
//						"from " + DBSourceDocument.class.getSimpleName() +
//						" where  "
		
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
			SourceDocument sourceDocument) throws IOException {
		// TODO Auto-generated method stub

	}

	public void update(StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub

	}

	public void insert(SourceDocument sourceDocument) throws IOException {
		DBSourceDocument dbSourceDocument = 
				new DBSourceDocument(sourceDocument);

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

	public String createIdFromURI(URI uri) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean isAuthenticationRequired() {
		return !isLocal;
	}
	
	public User getUser() {
		return currentUser;
	}

}
