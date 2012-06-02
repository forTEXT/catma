package de.catma.repository.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.repository.db.model.DBPropertyDefPossibleValue;
import de.catma.repository.db.model.DBPropertyDefinition;
import de.catma.repository.db.model.DBTagDefinition;
import de.catma.repository.db.model.DBTagLibrary;
import de.catma.repository.db.model.DBTagsetDefinition;
import de.catma.repository.db.model.DBUserTagLibrary;
import de.catma.serialization.TagLibrarySerializationHandler;
import de.catma.tag.ITagLibrary;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyPossibleValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;
import de.catma.util.CloseSafe;
import de.catma.util.IDGenerator;

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
	private Map<String,DBTagLibrary> tagLibrariesByID;
	private Set<TagLibraryReference> tagLibraryReferences;
	private IDGenerator idGenerator;

	public DBTagLibraryHandler(DBRepository dbRepository, IDGenerator idGenerator) {
		this.dbRepository = dbRepository;
		this.idGenerator = idGenerator;
		tagLibrariesByID = new HashMap<String, DBTagLibrary>();
		tagLibraryReferences = new HashSet<TagLibraryReference>();
	}

	public void createTagLibrary(String name) throws IOException {

		DBTagLibrary tagLibrary = new DBTagLibrary(name, true);
		DBUserTagLibrary dbUserTagLibrary = 
				new DBUserTagLibrary(dbRepository.getCurrentUser(), tagLibrary);
		tagLibrary.getDbUserTagLibraries().add(dbUserTagLibrary);
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			
			session.save(tagLibrary);
			
			session.getTransaction().commit();
			tagLibrariesByID.put(tagLibrary.getId(), tagLibrary);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.tagLibraryChanged.name(),
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
			
			for (DBTagLibrary tagLibrary : (List<DBTagLibrary>)query.list()) {
				this.tagLibrariesByID.put(tagLibrary.getId(), tagLibrary);
				this.tagLibraryReferences.add(
					new TagLibraryReference(
							tagLibrary.getId(), tagLibrary.getName()));
			}
		}
		
	}
	
	Set<TagLibraryReference> getTagLibraryReferences() {
		return Collections.unmodifiableSet(this.tagLibraryReferences);
	}
	
	ITagLibrary getTagLibrary(TagLibraryReference tagLibraryReference) 
			throws IOException{
		
		DBTagLibrary tagLibrary = 
				this.tagLibrariesByID.get(tagLibraryReference.getId());
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			loadTagLibrayContent(session, tagLibrary);
		}
		finally {
			CloseSafe.close(new ClosableSession(session));
		}

		
		return tagLibrary;
	}
	
	@SuppressWarnings("unchecked")
	void loadTagLibrayContent(Session session, DBTagLibrary tagLibrary) throws IOException {

		Query query = session.createQuery(
				"select distinct tsd from "
				+ DBTagsetDefinition.class.getSimpleName() + " as tsd "
				+ "left join fetch tsd.dbTagDefinitions as td "
				+ "left join fetch td.dbPropertyDefinitions as pd "
				+ "left join fetch pd.dbPropertyDefPossibleValues "
				+ "where tsd.tagLibraryId = " + tagLibrary.getTagLibraryId());
		List<DBTagsetDefinition> dbTagsetDefinitions = (List<DBTagsetDefinition>)query.list();
		
		for (DBTagsetDefinition dbTsDef : dbTagsetDefinitions) {
			tagLibrary.add(createTagsetDefinition(dbTsDef));
		}
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
		final ITagLibrary tagLibrary = 
				tagLibrarySerializationHandler.deserialize(null, inputStream);
		
		importTagLibrary(tagLibrary, null, true);
	}
		
	public void importTagLibrary(
			final ITagLibrary tagLibrary, 
			final ExecutionListener<TagLibraryImportResult> executionListener, 
			final boolean independent) {
		
		dbRepository.getBackgroundServiceProvider().submit(
				new DefaultProgressCallable<TagLibraryImportResult>() {
			public TagLibraryImportResult call() throws Exception {
				DBTagLibrary dbTagLibrary = new DBTagLibrary(tagLibrary, independent);
				DBUserTagLibrary dbUserTagLibrary = 
						new DBUserTagLibrary(dbRepository.getCurrentUser(), dbTagLibrary);
				dbTagLibrary.getDbUserTagLibraries().add(dbUserTagLibrary);
				
				Session session = dbRepository.getSessionFactory().openSession();
				try {
					session.beginTransaction();
					
					session.save(dbTagLibrary);

					HashMap<String, DBTagDefinition> dbTagDefs = 
							new HashMap<String, DBTagDefinition>();
					
					for (TagsetDefinition tsDef : dbTagLibrary) {
						dbTagDefs.putAll(
							importTagsetDefinition(
									session, dbTagLibrary.getTagLibraryId(), 
									tsDef));
					}
					
					SQLQuery maintainTagDefHierarchyQuery = session.createSQLQuery(
							MAINTAIN_TAGDEFHIERARCHY);
					
					maintainTagDefHierarchyQuery.setInteger(
							"curTagLibraryId", dbTagLibrary.getTagLibraryId());
					
					maintainTagDefHierarchyQuery.executeUpdate();
					
					if (independent) {
						session.getTransaction().commit();
						CloseSafe.close(new ClosableSession(session));
					}
					
					return new TagLibraryImportResult(session,dbTagLibrary, dbTagDefs);
				}
				catch (Exception e) {
					try {
						if (session.getTransaction().isActive()) {
							session.getTransaction().rollback();
						}
					}
					catch(Exception notOfInterest){}
					CloseSafe.close(new ClosableSession(session));
					throw new IOException(e);
				}
			};
		}, 
		new ExecutionListener<TagLibraryImportResult>() {
			public void done(TagLibraryImportResult result) {
				tagLibrariesByID.put(
						result.getDbTagLibrary().getId(), 
						result.getDbTagLibrary());

				dbRepository.setTagManagerListenersEnabled(true);
				
				if (result.getDbTagLibrary().isIndependent()) {
					dbRepository.getPropertyChangeSupport().firePropertyChange(
							RepositoryChangeEvent.tagLibraryChanged.name(),
							null, 
							new TagLibraryReference(
									result.getDbTagLibrary().getId(), 
									result.getDbTagLibrary().getName()));
				}
				
				if (executionListener != null) {
					executionListener.done(result);
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

	private Map<String, DBTagDefinition> importTagsetDefinition(
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
		
		return result;
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
					
					return dbTagDefinition;
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
						
						return null;
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

	void saveTagsetDefinition(final ITagLibrary tagLibrary,
			final TagsetDefinition tagsetDefinition) {
		
		dbRepository.getBackgroundServiceProvider().submit(
		new DefaultProgressCallable<DBTagsetDefinition>() {
			public DBTagsetDefinition call() throws Exception {
				DBTagsetDefinition dbTagsetDefinition = 
					new DBTagsetDefinition(
						idGenerator.catmaIDToUUIDBytes(tagsetDefinition.getUuid()),
						tagsetDefinition.getVersion().getDate(),
						tagsetDefinition.getName(),
						((DBTagLibrary)tagLibrary).getTagLibraryId());
				
				Session session = dbRepository.getSessionFactory().openSession();
				
				try {
					session.beginTransaction();
					session.save(dbTagsetDefinition);
					session.getTransaction().commit();
					return dbTagsetDefinition;
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
		}, 
		new ExecutionListener<DBTagsetDefinition>() {
			public void done(DBTagsetDefinition result) {
				tagsetDefinition.setId(result.getTagsetDefinitionId());
			}
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
						
						return null;
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
		ITagLibrary tagLibrary = getTagLibrary(tagLibraryReference);
		
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
			
			session.delete(tagLibrary);
			
			session.getTransaction().commit();
			
			tagLibrariesByID.remove(tagLibrary.getId());
			tagLibraryReferences.remove(tagLibraryReference);
			tagManager.removeTagLibrary(tagLibrary);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.tagLibraryChanged.name(),
					tagLibraryReference, null);	
			
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

	void updateTagDefinition(final TagDefinition tagDefinition) {
		
		dbRepository.getBackgroundServiceProvider().submit(
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
						
						return null;
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
						
						return null;
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

	void updateTagsetDefinition(Session session, ITagLibrary tagLibrary,
			TagsetDefinition tagsetDefinition) {

		DBTagLibrary dbTagLibrary = (DBTagLibrary)tagLibrary;
		DBTagsetDefinition dbTagsetDefinition = 
				dbTagLibrary.getDbTagsetDefinition(tagsetDefinition.getUuid());
		
		if (dbTagsetDefinition != null) {
			updateDbTagsetDefinition(dbTagsetDefinition, tagsetDefinition);
			session.saveOrUpdate(dbTagsetDefinition);
		}
		else {
			dbTagsetDefinition = 
					createDbTagsetDefinition(tagsetDefinition, dbTagLibrary);
			session.saveOrUpdate(dbTagsetDefinition);
		}
		
		SQLQuery maintainTagDefHierarchyQuery = session.createSQLQuery(
				MAINTAIN_TAGDEFHIERARCHY);
		
		maintainTagDefHierarchyQuery.setInteger(
				"curTagLibraryId", dbTagLibrary.getTagLibraryId());
		
		maintainTagDefHierarchyQuery.executeUpdate();
		tagLibrary.remove(tagsetDefinition);
		
		tagLibrary.add(createTagsetDefinition(dbTagsetDefinition));
	}
	

	private void updateDbTagsetDefinition(
			DBTagsetDefinition dbTagsetDefinition, TagsetDefinition tagsetDefinition) {
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
			else if (tagsetDefinition.getDeletedTagDefinitions().contains(id)){
				//delete
				dbTagsetDefinition.getDbTagDefinitions().remove(
					dbTagsetDefinition.getDbTagDefinition(id));
			}
			else {
				//update
				updateDbTagDefinition(
					dbTagsetDefinition.getDbTagDefinition(id), tagDefinition);
			}
		}
	}

	private DBTagsetDefinition createDbTagsetDefinition(
			TagsetDefinition tagsetDefinition, DBTagLibrary dbTagLibrary) {

		DBTagsetDefinition dbTagsetDefinition = 
			new DBTagsetDefinition(
				idGenerator.catmaIDToUUIDBytes(tagsetDefinition.getUuid()), 
				tagsetDefinition.getVersion().getDate(), 
				tagsetDefinition.getName(),
				dbTagLibrary.getTagLibraryId());
		
		for (TagDefinition td : tagsetDefinition) {
			dbTagsetDefinition.getDbTagDefinitions().add(
					createDbTagDefinition(td, dbTagsetDefinition));
		}
		return dbTagsetDefinition;
	}

	private void updateDbTagDefinition(DBTagDefinition dbTagDefinition,
			TagDefinition tagDefinition) {
		
		dbTagDefinition.setName(
				tagDefinition.getName());

		dbTagDefinition.setVersion(
				tagDefinition.getVersion().getDate());

		dbTagDefinition.setParentUuid(
				idGenerator.catmaIDToUUIDBytes(tagDefinition.getParentUuid()));

		for (PropertyDefinition pd : tagDefinition.getSystemPropertyDefinitions()) {
			updatePropertyDefinition(pd, true, dbTagDefinition, tagDefinition);
		}
		
	}

	private void updatePropertyDefinition(
			PropertyDefinition pd, boolean isSystemPropertyDef,
			DBTagDefinition dbTagDefinition, TagDefinition tagDefinition) {
		
		Integer id = pd.getId();
		if (id == null) {
			//create
			dbTagDefinition.getDbPropertyDefinitions().add(
					createDbPropertyDefinition(pd, dbTagDefinition, isSystemPropertyDef));
		}
		else if (tagDefinition.getDeletedPropertyDefinitions().contains(id)) {
			//delete
			dbTagDefinition.getDbPropertyDefinitions().remove(
				dbTagDefinition.getDbPropertyDefinition(pd.getUuid()));
		}
		else {
			// update
			updateDbPropertyDefinition(
				dbTagDefinition.getDbPropertyDefinition(pd.getUuid()), pd);
		}
	}

	private DBTagDefinition createDbTagDefinition(TagDefinition tagDefinition,
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
			DBPropertyDefinition dbPropertyDefinition, PropertyDefinition pd) {
		dbPropertyDefinition.setName(pd.getName());
		
		Iterator<DBPropertyDefPossibleValue> iterator = 
				dbPropertyDefinition.getDbPropertyDefPossibleValues().iterator();
		
		while (iterator.hasNext()) {
			DBPropertyDefPossibleValue currentValue = iterator.next();
			if (!pd.getPossibleValueList().getPropertyValueList().getValues().contains(
					currentValue.getValue())){
				iterator.remove();
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


}
