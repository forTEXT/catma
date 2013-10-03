/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.repository.db;

import static de.catma.repository.db.jooq.catmarepository.Tables.PROPERTYDEFINITION;
import static de.catma.repository.db.jooq.catmarepository.Tables.PROPERTYDEF_POSSIBLEVALUE;
import static de.catma.repository.db.jooq.catmarepository.Tables.TAGDEFINITION;
import static de.catma.repository.db.jooq.catmarepository.Tables.TAGLIBRARY;
import static de.catma.repository.db.jooq.catmarepository.Tables.TAGSETDEFINITION;
import static de.catma.repository.db.jooq.catmarepository.Tables.USER_TAGLIBRARY;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.db.CloseableSession;
import de.catma.document.repository.AccessMode;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.ContentInfoSet;
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
import de.catma.util.Collections3;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

class DBTagLibraryHandler {
	
	private static class TagLibRefNameComparator implements Comparator<TagLibraryReference> {
		public int compare(TagLibraryReference o1, TagLibraryReference o2) {
			return o1.toString().compareToIgnoreCase(o2.toString());
		}
	}
	
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
	private HashMap<String, TagLibraryReference> tagLibraryReferencesById;
	private IDGenerator idGenerator;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private DataSource dataSource;

	public DBTagLibraryHandler(
		DBRepository dbRepository, IDGenerator idGenerator) throws NamingException {
		this.dbRepository = dbRepository;
		this.idGenerator = idGenerator;
		tagLibraryReferencesById = new HashMap<String, TagLibraryReference>();
		Context  context = new InitialContext();
		this.dataSource = (DataSource) context.lookup("catmads");
	}

	public void createTagLibrary(String name) throws IOException {
		
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			db.beginTransaction();
			
			Integer tagLibraryId = db
			.insertInto(
				TAGLIBRARY,
					TAGLIBRARY.TITLE)
			.values(name)
			.returning(TAGLIBRARY.TAGLIBRARYID)
			.fetchOne()
			.map(new IDFieldToIntegerMapper(TAGLIBRARY.TAGLIBRARYID));
					
			db
			.insertInto(
				USER_TAGLIBRARY,
					USER_TAGLIBRARY.USERID,
					USER_TAGLIBRARY.TAGLIBRARYID,
					USER_TAGLIBRARY.ACCESSMODE,
					USER_TAGLIBRARY.OWNER)
			.values(
				dbRepository.getCurrentUser().getUserId(),
				tagLibraryId,
				AccessMode.WRITE.getNumericRepresentation(),
				(byte)1)
			.execute();

			db.commitTransaction();
			
			TagLibraryReference ref = new TagLibraryReference(
					String.valueOf(tagLibraryId), new ContentInfoSet(name));
			tagLibraryReferencesById.put(ref.getId(), ref);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.tagLibraryChanged.name(),
					null, 
					ref);

		}
		catch (DataAccessException dae) {
			db.rollbackTransaction();
			db.close();
			throw new IOException(dae);
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	}
	
	void loadTagLibraryReferences(DSLContext db) {
		this.tagLibraryReferencesById = getTagLibraryReferences(db);
	}

	private HashMap<String, TagLibraryReference> getTagLibraryReferences(
			DSLContext db) {
		
		List<TagLibraryReference> tagLibReferences = db
		.select()
		.from(TAGLIBRARY)
		.join(USER_TAGLIBRARY)
			.on(USER_TAGLIBRARY.TAGLIBRARYID.eq(TAGLIBRARY.TAGLIBRARYID)
			.and(USER_TAGLIBRARY.USERID.eq(dbRepository.getCurrentUser().getUserId())))
		.where(TAGLIBRARY.INDEPENDENT.eq((byte)1))
		.fetch()
		.map(new TagLibraryReferenceMapper());
		
		HashMap<String, TagLibraryReference> result = 
				new HashMap<String, TagLibraryReference>();
		
		for (TagLibraryReference ref : tagLibReferences) {
			result.put(ref.getId(), ref);
		}
		
		return result;
	}

	List<TagLibraryReference> getTagLibraryReferences() {
		List<TagLibraryReference> orderedList = new ArrayList<TagLibraryReference>();
		orderedList.addAll(this.tagLibraryReferencesById.values());
		Collections.sort(orderedList, new TagLibRefNameComparator());
		return Collections.unmodifiableList(orderedList);
	}
	
	TagLibrary getTagLibrary(TagLibraryReference tagLibraryReference) 
			throws IOException{
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		
		TagLibrary tagLibrary = 
				loadTagLibrayContent(db, tagLibraryReference);
		return tagLibrary;
	}

	TagLibrary loadTagLibrayContent(DSLContext db, TagLibraryReference reference) throws IOException {
		
		TagLibrary tagLibrary = new TagLibrary(reference.getId(), reference.toString());
		
		int tagLibraryId = Integer.valueOf(reference.getId());
		//HIER GEHTS WEITER
		
		Map<byte[], Result<Record>> possValuesByDefUuid = db
		.select(Collections3.getUnion(
				PROPERTYDEF_POSSIBLEVALUE.fields(), 
				PROPERTYDEFINITION.fields()))
		.from(PROPERTYDEF_POSSIBLEVALUE)
		.join(PROPERTYDEFINITION)
			.on(PROPERTYDEFINITION.PROPERTYDEFINITIONID.eq(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID))
		.join(TAGDEFINITION)
			.on(TAGDEFINITION.TAGDEFINITIONID.eq(PROPERTYDEFINITION.TAGDEFINITIONID))
		.join(TAGSETDEFINITION)
			.on(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(TAGDEFINITION.TAGSETDEFINITIONID))
			.and(TAGSETDEFINITION.TAGLIBRARYID.eq(tagLibraryId))
		.fetchGroups(PROPERTYDEFINITION.UUID);
		
		Map<byte[], Result<Record>> propertyDefByTagDefUuid = db
		.select(Collections3.getUnion(PROPERTYDEFINITION.fields(), TAGDEFINITION.fields()))
		.from(PROPERTYDEFINITION)
		.join(TAGDEFINITION)
			.on(TAGDEFINITION.TAGDEFINITIONID.eq(PROPERTYDEFINITION.TAGDEFINITIONID))
		.join(TAGSETDEFINITION)
			.on(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(TAGDEFINITION.TAGSETDEFINITIONID))
			.and(TAGSETDEFINITION.TAGLIBRARYID.eq(tagLibraryId))
		.fetchGroups(TAGDEFINITION.UUID);
	
		Map<byte[], Result<Record>> tagDefByTagsetDefUuid = db
		.select()
		.from(TAGDEFINITION)
		.join(TAGSETDEFINITION)
			.on(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(TAGDEFINITION.TAGSETDEFINITIONID))
			.and(TAGSETDEFINITION.TAGLIBRARYID.eq(tagLibraryId))
		.fetchGroups(TAGSETDEFINITION.UUID);
		
		List<TagsetDefinition> tagsetDefinitions = db
		.select(TAGSETDEFINITION.fields())
		.from(TAGSETDEFINITION)
		.where(TAGSETDEFINITION.TAGLIBRARYID.eq(tagLibraryId))
		.fetch()
		.map(new TagsetDefinitionMapper(
			tagDefByTagsetDefUuid, 
			propertyDefByTagDefUuid, 
			possValuesByDefUuid));
		
		for (TagsetDefinition tagsetDef : tagsetDefinitions) {
			tagLibrary.add(tagsetDef);
		}
		
		return tagLibrary;
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
		
	
	// HIER GEHTS WEITER
	
	private void importTagLibrary(
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
						logger.info("importing TagsetDefinition:" + tsDef);
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
					tDef.getParentId(),
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
					DBTagsetDefinition dbTagsetDefinition = 
							(DBTagsetDefinition)session.load(
									DBTagsetDefinition.class, 
									tagsetDefinition.getId());
					getLibraryAccess(
						session, dbTagsetDefinition.getTagLibraryId(), true);
					
					DBTagDefinition dbTagDefinition = 
						new DBTagDefinition(
							tagDefinition.getVersion().getDate(),
							idGenerator.catmaIDToUUIDBytes(
									tagDefinition.getUuid()),
							tagDefinition.getName(),
							dbTagsetDefinition,
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
						getLibraryAccess(
							session, dbTagsetDefinition.getTagLibraryId(), true);
							
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
					
					getLibraryAccess(session, tagLibrary.getId(), true);
					
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
	
	private AccessMode getLibraryAccess(
			DSLContext db, String tagLibraryId, boolean checkWriteAccess) throws IOException {
		return getLibraryAccess(db, Integer.valueOf(tagLibraryId), checkWriteAccess);
	}
	
	AccessMode getLibraryAccess(
			DSLContext db, int tagLibraryId, boolean checkWriteAccess) throws IOException {
	
		Record accessModeRecord = db
			.select(TAGLIBRARY.INDEPENDENT, USER_TAGLIBRARY.ACCESSMODE)
			.from(TAGLIBRARY)
			.leftOuterJoin(USER_TAGLIBRARY)
				.on(USER_TAGLIBRARY.TAGLIBRARYID.eq(tagLibraryId))
				.and(USER_TAGLIBRARY.USERID.eq(dbRepository.getCurrentUser().getUserId()))
			.where(TAGLIBRARY.TAGLIBRARYID.eq(tagLibraryId))
			.fetchOne();
		
		Integer existingAccessModeValue = 
				accessModeRecord.getValue(USER_TAGLIBRARY.ACCESSMODE);
		Boolean independent = 
				accessModeRecord.getValue(TAGLIBRARY.INDEPENDENT, Boolean.class);
		
		if (independent && (existingAccessModeValue == null)) {
			throw new IOException(
					"You seem to have no access to this library! " +
					"Please reload the repository!");
		}
		else if (checkWriteAccess && independent && 
					(existingAccessModeValue 
							!= AccessMode.WRITE.getNumericRepresentation())) {
			throw new IOException(
					"You seem to have no write access to this library! " +
					"Please reload your Tag Library using the Tag Manager!");
		}
		else {
			return AccessMode.getAccessMode(existingAccessModeValue);
		}
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

						getLibraryAccess(session,
								dbTagsetDefinition.getTagLibraryId(),
								true);
						
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
			
			Pair<DBTagLibrary, DBUserTagLibrary> libraryAccess =   
					getLibraryAccess(session, tagLibrary.getId(), true);

			Set<DBUserTagLibrary> dbUserTagLibraries = 
					libraryAccess.getFirst().getDbUserTagLibraries();
			
			DBUserTagLibrary currentUserTagLibrary = libraryAccess.getSecond();
			
			if (currentUserTagLibrary.isOwner() 
					&& (dbUserTagLibraries.size() == 1)) {
				
				session.delete(currentUserTagLibrary);
				
				for (TagsetDefinition tagsetDefinition : tagLibrary) {
					DBTagsetDefinition dbTagsetDefinition = 
						(DBTagsetDefinition)session.get(
							DBTagsetDefinition.class, 
							tagsetDefinition.getId());
					
					session.delete(dbTagsetDefinition);
				}
				session.delete(libraryAccess.getFirst());
			}
			else {
				session.delete(currentUserTagLibrary);
			}
			
			session.getTransaction().commit();
			
			tagLibraryReferencesById.remove(tagLibraryReference);
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

						getLibraryAccess(
							session,
							dbTagDefinition.getDbTagsetDefinition().getTagLibraryId(),
							true);

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
						
						getLibraryAccess(
							session, 
							dbTagDefinition.getDbTagsetDefinition().getTagLibraryId(),
							true);
						
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

	Set<byte[]> updateTagsetDefinition(DSLContext db, TagLibrary tagLibrary,
			TagsetDefinition tagsetDefinition) {
		Set<byte[]> deletedUuids = new HashSet<byte[]>();
		
		DBTagsetDefinition dbTagsetDefinition = 
				getDBTagsetDefinition(session, tagsetDefinition.getUuid(), tagLibrary.getId());
		
		if (dbTagsetDefinition != null) {
			//TODO: try to get along without getDeletedTagDefinitions()
			for (Integer id : tagsetDefinition.getDeletedTagDefinitions()) {
				DBTagDefinition dbTagDefinition = dbTagsetDefinition.getDbTagDefinition(id);
				deletedUuids.add(dbTagDefinition.getUuid());
				//delete
				dbTagsetDefinition.getDbTagDefinitions().remove(dbTagDefinition);
				session.delete(dbTagDefinition);
			}
			tagsetDefinition.getDeletedTagDefinitions().clear();

			updateDBTagsetDefinition(session,dbTagsetDefinition, tagsetDefinition);
		}
		else {
			dbTagsetDefinition = 
					createDBTagsetDefinition(tagsetDefinition, tagLibrary.getId());
		}
		session.saveOrUpdate(dbTagsetDefinition);
		
		SQLQuery maintainTagDefHierarchyQuery = session.createSQLQuery(
				MAINTAIN_TAGDEFHIERARCHY);
		
		maintainTagDefHierarchyQuery.setInteger(
				"curTagLibraryId", Integer.valueOf(tagLibrary.getId()));
		
		maintainTagDefHierarchyQuery.executeUpdate();
		updateTagsetDefinitionIDs(tagsetDefinition, dbTagsetDefinition);
		
		return deletedUuids;
	}
	

	DBTagsetDefinition getDBTagsetDefinition(Session session,
			String uuid, String tagLibraryId) {
		Criteria criteria = session.createCriteria(DBTagsetDefinition.class).add(
				Restrictions.and(
					Restrictions.eq("uuid", idGenerator.catmaIDToUUIDBytes(uuid)),
					Restrictions.eq("tagLibraryId", Integer.valueOf(tagLibraryId))));
		
		DBTagsetDefinition result = (DBTagsetDefinition) criteria.uniqueResult();
		
		return result;
	}

	private void updateDBTagsetDefinition(
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
						createDBTagDefinition(tagDefinition, dbTagsetDefinition));
			}
			else {
				//update
				updateDBTagDefinition(session,
					dbTagsetDefinition.getDbTagDefinition(id), tagDefinition);
			}
		}
	}

	private DBTagsetDefinition createDBTagsetDefinition(
			TagsetDefinition tagsetDefinition, String dbTagLibraryId) {

		DBTagsetDefinition dbTagsetDefinition = 
			new DBTagsetDefinition(
				idGenerator.catmaIDToUUIDBytes(tagsetDefinition.getUuid()), 
				tagsetDefinition.getVersion().getDate(), 
				tagsetDefinition.getName(),
				Integer.valueOf(dbTagLibraryId));
		
		for (TagDefinition td : tagsetDefinition) {
			dbTagsetDefinition.getDbTagDefinitions().add(
					createDBTagDefinition(td, dbTagsetDefinition));
		}
		return dbTagsetDefinition;
	}

	private void updateDBTagDefinition(Session session, DBTagDefinition dbTagDefinition,
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
		
		for (PropertyDefinition pd : tagDefinition.getUserDefinedPropertyDefinitions()) {
			updatePropertyDefinition(session, pd, false, dbTagDefinition, tagDefinition);
		}
	}

	private void updatePropertyDefinition(Session session, PropertyDefinition pd, boolean isSystemPropertyDef,
			DBTagDefinition dbTagDefinition, TagDefinition tagDefinition) {
		
		Integer id = pd.getId();
		if (id == null) {
			//create
			dbTagDefinition.getDbPropertyDefinitions().add(
					createDBPropertyDefinition(pd, dbTagDefinition, isSystemPropertyDef));
		}
		else {
			logger.info("updating " + pd + " in " + tagDefinition);
			// update
			updateDBPropertyDefinition(session,
				dbTagDefinition.getDbPropertyDefinition(pd.getUuid()), pd);
		}
	}

	private void updateDBPropertyDefinition(
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
							Pair<DBTagLibrary, DBUserTagLibrary> libraryAccess = 
									getLibraryAccess(
										session, tagLibraryId, true);
							DBTagLibrary dbTagLibrary = libraryAccess.getFirst();
							
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
						dbRepository.getPropertyChangeSupport().firePropertyChange(
								RepositoryChangeEvent.exceptionOccurred.name(),
								null, t);
					}
				}
			);;
		}


	void close() {
		dbRepository.setTagManagerListenersEnabled(false);
		
		for (Map.Entry<String,TagLibraryReference> entry : tagLibraryReferencesById.entrySet()) {
			TagLibraryReference  tagLibraryReference = entry.getValue();
			
			dbRepository.getTagManager().removeTagLibrary(tagLibraryReference);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.tagLibraryChanged.name(),
					tagLibraryReference, null);	
		}
	}
	
	void savePropertyDefinition(final PropertyDefinition pd, final TagDefinition td) {
		dbRepository.getBackgroundServiceProvider().submit(
				"Saving Property definition...",
		new DefaultProgressCallable<DBPropertyDefinition>() {
			public DBPropertyDefinition call() throws Exception {
				Session session = dbRepository.getSessionFactory().openSession();
				
				try {
					DBTagDefinition dbTagDefinition = 
							(DBTagDefinition)session.get(DBTagDefinition.class, td.getId());
					DBTagsetDefinition dbTagsetDefinition = 
							dbTagDefinition.getDbTagsetDefinition();
					
					getLibraryAccess(
						session, dbTagsetDefinition.getTagLibraryId(), true);
					
					DBPropertyDefinition dbPropertyDefinition = new
						DBPropertyDefinition(
							idGenerator.catmaIDToUUIDBytes(pd.getUuid()), 
							pd.getName(), 
							dbTagDefinition,
							false);
					for (String value : pd.getPossibleValueList().getPropertyValueList().getValues()) {
						dbPropertyDefinition.getDbPropertyDefPossibleValues().add(
								new DBPropertyDefPossibleValue(value, dbPropertyDefinition));
					}
					dbPropertyDefinition.getDbTagDefinition().setVersion(td.getVersion().getDate());
					session.beginTransaction();
					session.save(dbPropertyDefinition);
					session.getTransaction().commit();
					
					CloseSafe.close(new CloseableSession(session));
					return dbPropertyDefinition;
				}
				catch (Exception e) {
					CloseSafe.close(new CloseableSession(session,true));
					throw new Exception(e);
				}
			}
		}, 
		new ExecutionListener<DBPropertyDefinition>() {
			public void done(DBPropertyDefinition result) {
				pd.setId(result.getPropertyDefinitionId());
			}
			public void error(Throwable t) {
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.exceptionOccurred.name(),
						null, 
						t);	
			}
		});
	}

	void updatePropertyDefinition(
			final PropertyDefinition pd, final TagDefinition td) {
		dbRepository.getBackgroundServiceProvider().submit(
				"Saving Property definition...",
		new DefaultProgressCallable<DBPropertyDefinition>() {
			public DBPropertyDefinition call() throws Exception {
				Session session = dbRepository.getSessionFactory().openSession();
				
				try {
					
					DBPropertyDefinition dbPropertyDefinition = 
							(DBPropertyDefinition)session.get(DBPropertyDefinition.class, pd.getId());
					
					getLibraryAccess(
						session,
						dbPropertyDefinition.getDbTagDefinition().getDbTagsetDefinition().getTagLibraryId(),
						true);
					
					Iterator<DBPropertyDefPossibleValue> iterator =
							dbPropertyDefinition.getDbPropertyDefPossibleValues().iterator();
					dbPropertyDefinition.setName(pd.getName());
					dbPropertyDefinition.getDbTagDefinition().setVersion(
							td.getVersion().getDate());
					session.beginTransaction();
					
					while (iterator.hasNext()) {
						DBPropertyDefPossibleValue val = iterator.next();
						if (!pd.getPossibleValueList().getPropertyValueList().getValues().contains(
								val.getValue())) {
							iterator.remove();
							session.delete(val);
						}
					}
					
					for (String value : pd.getPossibleValueList().getPropertyValueList().getValues()) {
						if (!dbPropertyDefinition.hasValue(value)) {
							dbPropertyDefinition.getDbPropertyDefPossibleValues().add(
									new DBPropertyDefPossibleValue(value, dbPropertyDefinition));
						}
					}
					
					session.save(dbPropertyDefinition);
					session.getTransaction().commit();
					
					CloseSafe.close(new CloseableSession(session));
					return dbPropertyDefinition;
				}
				catch (Exception e) {
					CloseSafe.close(new CloseableSession(session,true));
					throw new Exception(e);
				}
			}
		}, 
		new ExecutionListener<DBPropertyDefinition>() {
			public void done(DBPropertyDefinition result) {
			}
			public void error(Throwable t) {
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.exceptionOccurred.name(),
						null, 
						t);	
			}
		});
	}

	public void removePropertyDefinition(
			final PropertyDefinition propertyDefinition, final TagDefinition tagDefinition) {
		dbRepository.getBackgroundServiceProvider().submit(
				"Removing Property definition",
			new DefaultProgressCallable<Void>() {
				public Void call() throws Exception {
					Session session = dbRepository.getSessionFactory().openSession();
					try {
						DBPropertyDefinition dbPropertyDefinition = 
							(DBPropertyDefinition)session.get(
								DBPropertyDefinition.class, 
								propertyDefinition.getId());
						
						getLibraryAccess(
							session, 
							dbPropertyDefinition.getDbTagDefinition().getDbTagsetDefinition().getTagLibraryId(),
							true);
						
						dbPropertyDefinition.getDbTagDefinition().setVersion(
								tagDefinition.getVersion().getDate());
						session.beginTransaction();
						session.update(dbPropertyDefinition.getDbTagDefinition());
						session.delete(dbPropertyDefinition);
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

	public void reloadTagLibraryReferences(Session session) {
		HashMap<String, TagLibraryReference> result = 
				getTagLibraryReferences(session);
		
		for (Map.Entry<String, TagLibraryReference> entry : result.entrySet()) {
			
			if (!tagLibraryReferencesById.containsKey(entry.getKey())) {
				tagLibraryReferencesById.put(
						entry.getKey(), entry.getValue());
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.tagLibraryChanged.name(),
						null, 
						entry.getValue());
			}
			else {
				ContentInfoSet oldContentInfoSet = 
						tagLibraryReferencesById.get(entry.getKey()).getContentInfoSet();
				tagLibraryReferencesById.put(
						entry.getKey(), entry.getValue());
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.tagLibraryChanged.name(),
						oldContentInfoSet, entry.getValue());
			}
		}
		
		Iterator<Map.Entry<String,TagLibraryReference>> iter = 
				tagLibraryReferencesById.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, TagLibraryReference> entry = iter.next();
			TagLibraryReference ref = entry.getValue();
			if (!result.containsKey(ref.getId())) {
				iter.remove();
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.tagLibraryChanged.name(),
						ref, null);	
			}
		}

	}
}
