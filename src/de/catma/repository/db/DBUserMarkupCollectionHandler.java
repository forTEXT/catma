package de.catma.repository.db;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.Range;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.ISourceDocument;
import de.catma.document.standoffmarkup.usermarkup.IUserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
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
import de.catma.tag.ITagLibrary;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagsetDefinition;
import de.catma.util.CloseSafe;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

class DBUserMarkupCollectionHandler {
	
	private DBRepository dbRepository;
	private IDGenerator idGenerator;
	
	public DBUserMarkupCollectionHandler(DBRepository dbRepository) {
		this.dbRepository = dbRepository;
		this.idGenerator = new IDGenerator();
	}

	void createUserMarkupCollection(String name,
			ISourceDocument sourceDocument) throws IOException {
		
		DBUserMarkupCollection dbUserMarkupCollection = 
				new DBUserMarkupCollection(
						((DBSourceDocument)sourceDocument).getSourceDocumentId(), 
						name);
		DBUserUserMarkupCollection dbUserUserMarkupCollection =
				new DBUserUserMarkupCollection(
						dbRepository.getCurrentUser(), dbUserMarkupCollection);
		
		dbUserMarkupCollection.getDbUserUserMarkupCollections().add(
				dbUserUserMarkupCollection);
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			
			session.save(dbUserMarkupCollection);
			session.save(dbUserUserMarkupCollection);
			
			session.getTransaction().commit();
			
			UserMarkupCollectionReference reference = 
					new UserMarkupCollectionReference(
							dbUserMarkupCollection.getId(), 
							dbUserMarkupCollection.getContentInfoSet());
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.userMarkupCollectionChanged.name(),
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

	void importUserMarkupCollection(InputStream inputStream,
			final ISourceDocument sourceDocument) throws IOException {
		dbRepository.setTagManagerListenersEnabled(false);

		UserMarkupCollectionSerializationHandler userMarkupCollectionSerializationHandler = 
				dbRepository.getSerializationHandlerFactory().getUserMarkupCollectionSerializationHandler();
		
		final IUserMarkupCollection umc =
				userMarkupCollectionSerializationHandler.deserialize(null, inputStream);

		dbRepository.getDbTagLibraryHandler().importTagLibrary(
				umc.getTagLibrary(), new ExecutionListener<TagLibraryImportResult>() {
					
					public void error(Throwable t) {}
					
					public void done(TagLibraryImportResult result) {
						umc.setTagLibrary(result.getDbTagLibrary());
						importUserMarkupCollection(
								result.getSession(), umc, sourceDocument,
								result.getDbTagDefinitons());
					}
				}, 
				false);
	}

	private void importUserMarkupCollection(
			final Session session, final IUserMarkupCollection umc,
			final ISourceDocument sourceDocument,
			final Map<String, DBTagDefinition> dbTagDefs) {
		
		dbRepository.getBackgroundServiceProvider().submit(
				new DefaultProgressCallable<DBUserMarkupCollection>() {
			public DBUserMarkupCollection call() throws Exception {
				
				DBUserMarkupCollection dbUserMarkupCollection =
					new DBUserMarkupCollection(
						((DBSourceDocument)sourceDocument).getSourceDocumentId(), 
						umc);
				
				addDbTagReferences(dbUserMarkupCollection, dbTagDefs);

				dbUserMarkupCollection.getDbUserUserMarkupCollections().add(
					new DBUserUserMarkupCollection(
						dbRepository.getCurrentUser(), dbUserMarkupCollection));
				
				try {
					session.save(dbUserMarkupCollection);
//TODO: indexing
//					dbRepository.getIndexer().index(
//							dbUserMarkupCollection.getTagReferences(), 
//							sourceDocument.getID(),
//							dbUserMarkupCollection.getId(),
//							umc.getTagLibrary());

					session.getTransaction().commit();

					return dbUserMarkupCollection;
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
			};
		}, 
		new ExecutionListener<DBUserMarkupCollection>() {
			public void done(DBUserMarkupCollection result) {
				UserMarkupCollectionReference umcRef = 
						new UserMarkupCollectionReference(
								result.getId(), result.getContentInfoSet());
				sourceDocument.addUserMarkupCollectionReference(umcRef);
				
				dbRepository.setTagManagerListenersEnabled(true);

				dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.userMarkupCollectionChanged.name(),
					null, new Pair<UserMarkupCollectionReference, ISourceDocument>(
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
			DBUserMarkupCollection dbUserMarkupCollection, 
			Map<String, DBTagDefinition> dbTagDefs) {
		
		HashMap<String, DBTagInstance> dbTagInstances = 
				new HashMap<String, DBTagInstance>();
		
		for (TagReference tr : dbUserMarkupCollection.getTagReferences()) {
			DBTagInstance dbTagInstance = null;

			if (dbTagInstances.containsKey(tr.getTagInstanceID())) {
				dbTagInstance = dbTagInstances.get(tr.getTagInstanceID());
			}
			else {
				TagInstance ti = tr.getTagInstance();

				TagDefinition tDef = 
					dbUserMarkupCollection.getTagLibrary().getTagDefinition(
						tr.getTagInstance().getTagDefinition().getUuid());

				DBTagDefinition dbTagDefinition = dbTagDefs.get(tDef.getUuid());
				
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

	void delete(
			UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			Object dbUserMarkupCollection = 
				session.get(
						DBUserMarkupCollection.class,
						Integer.valueOf(userMarkupCollectionReference.getId()));
			
			session.delete(dbUserMarkupCollection);
			
			session.getTransaction().commit();
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.userMarkupCollectionChanged.name(),
					userMarkupCollectionReference, null);
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

	IUserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {
		String localSourceDocUri = 
			dbRepository.getDbSourceDocumentHandler().getLocalUriFor(
					userMarkupCollectionReference);
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			Query query = session.createQuery(
					"select umc from " + 
					DBUserMarkupCollection.class.getSimpleName() + " as umc " +
					" join umc.tagLibrary " +
					" left join umc.dbTagReferences as tr " +
					" left join tr.dbTagInstance ti " +
					" left join ti.dbProperties p " +
					" left join p.dbPropertyValues " +
					" where umc.usermarkupCollectionId = " + 
					userMarkupCollectionReference.getId()
					);
			
			DBUserMarkupCollection dbUserMarkupCollection = 
					(DBUserMarkupCollection)query.list().get(0);

			dbRepository.getDbTagLibraryHandler().loadTagLibrayContent(
					session, dbUserMarkupCollection.getTagLibrary());
			
			try {
				
				initUserMarkupCollection(
						dbUserMarkupCollection, localSourceDocUri);
				return dbUserMarkupCollection;
				
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
		}
		finally {
			CloseSafe.close(new ClosableSession(session));
		}
	}

	private void initUserMarkupCollection(
			DBUserMarkupCollection dbUserMarkupCollection, 
			String localSourceDocUri) throws URISyntaxException {
		
		DBTagLibrary tagLibrary = dbUserMarkupCollection.getTagLibrary(); 
		
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
				
				tagInstances.put(dbTagInstance, tagInstance);
			}
			
			
			TagReference tr = 
				new TagReference(
						tagInstance, 
						localSourceDocUri,
						new Range(
								dbTagReference.getCharacterStart(), 
								dbTagReference.getCharacterEnd()));
			
			dbUserMarkupCollection.addTagReference(tr);
		}
		
	}

	void addTagReferences(IUserMarkupCollection userMarkupCollection,
			Collection<TagReference> tagReferences) throws IOException {
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			Set<TagInstance> incomingTagInstances = 
					new HashSet<TagInstance>();
			
			for (TagReference tr : tagReferences) {
				incomingTagInstances.add(tr.getTagInstance());
			}
			IUserMarkupCollection userMarkupCollectionDelegate = 
					((DBUserMarkupCollection)userMarkupCollection).getUserMarkupCollectionDelegate();
			session.load(
					userMarkupCollection, 
					Integer.valueOf(userMarkupCollection.getId()));
			((DBUserMarkupCollection)userMarkupCollection).setUserMarkupCollectionDelegate(
					userMarkupCollectionDelegate);
			
			session.beginTransaction();
			
			session.saveOrUpdate(userMarkupCollection.getTagLibrary());
			for (TagInstance ti : incomingTagInstances) {
				addTagReferences(
					session, ti, (DBUserMarkupCollection)userMarkupCollection);
			}
			
			session.saveOrUpdate(userMarkupCollection);
			session.getTransaction().commit();
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
	
	private void update(Session session, IUserMarkupCollection userMarkupCollection,
			List<TagReference> tagReferences) {
		
		IUserMarkupCollection userMarkupCollectionDelegate = 
				((DBUserMarkupCollection)userMarkupCollection).getUserMarkupCollectionDelegate();
		session.load(
				userMarkupCollection, 
				Integer.valueOf(userMarkupCollection.getId()));
		((DBUserMarkupCollection)userMarkupCollection).setUserMarkupCollectionDelegate(
				userMarkupCollectionDelegate);
		
		DBUserMarkupCollection dbUserMarkupCollection =
				(DBUserMarkupCollection) userMarkupCollection;
		
		Map<String, DBTagInstance> persistentTagInstances = 
				new HashMap<String,DBTagInstance>();
		
		for (DBTagReference dbTr : dbUserMarkupCollection.getDbTagReferences()) {
			persistentTagInstances.put(
				idGenerator.uuidBytesToCatmaID(dbTr.getDbTagInstance().getUuid()),
				dbTr.getDbTagInstance());
		}
		
		Map<String, TagInstance> incomingTagInstances = 
				new HashMap<String, TagInstance>();
		
		for (TagReference tr : tagReferences) {
			incomingTagInstances.put(tr.getTagInstanceID(), tr.getTagInstance());
		}
		
		removeTagReferences(
			session, incomingTagInstances, 
			persistentTagInstances, dbUserMarkupCollection);
		
		update(session, persistentTagInstances, 
				dbUserMarkupCollection, incomingTagInstances);
		
		session.saveOrUpdate(dbUserMarkupCollection);
	}

	private void removeTagReferences(
		Session session, Map<String, TagInstance> incomingTagInstances, 
		Map<String, DBTagInstance> persistentTagInstances, 
		DBUserMarkupCollection dbUserMarkupCollection) {
		
		Iterator<Map.Entry<String, DBTagInstance>> iterator = 
				persistentTagInstances.entrySet().iterator();
		
		while(iterator.hasNext()) {
			DBTagInstance dbTagInstance = iterator.next().getValue();
			if (!incomingTagInstances.containsKey(
				idGenerator.uuidBytesToCatmaID(dbTagInstance.getUuid()))) {
				dbUserMarkupCollection.getDbTagReferences().removeAll(
						dbTagInstance.getDbTagReferences());
				session.delete(dbTagInstance);
				iterator.remove();
			}
		}
	}

	private void update(
			Session session,  
			Map<String, DBTagInstance> persistentTagInstances,
			DBUserMarkupCollection dbUserMarkupCollection, 
			Map<String, TagInstance> incomingTagInstances) {
		
		for (TagInstance ti : incomingTagInstances.values()) {
			if (persistentTagInstances.containsKey(ti.getUuid())) {
				DBTagInstance dbTagInstance = 
						persistentTagInstances.get(ti.getUuid());
				update(idGenerator, dbTagInstance, ti, dbUserMarkupCollection);
			}
			else {
				addTagReferences(session, ti, dbUserMarkupCollection);
			}
		}
	}

	private void addTagReferences(
			Session session, TagInstance ti, 
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
		
		for (TagReference tr : 
			dbUserMarkupCollection.getTagReferences(ti)) {
			
			DBTagReference dbTagReference = 
					new DBTagReference(
							tr.getRange().getStartPoint(), 
							tr.getRange().getEndPoint(), 
							dbUserMarkupCollection, dbTagInstance);
			
			dbUserMarkupCollection.getDbTagReferences().add(dbTagReference);
		}
	}


	private void update(
			IDGenerator idGenerator, DBTagInstance dbTagInstance, TagInstance ti, 
			DBUserMarkupCollection dbUserMarkupCollection) {
		
		Set<Range> incomingRanges = new HashSet<Range>();
		Set<TagReference> incomingTagReferences = 
				dbUserMarkupCollection.getTagReferences(ti);
		
		for (TagReference tr : incomingTagReferences) {
			incomingRanges.add(tr.getRange());
		}
		
		Map<Range, DBTagReference> dbTagRefsByRange = 
				new HashMap<Range, DBTagReference>();
		
		Iterator<DBTagReference> iterator = 
				dbTagInstance.getDbTagReferences().iterator();
		while (iterator.hasNext()) {
			DBTagReference curDbTagRef = iterator.next();
			Range curRange = 
					new Range(
						curDbTagRef.getCharacterStart(), 
						curDbTagRef.getCharacterEnd());
			if (!incomingRanges.contains(curRange)) {
				iterator.remove();
			}
			else {
				dbTagRefsByRange.put(curRange, curDbTagRef);
			}
		}
		
		for (TagReference tr : incomingTagReferences) {
			if (!dbTagRefsByRange.containsKey(tr.getRange())) {
				DBTagReference dbTagReference = 
						new DBTagReference(
							tr.getRange().getStartPoint(), 
							tr.getRange().getEndPoint(), 
							dbUserMarkupCollection, dbTagInstance);
				dbUserMarkupCollection.getDbTagReferences().add(dbTagReference);
			}
		}
		
		for (DBProperty dbProperty : dbTagInstance.getDbProperties()) {
			Property property = 
				ti.getProperty(
					idGenerator.uuidBytesToCatmaID(
							dbProperty.getDbPropertyDefinition().getUuid()));
			Iterator<DBPropertyValue> dbPropertyValIterator = 
					dbProperty.getDbPropertyValues().iterator();
			while (dbPropertyValIterator.hasNext()) {
				DBPropertyValue curValue = dbPropertyValIterator.next();
				if (!property.getPropertyValueList().getValues().contains(curValue.getValue())) {
					dbPropertyValIterator.remove();
				}
			}
			
			for (String value : property.getPropertyValueList().getValues()) {
				if (!dbProperty.hasPropertyValue(value)) {
					dbProperty.getDbPropertyValues().add(
							new DBPropertyValue(dbProperty, value));
				}
			}
 		}
	}

	void update(List<IUserMarkupCollection> userMarkupCollections,
			TagsetDefinition tagsetDefinition) throws IOException {
		/**
		 * siehe dann auch todos in MarkupPanel!
		 */
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			for (IUserMarkupCollection userMarkupCollection : 
				userMarkupCollections) {
				DBTagLibraryHandler dbTagLibraryHandler = 
						dbRepository.getDbTagLibraryHandler();
				DBTagLibrary dbTagLibrary = 
						(DBTagLibrary)userMarkupCollection.getTagLibrary();
				ITagLibrary tagLibraryDelegate = dbTagLibrary.getTagLibraryDelegate(); 

				session.load(
					dbTagLibrary, 
					Integer.valueOf(dbTagLibrary.getId()));
				dbTagLibrary.setTagLibraryDelegate(tagLibraryDelegate);
				
				dbTagLibraryHandler.updateTagsetDefinition(
					session, dbTagLibrary,
					dbTagLibrary.getTagsetDefinition(tagsetDefinition.getUuid()));

				update(session, userMarkupCollection, userMarkupCollection.getTagReferences());
			}
			session.getTransaction().commit();
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

	void removeTagReferences(IUserMarkupCollection userMarkupCollection,
			Collection<TagReference> tagReferences) throws IOException {
		DBUserMarkupCollection dbUserMarkupCollection = 
				(DBUserMarkupCollection)userMarkupCollection;
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			Map<String, DBTagInstance> persistentTagInstances = 
					new HashMap<String,DBTagInstance>();
			
			for (DBTagReference dbTr : dbUserMarkupCollection.getDbTagReferences()) {
				persistentTagInstances.put(
					idGenerator.uuidBytesToCatmaID(dbTr.getDbTagInstance().getUuid()),
					dbTr.getDbTagInstance());
			}
			
			Map<String, TagInstance> incomingTagInstances = 
					new HashMap<String, TagInstance>();
			
			for (TagReference tr : tagReferences) {
				incomingTagInstances.put(
						tr.getTagInstanceID(), tr.getTagInstance());
			}
			
			IUserMarkupCollection userMarkupCollectionDelegate = 
					((DBUserMarkupCollection)userMarkupCollection).getUserMarkupCollectionDelegate();
			session.load(
					userMarkupCollection, 
					Integer.valueOf(userMarkupCollection.getId()));
			((DBUserMarkupCollection)userMarkupCollection).setUserMarkupCollectionDelegate(
					userMarkupCollectionDelegate);
			
			session.beginTransaction();
			
			removeTagReferences(
				session, incomingTagInstances, 
				persistentTagInstances, dbUserMarkupCollection);

			session.saveOrUpdate(userMarkupCollection);
			session.getTransaction().commit();
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

}
