package de.catma.repository.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.db.CloseableSession;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.repository.db.model.DBPropertyDefPossibleValue;
import de.catma.repository.db.model.DBPropertyDefinition;
import de.catma.repository.db.model.DBTagDefinition;
import de.catma.repository.db.model.DBTagLibrary;
import de.catma.repository.db.model.DBTagsetDefinition;
import de.catma.repository.db.model.DBUserTagLibrary;
import de.catma.serialization.TagLibrarySerializationHandler;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyPossibleValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.util.CloseSafe;
import de.catma.util.ContentInfoSet;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

class DBTagLibraryHandler {
	
	private static String MAINTAIN_TAGDEFHIERARCHY = 
			"UPDATE " + DBTagDefinition.TABLE + " td1, " + 
			DBTagDefinition.TABLE + " td2, " +
			DBTagsetDefinition.TABLE + " tsd " +
			"SET td1.parentID = td2.tagDefinitionID " +
			"WHERE td1.parentUuid = td2.uuid AND " +
			"td1.parentID IS NULL AND " + 
			"td1.parentUuid IS NOT NULL " +
			"AND td1.tagsetDefinitionID = tsd.tagsetDefinitionID " +
			"AND td2.tagsetDefinitionID = tsd.tagsetDefinitionID " +
			"AND tsd.tagLibraryID = :curTagLibraryId";

	private DBRepository dbRepository;
	private Set<TagLibraryReference> tagLibraryReferences;
	private IDGenerator idGenerator;

	public DBTagLibraryHandler(DBRepository dbRepository, IDGenerator idGenerator) {
		this.dbRepository = dbRepository;
		this.idGenerator = idGenerator;
		tagLibraryReferences = new HashSet<TagLibraryReference>();
	}

	public void createTagLibrary(String name) throws IOException {

		DBTagLibrary dbTagLibrary = new DBTagLibrary(name, true);
		DBUserTagLibrary dbUserTagLibrary = 
				new DBUserTagLibrary(dbRepository.getCurrentUser(), dbTagLibrary);
		dbTagLibrary.getDbUserTagLibraries().add(dbUserTagLibrary);
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			
			session.save(dbTagLibrary);
			
			session.getTransaction().commit();
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.tagLibraryChanged.name(),
					null, 
					new TagLibraryReference(
							dbTagLibrary.getId(), new ContentInfoSet(name)));

			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}
	}
	
	@SuppressWarnings("unchecked") 
	void loadTagLibraryReferences(Session session) {
		if (!dbRepository.getCurrentUser().isLocked()) {
			Query query = session.createQuery(
					"select tl from "
					+ DBTagLibrary.class.getSimpleName() + " as tl "
					+ " inner join tl.dbUserTagLibraries as utl "
					+ " inner join utl.dbUser as user "
					+ " where tl.independent = true and user.userId = " 
					+ dbRepository.getCurrentUser().getUserId() );
			
			for (DBTagLibrary dbTagLibrary : (List<DBTagLibrary>)query.list()) {
				this.tagLibraryReferences.add(
					new TagLibraryReference(
							dbTagLibrary.getId(), 
							new ContentInfoSet(
								dbTagLibrary.getAuthor(),
								dbTagLibrary.getDescription(),
								dbTagLibrary.getPublisher(),
								dbTagLibrary.getTitle())));
			}
		}
		
	}
	
	Set<TagLibraryReference> getTagLibraryReferences() {
		return Collections.unmodifiableSet(this.tagLibraryReferences);
	}
	
	TagLibrary getTagLibrary(TagLibraryReference tagLibraryReference) 
			throws IOException{

		Session session = dbRepository.getSessionFactory().openSession();
		try {
			TagLibrary tagLibrary = 
					loadTagLibrayContent(session, tagLibraryReference);
			return tagLibrary;
		}
		finally {
			CloseSafe.close(new CloseableSession(session));
		}
	}
	
	@SuppressWarnings("unchecked")
	TagLibrary loadTagLibrayContent(Session session, TagLibraryReference reference) throws IOException {

		Query query = session.createQuery(
				"select distinct tsd from "
				+ DBTagsetDefinition.class.getSimpleName() + " as tsd "
				+ "left join fetch tsd.dbTagDefinitions as td "
				+ "left join fetch td.dbPropertyDefinitions as pd "
				+ "left join fetch pd.dbPropertyDefPossibleValues "
				+ "where tsd.tagLibraryId = " + reference.getId());
		
		List<DBTagsetDefinition> dbTagsetDefinitions = (List<DBTagsetDefinition>)query.list();
		TagLibrary tagLibrary = new TagLibrary(reference.getId(), reference.toString());
		
		for (DBTagsetDefinition dbTsDef : dbTagsetDefinitions) {
			tagLibrary.add(createTagsetDefinition(dbTsDef));
		}
		
		return tagLibrary;
	}

	private TagsetDefinition createTagsetDefinition(DBTagsetDefinition dbTsDef) {
		TagsetDefinition tsDef = 
			new TagsetDefinition(
				dbTsDef.getTagsetDefinitionId(),
				idGenerator.uuidBytesToCatmaID(dbTsDef.getUuid()),
				dbTsDef.getName(),
				new Version(dbTsDef.getVersion()));

		for (DBTagDefinition dbTDef : dbTsDef.getDbTagDefinitions()) {
			TagDefinition tDef = 
				new TagDefinition(
					dbTDef.getTagDefinitionId(),
					idGenerator.uuidBytesToCatmaID(dbTDef.getUuid()),
					dbTDef.getName(),
					new Version(dbTDef.getVersion()),
					dbTDef.getParentId(),
					idGenerator.uuidBytesToCatmaID(dbTDef.getParentUuid()));
			tsDef.addTagDefinition(tDef);
		
			for (DBPropertyDefinition dbPDef : 
				dbTDef.getDbPropertyDefinitions()) {
				
				List<String> pValues = new ArrayList<String>();
				for (DBPropertyDefPossibleValue dbPVal : 
					dbPDef.getDbPropertyDefPossibleValues()) {
					pValues.add(dbPVal.getValue());
				}
				
				PropertyPossibleValueList ppvList = 
						new PropertyPossibleValueList(pValues, true);
				PropertyDefinition pDef = 
						new PropertyDefinition(
							dbPDef.getPropertyDefinitionId(),
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
		return tsDef;
	}

	public void importTagLibrary(InputStream inputStream) throws IOException {
		dbRepository.setTagManagerListenersEnabled(false);

		TagLibrarySerializationHandler tagLibrarySerializationHandler = 
				dbRepository.getSerializationHandlerFactory().getTagLibrarySerializationHandler();
		
		//emits events to the TagManager and is not suited for background loading
		final TagLibrary tagLibrary = 
				tagLibrarySerializationHandler.deserialize(null, inputStream);
		
		importTagLibrary(tagLibrary, null, true);
	}
		
	public void importTagLibrary(
			final TagLibrary tagLibrary, 
			final ExecutionListener<Session> executionListener, 
			final boolean independent) {
		
		dbRepository.getBackgroundServiceProvider().submit(
				"Importing Tag Library...",
				new DefaultProgressCallable<Pair<Session,Boolean>>() {
			public Pair<Session,Boolean> call() throws Exception {
				DBTagLibrary dbTagLibrary = 
						new DBTagLibrary(tagLibrary.getName(), independent);
				
				dbTagLibrary.setAuthor(
						tagLibrary.getContentInfoSet().getAuthor());
				dbTagLibrary.setDescription(
						tagLibrary.getContentInfoSet().getDescription());
				dbTagLibrary.setPublisher(
						tagLibrary.getContentInfoSet().getPublisher());
				
				DBUserTagLibrary dbUserTagLibrary = 
						new DBUserTagLibrary(dbRepository.getCurrentUser(), dbTagLibrary);
				dbTagLibrary.getDbUserTagLibraries().add(dbUserTagLibrary);
				
				Session session = dbRepository.getSessionFactory().openSession();
				try {
					session.beginTransaction();
					
					session.save(dbTagLibrary);
					
					for (TagsetDefinition tsDef : tagLibrary) {
						importTagsetDefinition(
								session, dbTagLibrary.getTagLibraryId(), 
								tsDef);
					}
					
					SQLQuery maintainTagDefHierarchyQuery = session.createSQLQuery(
							MAINTAIN_TAGDEFHIERARCHY);
					
					maintainTagDefHierarchyQuery.setInteger(
							"curTagLibraryId", dbTagLibrary.getTagLibraryId());
					
					maintainTagDefHierarchyQuery.executeUpdate();
					
					if (independent) {
						session.getTransaction().commit();
						CloseSafe.close(new CloseableSession(session));
					}
					
					tagLibrary.setId(dbTagLibrary.getId());
					
					return new Pair<Session,Boolean>(
							session,dbTagLibrary.isIndependent());
				}
				catch (Exception e) {
					CloseSafe.close(new CloseableSession(session,true));
					throw new IOException(e);
				}
			};
		}, 
		new ExecutionListener<Pair<Session, Boolean>>() {
			public void done(Pair<Session, Boolean> result) {
				
				dbRepository.setTagManagerListenersEnabled(true);
				
				if (result.getSecond()) {
					dbRepository.getPropertyChangeSupport().firePropertyChange(
							RepositoryChangeEvent.tagLibraryChanged.name(),
							null, 
							new TagLibraryReference(
									tagLibrary.getId(), 
									new ContentInfoSet(
											tagLibrary.getContentInfoSet().getAuthor(),
											tagLibrary.getContentInfoSet().getDescription(),
											tagLibrary.getContentInfoSet().getPublisher(),
											tagLibrary.getName())));
				}
				
				if (executionListener != null) {
					executionListener.done(result.getFirst());
				}
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

	private void importTagsetDefinition(
			Session session, Integer tagLibraryId, TagsetDefinition tsDef) {
		HashMap<String, DBTagDefinition> result = new HashMap<String, DBTagDefinition>();
		
		DBTagsetDefinition dbTagsetDefinition = 
				new DBTagsetDefinition(
						idGenerator.catmaIDToUUIDBytes(tsDef.getUuid()),
						tsDef.getVersion().getDate(),
						tsDef.getName(), tagLibraryId);
		for (TagDefinition tDef : tsDef) {
			DBTagDefinition dbTagDefinition = 
					createDBTagDefinition(tDef, dbTagsetDefinition);
			dbTagsetDefinition.getDbTagDefinitions().add(dbTagDefinition);
			result.put(tDef.getUuid(), dbTagDefinition);
		}
		
		session.save(dbTagsetDefinition);
		updateTagsetDefinitionIDs(tsDef, dbTagsetDefinition);
	}

	private void updateTagsetDefinitionIDs(TagsetDefinition tsDef,
			DBTagsetDefinition dbTagsetDefinition) {
		tsDef.setId(dbTagsetDefinition.getTagsetDefinitionId());
		
		for (DBTagDefinition dbTagDefinition : dbTagsetDefinition.getDbTagDefinitions()) {
			TagDefinition tDef = 
					tsDef.getTagDefinition(
							idGenerator.uuidBytesToCatmaID(dbTagDefinition.getUuid()));
			tDef.setId(dbTagDefinition.getTagDefinitionId());
			for (DBPropertyDefinition dbPropertyDefinition : dbTagDefinition.getDbPropertyDefinitions()) {
				PropertyDefinition pd = tDef.getPropertyDefinition(
						idGenerator.uuidBytesToCatmaID(dbPropertyDefinition.getUuid()));
				pd.setId(dbPropertyDefinition.getPropertyDefinitionId());
			}
			
		}
		for (DBTagDefinition dbTagDefinition : dbTagsetDefinition.getDbTagDefinitions()) {
			if (dbTagDefinition.getParentUuid() != null) {
				TagDefinition tDef = 
						tsDef.getTagDefinition(
								idGenerator.uuidBytesToCatmaID(dbTagDefinition.getUuid()));
				TagDefinition parentTagDef = 
						tsDef.getTagDefinition(
								idGenerator.uuidBytesToCatmaID(dbTagDefinition.getParentUuid()));
				tDef.setParentId(parentTagDef.getId());
			}
		}
	}

	private void addAuthorIfAbsent(TagDefinition tDef) {		
		if (tDef.getAuthor() == null) {
			PropertyDefinition pdAuthor = 
				new PropertyDefinition(
					null,
					idGenerator.generate(), 
					PropertyDefinition.SystemPropertyName.catma_markupauthor.name(), 
					new PropertyPossibleValueList(
						dbRepository.getCurrentUser().getIdentifier()));
			tDef.addSystemPropertyDefinition(pdAuthor);
		}
	}

	private DBTagDefinition createDBTagDefinition(
			TagDefinition tDef, 
			DBTagsetDefinition dbTagsetDefinition) {
		addAuthorIfAbsent(tDef);

		DBTagDefinition dbTagDefinition = 
				new DBTagDefinition(
					tDef.getVersion().getDate(),
					idGenerator.catmaIDToUUIDBytes(tDef.getUuid()),
					tDef.getName(),
					dbTagsetDefinition,
					idGenerator.catmaIDToUUIDBytes(tDef.getParentUuid()));
					
		for (PropertyDefinition pDef : tDef.getSystemPropertyDefinitions()) {
			DBPropertyDefinition dbPropertyDefinition = 
					createDBPropertyDefinition(
							pDef, dbTagDefinition, true);
			dbTagDefinition.getDbPropertyDefinitions().add(dbPropertyDefinition);
		}
		
		for (PropertyDefinition pDef : tDef.getUserDefinedPropertyDefinitions()) {
			DBPropertyDefinition dbPropertyDefinition = 
					createDBPropertyDefinition(
							pDef, dbTagDefinition, false);
			dbTagDefinition.getDbPropertyDefinitions().add(dbPropertyDefinition);
		}		
		return dbTagDefinition;
	}

	private DBPropertyDefinition createDBPropertyDefinition(
			PropertyDefinition pDef,
			DBTagDefinition dbTagDefinition, boolean isSystemProperty) {

		DBPropertyDefinition dbPropertyDefinition = 
				new DBPropertyDefinition(
					idGenerator.catmaIDToUUIDBytes(pDef.getUuid()),
					pDef.getName(),
					dbTagDefinition, isSystemProperty);
		
		for (String value : 
			pDef.getPossibleValueList().getPropertyValueList().getValues()) {
			
			DBPropertyDefPossibleValue dbPropertyDefPossibleValue =
					new DBPropertyDefPossibleValue(value, dbPropertyDefinition);
			dbPropertyDefinition.getDbPropertyDefPossibleValues().add(
					dbPropertyDefPossibleValue);
		}
		
		
		return dbPropertyDefinition;
	}

	void saveTagDefinition(final TagsetDefinition tagsetDefinition,
			final TagDefinition tagDefinition) {
		addAuthorIfAbsent(tagDefinition);
		
		dbRepository.getBackgroundServiceProvider().submit(
				"Saving Tag definition...",
		new DefaultProgressCallable<DBTagDefinition>() {
			public DBTagDefinition call() throws Exception {
				Session session = dbRepository.getSessionFactory().openSession();
				
				try {
					DBTagDefinition dbTagDefinition = 
						new DBTagDefinition(
							tagDefinition.getVersion().getDate(),
							idGenerator.catmaIDToUUIDBytes(
									tagDefinition.getUuid()),
							tagDefinition.getName(),
							(DBTagsetDefinition)session.load(
									DBTagsetDefinition.class, 
									tagsetDefinition.getId()),
							(tagDefinition.getParentUuid().isEmpty()? null :
								tagDefinition.getParentId()),
							idGenerator.catmaIDToUUIDBytes(
									tagDefinition.getParentUuid()));
					
					for (PropertyDefinition systemPropDef : 
						tagDefinition.getSystemPropertyDefinitions()) {
						
						DBPropertyDefinition dbColorDefinition = 
								new DBPropertyDefinition(
										idGenerator.catmaIDToUUIDBytes(
												systemPropDef.getUuid()),
												systemPropDef.getName(),
												dbTagDefinition,
												true);
						dbColorDefinition.setSingleValue(
								systemPropDef.getFirstValue());
						
						dbTagDefinition.getDbPropertyDefinitions().add(
								dbColorDefinition);
					}
					
					session.beginTransaction();
					session.save(dbTagDefinition);
					session.getTransaction().commit();
					
					CloseSafe.close(new CloseableSession(session));
					return dbTagDefinition;
				}
				catch (Exception e) {
					CloseSafe.close(new CloseableSession(session,true));
					throw new Exception(e);
				}
			}
		}, 
		new ExecutionListener<DBTagDefinition>() {
			public void done(DBTagDefinition result) {
				tagDefinition.setId(result.getTagDefinitionId());
				for (PropertyDefinition pd : 
					tagDefinition.getSystemPropertyDefinitions()) {
					
					DBPropertyDefinition dbPropertyDefinition = 
							result.getDbPropertyDefinition(pd.getUuid());
					pd.setId(dbPropertyDefinition.getPropertyDefinitionId());
				}
			}
			public void error(Throwable t) {
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.exceptionOccurred.name(),
						null, 
						t);	
			}
		});

	}

	void removeTagsetDefinition(final TagsetDefinition tagsetDefinition) {
		dbRepository.getBackgroundServiceProvider().submit(
				"Removing Tagset definition...",
			new DefaultProgressCallable<Void>() {
				public Void call() throws Exception {
					Session session = dbRepository.getSessionFactory().openSession();
					try {
						session.beginTransaction();
						
						DBTagsetDefinition dbTagsetDefinition = 
							(DBTagsetDefinition)session.get(
								DBTagsetDefinition.class, 
								tagsetDefinition.getId());
						
						session.delete(dbTagsetDefinition);
						session.getTransaction().commit();
						
						CloseSafe.close(new CloseableSession(session));
						return null;
					}
					catch (Exception e) {
						CloseSafe.close(new CloseableSession(session,true));
						throw new Exception(e);
					}
				}
			}, 
			new ExecutionListener<Void>() {
				public void done(Void nothing) {}
				public void error(Throwable t) {
					dbRepository.getPropertyChangeSupport().firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null, 
							t);	
				}
			});

	}

	void saveTagsetDefinition(final TagLibrary tagLibrary,
			final TagsetDefinition tagsetDefinition) {
		
		dbRepository.getBackgroundServiceProvider().submit(
				"Saving Tagset definition...",
		new DefaultProgressCallable<Void>() {
			public Void call() throws Exception {

				Session session = dbRepository.getSessionFactory().openSession();
				try {
					session.beginTransaction();
					importTagsetDefinition(
							session, Integer.valueOf(tagLibrary.getId()), 
							tagsetDefinition);
					
					SQLQuery maintainTagDefHierarchyQuery = session.createSQLQuery(
							MAINTAIN_TAGDEFHIERARCHY);
					
					maintainTagDefHierarchyQuery.setInteger(
							"curTagLibraryId", Integer.valueOf(tagLibrary.getId()));
					
					maintainTagDefHierarchyQuery.executeUpdate();
				
					session.getTransaction().commit();
					CloseSafe.close(new CloseableSession(session));
					return null;
				}
				catch (Exception e) {
					CloseSafe.close(new CloseableSession(session,true));
					throw new Exception(e);
				}
			}
		}, 
		new ExecutionListener<Void>() {
			public void done(Void nothing) { /* noop */ }
			public void error(Throwable t) {
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.exceptionOccurred.name(),
						null, 
						t);	
			}
		});
	}
	
	void updateTagsetDefinition(final TagsetDefinition tagsetDefinition) {
		dbRepository.getBackgroundServiceProvider().submit(
				"Updating Tagset definition",
			new DefaultProgressCallable<Void>() {
				public Void call() throws Exception {
					Session session = dbRepository.getSessionFactory().openSession();
					try {
						session.beginTransaction();
						DBTagsetDefinition dbTagsetDefinition = 
							(DBTagsetDefinition)session.load(
								DBTagsetDefinition.class, 
								tagsetDefinition.getId());

						dbTagsetDefinition.setName(
								tagsetDefinition.getName());
						dbTagsetDefinition.setVersion(
							tagsetDefinition.getVersion().getDate());
						
						session.update(dbTagsetDefinition);
						
						session.getTransaction().commit();
						
						CloseSafe.close(new CloseableSession(session));
						return null;
					}
					catch (Exception e) {
						CloseSafe.close(new CloseableSession(session,true));
						throw new Exception(e);
					}
				}
			}, 
			new ExecutionListener<Void>() {
				public void done(Void nothing) {}
				public void error(Throwable t) {
					dbRepository.getPropertyChangeSupport().firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null, 
							t);	
				}
			});
	}

	void delete(TagManager tagManager, TagLibraryReference tagLibraryReference) throws IOException {
		TagLibrary tagLibrary = getTagLibrary(tagLibraryReference);
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			
			for (TagsetDefinition tagsetDefinition : tagLibrary) {
				DBTagsetDefinition dbTagsetDefinition = 
					(DBTagsetDefinition)session.get(
						DBTagsetDefinition.class, 
						tagsetDefinition.getId());
				
				session.delete(dbTagsetDefinition);
			}
			DBTagLibrary dbTagLibrary = (DBTagLibrary)session.get(
					DBTagLibrary.class, Integer.valueOf(tagLibrary.getId()));
			session.delete(dbTagLibrary);
			
			session.getTransaction().commit();
			
			tagLibraryReferences.remove(tagLibraryReference);
			tagManager.removeTagLibrary(tagLibrary);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.tagLibraryChanged.name(),
					tagLibraryReference, null);	
			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}
	}

	void updateTagDefinition(final TagDefinition tagDefinition) {
		
		dbRepository.getBackgroundServiceProvider().submit(
				"Updating Tag definition...",
			new DefaultProgressCallable<Void>() {
				public Void call() throws Exception {
					Session session = dbRepository.getSessionFactory().openSession();
					try {
						session.beginTransaction();
						
						DBTagDefinition dbTagDefinition = 
							(DBTagDefinition)session.load(
								DBTagDefinition.class, 
								tagDefinition.getId());
						
						dbTagDefinition.setName(
								tagDefinition.getName());
						
						PropertyDefinition colorDefinition = 
							tagDefinition.getPropertyDefinitionByName(
								PropertyDefinition.SystemPropertyName.catma_displaycolor.name());
						DBPropertyDefinition dbColorDefinition =
								(DBPropertyDefinition)session.load(
										DBPropertyDefinition.class,
										colorDefinition.getId());
						dbColorDefinition.setSingleValue(
								tagDefinition.getColor());
						
						dbTagDefinition.setVersion(
							tagDefinition.getVersion().getDate());
						
						session.update(dbTagDefinition);
						
						session.getTransaction().commit();
						
						CloseSafe.close(new CloseableSession(session));
						return null;
					}
					catch (Exception e) {
						CloseSafe.close(new CloseableSession(session,true));
						throw new Exception(e);
					}
				}
			}, 
			new ExecutionListener<Void>() {
				public void done(Void nothing) {}
				public void error(Throwable t) {
					dbRepository.getPropertyChangeSupport().firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null, 
							t);	
				}
			});
	}

	void removeTagDefinition(final TagDefinition tagDefinition) {
		dbRepository.getBackgroundServiceProvider().submit(
				"Removing Tag definition",
			new DefaultProgressCallable<Void>() {
				public Void call() throws Exception {
					Session session = dbRepository.getSessionFactory().openSession();
					try {
						session.beginTransaction();
						
					
						DBTagDefinition dbTagDefinition = 
							(DBTagDefinition)session.get(
								DBTagDefinition.class, 
								tagDefinition.getId());
						
						session.delete(dbTagDefinition);
						session.getTransaction().commit();
						
						CloseSafe.close(new CloseableSession(session));
						return null;
					}
					catch (Exception e) {
						CloseSafe.close(new CloseableSession(session,true));
						throw new Exception(e);
					}
				}
			}, 
			new ExecutionListener<Void>() {
				public void done(Void nothing) {}
				public void error(Throwable t) {
					dbRepository.getPropertyChangeSupport().firePropertyChange(
							RepositoryChangeEvent.exceptionOccurred.name(),
							null, 
							t);	
				}
			});
	}

	void updateTagsetDefinition(Session session, TagLibrary tagLibrary,
			TagsetDefinition tagsetDefinition) {

		DBTagsetDefinition dbTagsetDefinition = 
				getDbTagsetDefinition(session, tagsetDefinition.getUuid(), tagLibrary.getId());
		
		if (dbTagsetDefinition != null) {
			//TODO: try to get along without getDeletedTagDefinitions()
			for (Integer id : tagsetDefinition.getDeletedTagDefinitions()) {
				DBTagDefinition dbTagDefinition = dbTagsetDefinition.getDbTagDefinition(id);
				//delete
				dbTagsetDefinition.getDbTagDefinitions().remove(dbTagDefinition);
				session.delete(dbTagDefinition);
			}

			updateDbTagsetDefinition(session,dbTagsetDefinition, tagsetDefinition);
		}
		else {
			dbTagsetDefinition = 
					createDbTagsetDefinition(tagsetDefinition, tagLibrary.getId());
		}
		session.saveOrUpdate(dbTagsetDefinition);
		
		SQLQuery maintainTagDefHierarchyQuery = session.createSQLQuery(
				MAINTAIN_TAGDEFHIERARCHY);
		
		maintainTagDefHierarchyQuery.setInteger(
				"curTagLibraryId", Integer.valueOf(tagLibrary.getId()));
		
		maintainTagDefHierarchyQuery.executeUpdate();
		updateTagsetDefinitionIDs(tagsetDefinition, dbTagsetDefinition);
	}
	

	DBTagsetDefinition getDbTagsetDefinition(Session session,
			String uuid, String tagLibraryId) {
		Criteria criteria = session.createCriteria(DBTagsetDefinition.class).add(
				Restrictions.and(
					Restrictions.eq("uuid", idGenerator.catmaIDToUUIDBytes(uuid)),
					Restrictions.eq("tagLibraryId", Integer.valueOf(tagLibraryId))));
		
		DBTagsetDefinition result = (DBTagsetDefinition) criteria.uniqueResult();
		
		return result;
	}

	private void updateDbTagsetDefinition(
			Session session, DBTagsetDefinition dbTagsetDefinition, TagsetDefinition tagsetDefinition) {
		dbTagsetDefinition.setName(
				tagsetDefinition.getName());
		dbTagsetDefinition.setVersion(
			tagsetDefinition.getVersion().getDate());
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			Integer id = tagDefinition.getId();
			if (id == null) {
				//create
				dbTagsetDefinition.getDbTagDefinitions().add(
						createDbTagDefinition(tagDefinition, dbTagsetDefinition));
			}
			else {
				//update
				updateDbTagDefinition(session,
					dbTagsetDefinition.getDbTagDefinition(id), tagDefinition);
			}
		}
	}

	private DBTagsetDefinition createDbTagsetDefinition(
			TagsetDefinition tagsetDefinition, String dbTagLibraryId) {

		DBTagsetDefinition dbTagsetDefinition = 
			new DBTagsetDefinition(
				idGenerator.catmaIDToUUIDBytes(tagsetDefinition.getUuid()), 
				tagsetDefinition.getVersion().getDate(), 
				tagsetDefinition.getName(),
				Integer.valueOf(dbTagLibraryId));
		
		for (TagDefinition td : tagsetDefinition) {
			dbTagsetDefinition.getDbTagDefinitions().add(
					createDbTagDefinition(td, dbTagsetDefinition));
		}
		return dbTagsetDefinition;
	}

	private void updateDbTagDefinition(Session session, DBTagDefinition dbTagDefinition,
			TagDefinition tagDefinition) {
		
		dbTagDefinition.setName(
				tagDefinition.getName());

		dbTagDefinition.setVersion(
				tagDefinition.getVersion().getDate());

		dbTagDefinition.setParentUuid(
				idGenerator.catmaIDToUUIDBytes(tagDefinition.getParentUuid()));

		for (Integer toBeDeletedId : tagDefinition.getDeletedPropertyDefinitions()) {
			DBPropertyDefinition toBeDeleted = 
					dbTagDefinition.getDbPropertyDefinition(toBeDeletedId);
			
			//delete
			dbTagDefinition.getDbPropertyDefinitions().remove(toBeDeleted);
			session.delete(toBeDeleted);
		}

		for (PropertyDefinition pd : tagDefinition.getSystemPropertyDefinitions()) {
			updatePropertyDefinition(session, pd, true, dbTagDefinition, tagDefinition);
		}
	}

	private void updatePropertyDefinition(Session session, PropertyDefinition pd, boolean isSystemPropertyDef,
			DBTagDefinition dbTagDefinition, TagDefinition tagDefinition) {
		
		Integer id = pd.getId();
		if (id == null) {
			//create
			dbTagDefinition.getDbPropertyDefinitions().add(
					createDbPropertyDefinition(pd, dbTagDefinition, isSystemPropertyDef));
		}
		else {
			// update
			updateDbPropertyDefinition(session,
				dbTagDefinition.getDbPropertyDefinition(pd.getUuid()), pd);
		}
	}

	DBTagDefinition createDbTagDefinition(TagDefinition tagDefinition,
			DBTagsetDefinition dbTagsetDefinition) {

		DBTagDefinition dbTagDefinition = 
			new DBTagDefinition(
				tagDefinition.getVersion().getDate(),
				idGenerator.catmaIDToUUIDBytes(tagDefinition.getUuid()),
				tagDefinition.getName(),
				dbTagsetDefinition,
				tagDefinition.getParentId(),
				idGenerator.catmaIDToUUIDBytes(tagDefinition.getParentUuid()));
		
		for (PropertyDefinition pd  : tagDefinition.getSystemPropertyDefinitions()) {
			dbTagDefinition.getDbPropertyDefinitions().add(
					createDbPropertyDefinition(pd, dbTagDefinition, true));
		}
		
		for (PropertyDefinition pd  : tagDefinition.getUserDefinedPropertyDefinitions()) {
			dbTagDefinition.getDbPropertyDefinitions().add(
					createDbPropertyDefinition(pd, dbTagDefinition, false));
		}
				
		return dbTagDefinition;
	}
	
	
	private void updateDbPropertyDefinition(
			Session session, DBPropertyDefinition dbPropertyDefinition, PropertyDefinition pd) {
		dbPropertyDefinition.setName(pd.getName());
		
		Iterator<DBPropertyDefPossibleValue> iterator = 
				dbPropertyDefinition.getDbPropertyDefPossibleValues().iterator();
		
		while (iterator.hasNext()) {
			DBPropertyDefPossibleValue currentValue = iterator.next();
			if (!pd.getPossibleValueList().getPropertyValueList().getValues().contains(
					currentValue.getValue())){
				iterator.remove();
				session.delete(currentValue);
			}
		}
		
		for (String value : pd.getPossibleValueList().getPropertyValueList().getValues()) {
			if (!dbPropertyDefinition.hasValue(value)) {
				dbPropertyDefinition.getDbPropertyDefPossibleValues().add(
					new DBPropertyDefPossibleValue(value, dbPropertyDefinition));
			}
		}
		
	}

	private DBPropertyDefinition createDbPropertyDefinition(
			PropertyDefinition pd, DBTagDefinition dbTagDefinition, boolean isSystemPropertyDef) {
		DBPropertyDefinition dbPropertyDefinition = 
				new DBPropertyDefinition(
						idGenerator.catmaIDToUUIDBytes(pd.getUuid()),
						pd.getName(),
						dbTagDefinition,
						isSystemPropertyDef);
		for (String value : pd.getPossibleValueList().getPropertyValueList().getValues()) {
			dbPropertyDefinition.getDbPropertyDefPossibleValues().add(
					new DBPropertyDefPossibleValue(value, dbPropertyDefinition));
		}
		
		return dbPropertyDefinition;
	}

	public void update(final TagLibraryReference tagLibraryReference) {
		ContentInfoSet contentInfoSet = tagLibraryReference.getContentInfoSet();
		final Integer tagLibraryId = 
				Integer.valueOf(tagLibraryReference.getId());
		final String author = contentInfoSet.getAuthor();
		final String publisher = contentInfoSet.getPublisher();
		final String title = contentInfoSet.getTitle();
		final String description = contentInfoSet.getDescription();
		
		dbRepository.getBackgroundServiceProvider().submit(
				"Updating Tag Library...",
				new DefaultProgressCallable<ContentInfoSet>() {
					public ContentInfoSet call() throws Exception {
						
						Session session = 
								dbRepository.getSessionFactory().openSession();
						try {
							DBTagLibrary dbTagLibrary =
									(DBTagLibrary) session.load(
									DBTagLibrary.class, 
									tagLibraryId);
							
							ContentInfoSet oldContentInfoSet =
									new ContentInfoSet(
											dbTagLibrary.getAuthor(), 
											dbTagLibrary.getDescription(), 
											dbTagLibrary.getPublisher(), 
											dbTagLibrary.getTitle());
							
							dbTagLibrary.setAuthor(author);
							dbTagLibrary.setTitle(title);
							dbTagLibrary.setDescription(description);
							dbTagLibrary.setPublisher(publisher);
							
							session.beginTransaction();
							session.save(dbTagLibrary);
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
						dbRepository.getPropertyChangeSupport().firePropertyChange(
							RepositoryChangeEvent.tagLibraryChanged.name(),
							oldContentInfoSet, tagLibraryReference);
					}
					public void error(Throwable t) {
						t.printStackTrace();
						// TODO Auto-generated method stub
						
					}
				}
			);;
		}


	void close() {
		dbRepository.setTagManagerListenersEnabled(false);
		
		for (TagLibraryReference tagLibraryReference : tagLibraryReferences) {
			dbRepository.getTagManager().removeTagLibrary(tagLibraryReference);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.tagLibraryChanged.name(),
					tagLibraryReference, null);	
		}
	}
}
