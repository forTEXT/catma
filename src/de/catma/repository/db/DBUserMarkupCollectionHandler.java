package de.catma.repository.db;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.db.CloseableSession;
import de.catma.document.Range;
import de.catma.document.repository.AccessMode;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.db.DBIndexer;
import de.catma.repository.db.model.DBCorpusUserMarkupCollection;
import de.catma.repository.db.model.DBProperty;
import de.catma.repository.db.model.DBPropertyDefinition;
import de.catma.repository.db.model.DBPropertyValue;
import de.catma.repository.db.model.DBSourceDocument;
import de.catma.repository.db.model.DBTagDefinition;
import de.catma.repository.db.model.DBTagInstance;
import de.catma.repository.db.model.DBTagLibrary;
import de.catma.repository.db.model.DBTagReference;
import de.catma.repository.db.model.DBUserMarkupCollection;
import de.catma.repository.db.model.DBUserUserMarkupCollection;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagsetDefinition;
import de.catma.util.CloseSafe;
import de.catma.util.ContentInfoSet;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

class DBUserMarkupCollectionHandler {
	
	private DBRepository dbRepository;
	private IDGenerator idGenerator;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Map<String,WeakReference<UserMarkupCollection>> umcCache;
	
	public DBUserMarkupCollectionHandler(DBRepository dbRepository) {
		this.dbRepository = dbRepository;
		this.idGenerator = new IDGenerator();
		this.umcCache = new HashMap<String, WeakReference<UserMarkupCollection>>();
	}

	void createUserMarkupCollection(String name,
			SourceDocument sourceDocument) throws IOException {
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			DBSourceDocument dbSourceDocument = 
					dbRepository.getDbSourceDocumentHandler().getDbSourceDocument(
							session, sourceDocument.getID());
			DBUserMarkupCollection dbUserMarkupCollection = 
					new DBUserMarkupCollection(
							dbSourceDocument.getSourceDocumentId(), 
							name);
			DBUserUserMarkupCollection dbUserUserMarkupCollection =
					new DBUserUserMarkupCollection(
							dbRepository.getCurrentUser(), dbUserMarkupCollection);
			
			DBTagLibrary dbTagLibrary = new DBTagLibrary(name, false);
		
			dbUserMarkupCollection.getDbUserUserMarkupCollections().add(
				dbUserUserMarkupCollection);
			session.beginTransaction();
			
			session.save(dbTagLibrary);
			dbUserMarkupCollection.setDbTagLibraryId(dbTagLibrary.getTagLibraryId());
			
			session.save(dbUserMarkupCollection);
			session.save(dbUserUserMarkupCollection);
			
			session.getTransaction().commit();
			
			UserMarkupCollectionReference reference = 
					new UserMarkupCollectionReference(
							dbUserMarkupCollection.getId(), 
							new ContentInfoSet(name));
			
			sourceDocument.addUserMarkupCollectionReference(reference);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.userMarkupCollectionChanged.name(),
					null, new Pair<UserMarkupCollectionReference, SourceDocument>(
							reference,sourceDocument));

			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}
	}

	void importUserMarkupCollection(InputStream inputStream,
			final SourceDocument sourceDocument) throws IOException {
		dbRepository.setTagManagerListenersEnabled(false);

		UserMarkupCollectionSerializationHandler userMarkupCollectionSerializationHandler = 
				dbRepository.getSerializationHandlerFactory().getUserMarkupCollectionSerializationHandler();
		
		final UserMarkupCollection umc =
				userMarkupCollectionSerializationHandler.deserialize(null, inputStream);

		dbRepository.getDbTagLibraryHandler().importTagLibrary(
				umc.getTagLibrary(), new ExecutionListener<Session>() {
					
					public void error(Throwable t) {}
					
					public void done(Session result) {
						importUserMarkupCollection(
								result, umc, sourceDocument);
					}
				}, 
				false);
	}

	private void importUserMarkupCollection(
			final Session session, final UserMarkupCollection umc,
			final SourceDocument sourceDocument) {
		
		dbRepository.getBackgroundServiceProvider().submit(
				"Importing User Markup collection...",
				new DefaultProgressCallable<DBUserMarkupCollection>() {
			public DBUserMarkupCollection call() throws Exception {
				
				DBSourceDocument dbSourceDocument = 
						dbRepository.getDbSourceDocumentHandler().getDbSourceDocument(
								session, sourceDocument.getID());
				
				DBUserMarkupCollection dbUserMarkupCollection =
					new DBUserMarkupCollection(
						dbSourceDocument.getSourceDocumentId(), 
						umc,
						Integer.valueOf(umc.getTagLibrary().getId()));
				
				addDbTagReferences(session, dbUserMarkupCollection, umc);

				dbUserMarkupCollection.getDbUserUserMarkupCollections().add(
					new DBUserUserMarkupCollection(
						dbRepository.getCurrentUser(), dbUserMarkupCollection));
				
				try {
					session.save(dbUserMarkupCollection);

					dbRepository.getIndexer().index(
							umc.getTagReferences(), 
							sourceDocument.getID(),
							dbUserMarkupCollection.getId(),
							umc.getTagLibrary());

					session.getTransaction().commit();

					CloseSafe.close(new CloseableSession(session));
					return dbUserMarkupCollection;
				}
				catch (Exception e) {
					CloseSafe.close(new CloseableSession(session,true));
					throw new IOException(e);
				}
			};
		}, 
		new ExecutionListener<DBUserMarkupCollection>() {
			public void done(DBUserMarkupCollection result) {
				umc.setId(result.getId());
				UserMarkupCollectionReference umcRef = 
						new UserMarkupCollectionReference(
								result.getId(), umc.getContentInfoSet());
				sourceDocument.addUserMarkupCollectionReference(umcRef);
				
				dbRepository.setTagManagerListenersEnabled(true);

				dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.userMarkupCollectionChanged.name(),
					null, new Pair<UserMarkupCollectionReference, SourceDocument>(
							umcRef, sourceDocument));
			}
			public void error(Throwable t) {
				dbRepository.setTagManagerListenersEnabled(true);

				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.exceptionOccurred.name(),
						null, 
						t);				
			}
		});
		
	}
	
	private void addDbTagReferences(
			Session session,
			DBUserMarkupCollection dbUserMarkupCollection, 
			UserMarkupCollection umc) {
		
		HashMap<String, DBTagInstance> dbTagInstances = 
				new HashMap<String, DBTagInstance>();
		
		for (TagReference tr : umc.getTagReferences()) {
			DBTagInstance dbTagInstance = null;

			if (dbTagInstances.containsKey(tr.getTagInstanceID())) {
				dbTagInstance = dbTagInstances.get(tr.getTagInstanceID());
			}
			else {
				TagInstance ti = tr.getTagInstance();

				TagDefinition tDef = 
					umc.getTagLibrary().getTagDefinition(
						tr.getTagInstance().getTagDefinition().getUuid());

				DBTagDefinition dbTagDefinition  = 
						(DBTagDefinition) session.load(
								DBTagDefinition.class, tDef.getId());
				
				dbTagInstance = new DBTagInstance(
					idGenerator.catmaIDToUUIDBytes(tr.getTagInstanceID()),
					dbTagDefinition);
				
				dbTagInstances.put(tr.getTagInstanceID(), dbTagInstance);
				
				for (Property p : ti.getSystemProperties()) {
					DBProperty dbProperty = 
						new DBProperty(
							dbTagDefinition.getDbPropertyDefinition(
									p.getPropertyDefinition().getUuid()),
							dbTagInstance, 
							p.getPropertyValueList().getFirstValue());

					dbTagInstance.getDbProperties().add(dbProperty);
				}
				addAuthorIfAbsent(
					dbTagDefinition.getDbPropertyDefinition(
						tDef.getPropertyDefinitionByName(
							PropertyDefinition.SystemPropertyName.catma_markupauthor.name()).getUuid()), 
						dbTagInstance);
				
				for (Property p : ti.getUserDefinedProperties()) {
					DBProperty dbProperty = 
						new DBProperty(
							dbTagDefinition.getDbPropertyDefinition(
									p.getPropertyDefinition().getUuid()),
							dbTagInstance, 
							p.getPropertyValueList().getFirstValue());

					dbTagInstance.getDbProperties().add(dbProperty);
				}
				
			}
			
			DBTagReference dbTagReference = 
				new DBTagReference(
					tr.getRange().getStartPoint(), 
					tr.getRange().getEndPoint(),
					dbUserMarkupCollection, 
					dbTagInstance);
			

			dbUserMarkupCollection.getDbTagReferences().add(dbTagReference);
		}
	}

	private void addAuthorIfAbsent(
			DBPropertyDefinition authorPDef, DBTagInstance dbTagInstance) {
		if (!dbTagInstance.hasProperty(authorPDef)) {
			DBProperty dbProperty = 
				new DBProperty(
						authorPDef, 
						dbTagInstance, 
						dbRepository.getCurrentUser().getIdentifier());
			dbTagInstance.getDbProperties().add(dbProperty);
		}
	}

	@SuppressWarnings("unchecked")
	void delete(
			Session session,
			UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
		try {
			DBUserMarkupCollection dbUserMarkupCollection = 
				(DBUserMarkupCollection) session.get(
					DBUserMarkupCollection.class,
					Integer.valueOf(userMarkupCollectionReference.getId()));
			Set<DBUserUserMarkupCollection> dbUserUserMarkupCollections =
					dbUserMarkupCollection.getDbUserUserMarkupCollections();
			DBUserUserMarkupCollection currentUserUserMarkupCollection = null;
			for (DBUserUserMarkupCollection dbUserUserMarkupCollection :
					dbUserUserMarkupCollections) {
				if (dbUserUserMarkupCollection.getDbUser().getUserId().equals(
						dbRepository.getCurrentUser().getUserId())) {
					currentUserUserMarkupCollection = 
							dbUserUserMarkupCollection;
					break;
				}
			}
			
			if (currentUserUserMarkupCollection == null) {
				throw new IllegalStateException(
						"you seem to have no access rights for this collection!");
			}
			if (!currentUserUserMarkupCollection.isOwner() 
					|| (dbUserUserMarkupCollections.size() > 1)) {
				dbUserMarkupCollection.getDbUserUserMarkupCollections().remove(
						currentUserUserMarkupCollection);
				session.delete(currentUserUserMarkupCollection);
			}
			else {
				dbUserMarkupCollection.getDbUserUserMarkupCollections().remove(
						currentUserUserMarkupCollection);

				session.delete(currentUserUserMarkupCollection);
			
				Criteria criteria = 
						session.createCriteria(DBCorpusUserMarkupCollection.class).add(
								Restrictions.eq(
									"userMarkupCollectionId",
									dbUserMarkupCollection.getUsermarkupCollectionId()));
				
				if (!criteria.list().isEmpty()) {
					for (DBCorpusUserMarkupCollection dbCorpusUserMarkupCollection 
							: (List<DBCorpusUserMarkupCollection>)criteria.list()) {
						session.delete(dbCorpusUserMarkupCollection);				
					}
				}
				
				DBTagLibrary dbTagLibrary = 
					(DBTagLibrary) session.get(
						DBTagLibrary.class,
						dbUserMarkupCollection.getDbTagLibraryId());
				
				session.delete(dbUserMarkupCollection);
				dbTagLibrary.getDbTagsetDefinitions();
				session.delete(dbTagLibrary);
				
				if (dbRepository.getIndexer() instanceof DBIndexer) {
					((DBIndexer)dbRepository.getIndexer()).removeUserMarkupCollection(
							session,
							userMarkupCollectionReference.getId());
				}
				else {
					dbRepository.getIndexer().removeUserMarkupCollection(
							userMarkupCollectionReference.getId());
				}
			}
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
	void delete(
			UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			
			session.beginTransaction();
			
			delete(session, userMarkupCollectionReference);
			
			SourceDocument sd = dbRepository.getSourceDocument(userMarkupCollectionReference);
			sd.removeUserMarkupCollectionReference(userMarkupCollectionReference);
			
			
			session.getTransaction().commit();
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.userMarkupCollectionChanged.name(),
					userMarkupCollectionReference, null);
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}
	}

	UserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
		WeakReference<UserMarkupCollection> weakUmc = umcCache.get(userMarkupCollectionReference.getId());
		if (weakUmc != null) {
			UserMarkupCollection umc = weakUmc.get();
			if (umc != null) {
				return umc;
			}
		}
		String localSourceDocUri = 
			dbRepository.getDbSourceDocumentHandler().getLocalUriFor(
					userMarkupCollectionReference);
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			Query query = session.createQuery(
					"select umc from " + 
					DBUserMarkupCollection.class.getSimpleName() + " as umc " +
					" left join umc.dbTagReferences as tr " +
					" left join tr.dbTagInstance ti " +
					" left join ti.dbProperties p " +
					" left join p.dbPropertyValues " +
					" where umc.usermarkupCollectionId = " + 
					userMarkupCollectionReference.getId()
					);
			
			DBUserMarkupCollection dbUserMarkupCollection = 
					(DBUserMarkupCollection)query.uniqueResult();
			
			TagLibrary tagLibrary = 
					dbRepository.getDbTagLibraryHandler().loadTagLibrayContent(
						session, 
						new TagLibraryReference(
							String.valueOf(dbUserMarkupCollection.getDbTagLibraryId()), 
							new ContentInfoSet(
									dbUserMarkupCollection.getAuthor(),
									dbUserMarkupCollection.getDescription(),
									dbUserMarkupCollection.getPublisher(),
									dbUserMarkupCollection.getTitle())));
			
			try {
				
				UserMarkupCollection userMarkupCollection = createUserMarkupCollection(
						dbUserMarkupCollection, localSourceDocUri, tagLibrary);
				umcCache.put(
					userMarkupCollection.getId(),
					new WeakReference<UserMarkupCollection>(userMarkupCollection));
				return userMarkupCollection;
				
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
		}
		finally {
			CloseSafe.close(new CloseableSession(session));
		}
	}

	private UserMarkupCollection createUserMarkupCollection(
			DBUserMarkupCollection dbUserMarkupCollection, 
			String localSourceDocUri, TagLibrary tagLibrary) throws URISyntaxException {
		UserMarkupCollection userMarkupCollection = 
				new UserMarkupCollection(
						dbUserMarkupCollection.getId(),
						new ContentInfoSet(
								dbUserMarkupCollection.getAuthor(), 
								dbUserMarkupCollection.getDescription(), 
								dbUserMarkupCollection.getPublisher(), 
								dbUserMarkupCollection.getTitle()), 
						tagLibrary);
		
		HashMap<DBTagInstance, TagInstance> tagInstances = 
				new HashMap<DBTagInstance, TagInstance>();
		
		for (DBTagReference dbTagReference : 
			dbUserMarkupCollection.getDbTagReferences()) {
			DBTagInstance dbTagInstance = dbTagReference.getDbTagInstance();
			TagInstance tagInstance = 
					tagInstances.get(dbTagInstance);
			
			if (tagInstance == null) {
				tagInstance = 
					new TagInstance(
						idGenerator.uuidBytesToCatmaID(
							dbTagInstance.getUuid()), 
						tagLibrary.getTagDefinition(
							idGenerator.uuidBytesToCatmaID(
								dbTagInstance.getDbTagDefinition().getUuid())));
				addProperties(tagInstance, dbTagInstance);
				tagInstances.put(dbTagInstance, tagInstance);
			}
			
			
			TagReference tr = 
				new TagReference(
						tagInstance, 
						localSourceDocUri,
						new Range(
								dbTagReference.getCharacterStart(), 
								dbTagReference.getCharacterEnd()));
			
			userMarkupCollection.addTagReference(tr);
		}
		
		return userMarkupCollection;
	}

	private void addProperties(TagInstance tagInstance,
			DBTagInstance dbTagInstance) {

		for (DBProperty dbProperty : dbTagInstance.getDbProperties()) {
			if (dbProperty.getDbPropertyDefinition().isSystemproperty()) {
				tagInstance.addSystemProperty(
					new Property(
						tagInstance.getTagDefinition().getPropertyDefinition(
							idGenerator.uuidBytesToCatmaID(
								dbProperty.getDbPropertyDefinition().getUuid())),
						new PropertyValueList(dbProperty.getPropertyValues())));
			}
			else {
				tagInstance.addUserDefinedProperty(
					new Property(
						tagInstance.getTagDefinition().getPropertyDefinition(
							idGenerator.uuidBytesToCatmaID(
								dbProperty.getDbPropertyDefinition().getUuid())),
						new PropertyValueList(dbProperty.getPropertyValues())));
				
			}
		}
	}

	void addTagReferences(UserMarkupCollection userMarkupCollection,
			List<TagReference> tagReferences) throws IOException {
		String sourceDocumentID = 
				dbRepository.getDbSourceDocumentHandler().getLocalUriFor(
						new UserMarkupCollectionReference(
								userMarkupCollection.getId(), 
								userMarkupCollection.getContentInfoSet()));
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			Set<TagInstance> incomingTagInstances = 
					new HashSet<TagInstance>();
			
			for (TagReference tr : tagReferences) {
				incomingTagInstances.add(tr.getTagInstance());
			}

			session.beginTransaction();
			DBUserMarkupCollection dbUserMarkupCollection = 
					(DBUserMarkupCollection) session.load(
							DBUserMarkupCollection.class, 
							Integer.valueOf(userMarkupCollection.getId()));
			
			for (TagInstance ti : incomingTagInstances) {
				addTagInstance(session, ti, userMarkupCollection, dbUserMarkupCollection);
			}
			if (dbRepository.getIndexer() instanceof DBIndexer) {
				((DBIndexer)dbRepository.getIndexer()).index(
						session,
						tagReferences, sourceDocumentID, 
						userMarkupCollection.getId(), 
						userMarkupCollection.getTagLibrary());
			}
			else {
				dbRepository.getIndexer().index(
						tagReferences, sourceDocumentID, 
						userMarkupCollection.getId(), 
						userMarkupCollection.getTagLibrary());
			}
			session.getTransaction().commit();
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}
	}
	

	private DBTagInstance addTagInstance(
			Session session, TagInstance ti, 
			UserMarkupCollection userMarkupCollection, 
			DBUserMarkupCollection dbUserMarkupCollection) {
		
		DBTagDefinition dbTagDefinition = 
			(DBTagDefinition) session.load(
					DBTagDefinition.class, 
					ti.getTagDefinition().getId());
		
		DBTagInstance dbTagInstance = 
			new DBTagInstance(
				idGenerator.catmaIDToUUIDBytes(ti.getUuid()),
				dbTagDefinition);
		
		for (Property prop : ti.getSystemProperties()) {
			DBPropertyDefinition dbPropDef =
					(DBPropertyDefinition) session.load(
							DBPropertyDefinition.class,
							prop.getPropertyDefinition().getId());
						
			DBProperty sysProp = 
				new DBProperty(
					dbPropDef, dbTagInstance,
					prop.getPropertyValueList().getFirstValue());
			dbTagInstance.getDbProperties().add(sysProp);
		}
		
		for (Property prop : ti.getUserDefinedProperties()) {
			DBPropertyDefinition dbPropDef =
					(DBPropertyDefinition) session.load(
							DBPropertyDefinition.class,
							prop.getPropertyDefinition().getId());
			DBProperty userProp = 
					new DBProperty(
							dbPropDef, dbTagInstance);
			for (String value : prop.getPropertyValueList().getValues()) {
				userProp.getDbPropertyValues().add(
						new DBPropertyValue(userProp, value));
			}
			dbTagInstance.getDbProperties().add(userProp);
		}
		logger.info("saving TagInstance: " + ti);
		session.saveOrUpdate(dbTagInstance);
		
		for (TagReference tr : userMarkupCollection.getTagReferences(ti.getUuid())) {
			DBTagReference dbTagReference = 
				new DBTagReference(
						tr.getRange().getStartPoint(), 
						tr.getRange().getEndPoint(), 
						dbUserMarkupCollection, dbTagInstance);
			session.saveOrUpdate(dbTagReference);
		}
		return dbTagInstance;
	}



	void updateTagsetDefinitionInUserMarkupCollections(
			List<UserMarkupCollection> userMarkupCollections,
			TagsetDefinition tagsetDefinition) throws IOException {
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			for (UserMarkupCollection userMarkupCollection : 
				userMarkupCollections) {
				DBTagLibraryHandler dbTagLibraryHandler = 
						dbRepository.getDbTagLibraryHandler();
				TagLibrary tagLibrary = 
						userMarkupCollection.getTagLibrary();
				
				Set<byte[]> deletedTagDefUuids = 
					dbTagLibraryHandler.updateTagsetDefinition(
							session, tagLibrary,
							tagLibrary.getTagsetDefinition(tagsetDefinition.getUuid()));
				DBUserMarkupCollectionUpdater updater = 
						new DBUserMarkupCollectionUpdater(dbRepository);
				updater.updateUserMarkupCollection(session, userMarkupCollection);
				//FIXME: reindexing should occurr only if something has changed, i. e. 
				// not just additions to a tagset
				dbRepository.getIndexer().reindex(
					tagsetDefinition, 
					deletedTagDefUuids,
					userMarkupCollection, 
					dbRepository.getDbSourceDocumentHandler().getLocalUriFor(
							new UserMarkupCollectionReference(
									userMarkupCollection.getId(), 
									userMarkupCollection.getContentInfoSet())));
			}

			session.getTransaction().commit();
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}
	}

	void removeTagReferences(
			List<TagReference> tagReferences) throws IOException {

		Session session = dbRepository.getSessionFactory().openSession();
		try {
			Set<TagInstance> incomingTagInstances = 
					new HashSet<TagInstance>();
			
			for (TagReference tr : tagReferences) {
				incomingTagInstances.add(tr.getTagInstance());
			}
			
			session.beginTransaction();
			for (TagInstance ti : incomingTagInstances) {
				Criteria criteria = session.createCriteria(
						DBTagInstance.class).add(
							Restrictions.eq(
								"uuid", idGenerator.catmaIDToUUIDBytes(ti.getUuid())));
				DBTagInstance dbTagInstance = (DBTagInstance) criteria.uniqueResult();
				session.delete(dbTagInstance);
			}
			
			dbRepository.getIndexer().removeTagReferences(tagReferences);
			
			session.getTransaction().commit();
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}
	}

	public void update(
			final UserMarkupCollectionReference userMarkupCollectionReference, 
			final ContentInfoSet contentInfoSet) {
		
		final Integer userMarkupCollectionId = 
			Integer.valueOf(userMarkupCollectionReference.getId());

		final String author = contentInfoSet.getAuthor();
		final String publisher = contentInfoSet.getPublisher();
		final String title = contentInfoSet.getTitle();
		final String description = contentInfoSet.getDescription();
		
		dbRepository.getBackgroundServiceProvider().submit(
				"Updating User markup collection...",
				new DefaultProgressCallable<ContentInfoSet>() {
					public ContentInfoSet call() throws Exception {
						
						Session session = 
								dbRepository.getSessionFactory().openSession();
						try {
							DBUserMarkupCollection dbUserMarkupCollection =
									(DBUserMarkupCollection) session.load(
									DBUserMarkupCollection.class, 
									userMarkupCollectionId);
							
							ContentInfoSet oldContentInfoSet = 
									new ContentInfoSet(
										dbUserMarkupCollection.getAuthor(),
										dbUserMarkupCollection.getDescription(),
										dbUserMarkupCollection.getPublisher(),
										dbUserMarkupCollection.getTitle());
							
							dbUserMarkupCollection.setAuthor(author);
							dbUserMarkupCollection.setTitle(title);
							dbUserMarkupCollection.setDescription(description);
							dbUserMarkupCollection.setPublisher(publisher);
							
							session.beginTransaction();
							session.save(dbUserMarkupCollection);
							session.getTransaction().commit();
							CloseSafe.close(new CloseableSession(session));
							return oldContentInfoSet;
						}
						catch (Exception exc) {
							CloseSafe.close(new CloseableSession(session,true));
							throw new IOException(exc);
						}
					}
				},
				new ExecutionListener<ContentInfoSet>() {
					public void done(ContentInfoSet oldContentInfoSet) {
						userMarkupCollectionReference.setContentInfoSet(contentInfoSet);
						
						dbRepository.getPropertyChangeSupport().firePropertyChange(
								RepositoryChangeEvent.userMarkupCollectionChanged.name(),
								oldContentInfoSet, userMarkupCollectionReference);
					}
					public void error(Throwable t) {
						dbRepository.getPropertyChangeSupport().firePropertyChange(
								RepositoryChangeEvent.exceptionOccurred.name(),
								null, t);
					}
				}
			);;
	}

	List<UserMarkupCollectionReference> getWritableUserMarkupCollectionRefs(
			SourceDocument sd) {
	
		List<UserMarkupCollectionReference> result = 
				new ArrayList<UserMarkupCollectionReference>();
		
		List<UserMarkupCollectionReference> allUmcRefs = 
				sd.getUserMarkupCollectionRefs();
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			for (UserMarkupCollectionReference umcRef : allUmcRefs) {
				DBUserMarkupCollection dbUmc = 
					(DBUserMarkupCollection) session.load(
							DBUserMarkupCollection.class, Integer.valueOf(umcRef.getId()));
				for (DBUserUserMarkupCollection userInfo : dbUmc.getDbUserUserMarkupCollections()) {
					if ((userInfo.isOwner()) 
						|| (userInfo.getAccessMode() == AccessMode.WRITE.getNumericRepresentation())) {
						result.add(umcRef);
						break;
					}
				}
			}
		}
		finally {
			CloseSafe.close(new CloseableSession(session));
		}
		
		
		return result;
	}
	
	void updateProperty(TagInstance tagInstance, Property property) throws IOException {
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			Query query = session.createQuery(
				"from " + DBProperty.class.getSimpleName()
				+ " where dbTagInstance.uuid = :tagInstanceID "
				+ " and dbPropertyDefinition.uuid = :propertyDefID");
			query.setBinary(
				"tagInstanceID", 
				idGenerator.catmaIDToUUIDBytes(tagInstance.getUuid()));
			query.setBinary(
				"propertyDefID", 
				idGenerator.catmaIDToUUIDBytes(
						property.getPropertyDefinition().getUuid()));
			
			DBProperty dbProperty = (DBProperty) query.uniqueResult();
			if (dbProperty == null) {
				Criteria criteria = session.createCriteria(
					DBTagInstance.class).add(
						Restrictions.eq(
							"uuid", 
							idGenerator.catmaIDToUUIDBytes(tagInstance.getUuid())));
				DBTagInstance dbTagInstance = (DBTagInstance) criteria.uniqueResult();
				
				dbProperty = new DBProperty(
					(DBPropertyDefinition) session.get(
						DBPropertyDefinition.class, 
						property.getPropertyDefinition().getId()),
					dbTagInstance);
			}
			DBUserMarkupCollectionUpdater updater = 
					new DBUserMarkupCollectionUpdater(dbRepository);
			
			session.beginTransaction();
			updater.updateDbProperty(session, dbProperty, property);
			session.saveOrUpdate(dbProperty);
			session.getTransaction().commit();
			
			dbRepository.getIndexer().updateIndex(tagInstance, property);

			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}
	}
}
