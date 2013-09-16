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

import static de.catma.repository.db.jooq.catmarepository.Tables.PROPERTY;
import static de.catma.repository.db.jooq.catmarepository.Tables.PROPERTYDEFINITION;
import static de.catma.repository.db.jooq.catmarepository.Tables.PROPERTYDEF_POSSIBLEVALUE;
import static de.catma.repository.db.jooq.catmarepository.Tables.PROPERTYVALUE;
import static de.catma.repository.db.jooq.catmarepository.Tables.SOURCEDOCUMENT;
import static de.catma.repository.db.jooq.catmarepository.Tables.TAGDEFINITION;
import static de.catma.repository.db.jooq.catmarepository.Tables.TAGINSTANCE;
import static de.catma.repository.db.jooq.catmarepository.Tables.TAGLIBRARY;
import static de.catma.repository.db.jooq.catmarepository.Tables.TAGREFERENCE;
import static de.catma.repository.db.jooq.catmarepository.Tables.TAGSETDEFINITION;
import static de.catma.repository.db.jooq.catmarepository.Tables.USERMARKUPCOLLECTION;
import static de.catma.repository.db.jooq.catmarepository.Tables.USER_USERMARKUPCOLLECTION;

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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.db.CloseableSession;
import de.catma.document.Range;
import de.catma.document.repository.AccessMode;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.db.DBIndexer;
import de.catma.repository.db.model.DBCorpusUserMarkupCollection;
import de.catma.repository.db.model.DBProperty;
import de.catma.repository.db.model.DBPropertyDefinition;
import de.catma.repository.db.model.DBPropertyValue;
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
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

class DBUserMarkupCollectionHandler {
	
	private DBRepository dbRepository;
	private IDGenerator idGenerator;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Map<String,WeakReference<UserMarkupCollection>> umcCache;
	private DataSource dataSource;
	
	public DBUserMarkupCollectionHandler(DBRepository dbRepository) throws NamingException {
		this.dbRepository = dbRepository;
		this.idGenerator = new IDGenerator();
		this.umcCache = new HashMap<String, WeakReference<UserMarkupCollection>>();
		Context  context = new InitialContext();
		this.dataSource = (DataSource) context.lookup("catmads");
	}

	void createUserMarkupCollection(String name,
			SourceDocument sourceDocument) throws IOException {
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
		
		Integer sourceDocumentId = db
		.select(SOURCEDOCUMENT.SOURCEDOCUMENTID)
		.from(SOURCEDOCUMENT)
		.where(SOURCEDOCUMENT.LOCALURI.eq(sourceDocument.getID()))
		.fetchOne()
		.map(new IDFieldToIntegerMapper(SOURCEDOCUMENT.SOURCEDOCUMENTID));
		
		try {

			Integer tagLibraryId = db
			.insertInto(
				TAGLIBRARY,
					TAGLIBRARY.TITLE,
					TAGLIBRARY.INDEPENDENT)
			.values(
				name,
				(byte)0)
				.returning(TAGLIBRARY.TAGLIBRARYID)
				.fetchOne()
				.map(new IDFieldToIntegerMapper(TAGLIBRARY.TAGLIBRARYID));

			Integer userMarkupCollectionId = db
			.insertInto(
				USERMARKUPCOLLECTION,
					USERMARKUPCOLLECTION.TITLE,
					USERMARKUPCOLLECTION.SOURCEDOCUMENTID,
					USERMARKUPCOLLECTION.TAGLIBRARYID)
			.values(
				name,
				sourceDocumentId,
				tagLibraryId)
			.returning(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)
			.fetchOne()
			.map(new IDFieldToIntegerMapper(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID));
			
			db
			.insertInto(
				USER_USERMARKUPCOLLECTION,
					USER_USERMARKUPCOLLECTION.USERID,
					USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID,
					USER_USERMARKUPCOLLECTION.ACCESSMODE,
					USER_USERMARKUPCOLLECTION.OWNER)
			.values(
				dbRepository.getCurrentUser().getUserId(),
				userMarkupCollectionId,
				AccessMode.WRITE.getNumericRepresentation(),
				(byte)1)
			.execute();
				
			db.commitTransaction();

			UserMarkupCollectionReference reference = 
					new UserMarkupCollectionReference(
							String.valueOf(userMarkupCollectionId), 
							new ContentInfoSet(name));
			
			sourceDocument.addUserMarkupCollectionReference(reference);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.userMarkupCollectionChanged.name(),
					null, new Pair<UserMarkupCollectionReference, SourceDocument>(
							reference,sourceDocument));

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
	
	void importUserMarkupCollection(InputStream inputStream,
			final SourceDocument sourceDocument) throws IOException {
		dbRepository.setTagManagerListenersEnabled(false);

		UserMarkupCollectionSerializationHandler userMarkupCollectionSerializationHandler = 
				dbRepository.getSerializationHandlerFactory().getUserMarkupCollectionSerializationHandler();
		
		final UserMarkupCollection umc =
				userMarkupCollectionSerializationHandler.deserialize(null, inputStream);

		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			db.beginTransaction();
			
			dbRepository.getDbTagLibraryHandler().importTagLibrary(
				db, umc.getTagLibrary());
			
			importUserMarkupCollection(
					db, umc, sourceDocument);

			db.commitTransaction();
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
	
	
	private void importUserMarkupCollection(
			final DSLContext db, final UserMarkupCollection umc,
			final SourceDocument sourceDocument) throws Exception {
		
			Integer sourceDocumentId = db
			.select(SOURCEDOCUMENT.SOURCEDOCUMENTID)
			.from(SOURCEDOCUMENT)
			.where(SOURCEDOCUMENT.LOCALURI.eq(sourceDocument.getID()))
			.fetchOne()
			.map(new IDFieldToIntegerMapper(SOURCEDOCUMENT.SOURCEDOCUMENTID));

			Integer userMarkupCollectionId = db
			.insertInto(
				USERMARKUPCOLLECTION,
					USERMARKUPCOLLECTION.TITLE,
					USERMARKUPCOLLECTION.SOURCEDOCUMENTID,
					USERMARKUPCOLLECTION.TAGLIBRARYID)
			.values(
				umc.getName(),
				sourceDocumentId,
				Integer.valueOf(umc.getTagLibrary().getId()))
			.returning(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)
			.fetchOne()
			.map(new IDFieldToIntegerMapper(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID));
			
			db
			.insertInto(
				USER_USERMARKUPCOLLECTION,
					USER_USERMARKUPCOLLECTION.USERID,
					USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID,
					USER_USERMARKUPCOLLECTION.ACCESSMODE,
					USER_USERMARKUPCOLLECTION.OWNER)
			.values(
				dbRepository.getCurrentUser().getUserId(),
				userMarkupCollectionId,
				AccessMode.WRITE.getNumericRepresentation(),
				(byte)1)
			.execute();

			umc.setId(String.valueOf(userMarkupCollectionId));
			
			addDbTagReferences(db, umc);

			dbRepository.getIndexer().index(
					umc.getTagReferences(), 
					sourceDocument.getID(),
					umc.getId(),
					umc.getTagLibrary());
			
			UserMarkupCollectionReference umcRef = 
					new UserMarkupCollectionReference(
							String.valueOf(userMarkupCollectionId), 
							umc.getContentInfoSet());
			sourceDocument.addUserMarkupCollectionReference(umcRef);
				
			dbRepository.setTagManagerListenersEnabled(true);

			dbRepository.getPropertyChangeSupport().firePropertyChange(
				RepositoryChangeEvent.userMarkupCollectionChanged.name(),
				null, new Pair<UserMarkupCollectionReference, SourceDocument>(
						umcRef, sourceDocument));
	}

	private void addDbTagReferences(
			DSLContext db,
			UserMarkupCollection umc) {
		
		HashMap<String, Integer> tagInstances = new HashMap<String, Integer>();
		Integer curTagInstanceId = null;
		for (TagReference tr : umc.getTagReferences()) {

			if (tagInstances.containsKey(tr.getTagInstanceID())) {
				curTagInstanceId = tagInstances.get(tr.getTagInstanceID());
			}
			else {
				TagInstance ti = tr.getTagInstance();

				TagDefinition tDef = 
					umc.getTagLibrary().getTagDefinition(
						tr.getTagInstance().getTagDefinition().getUuid());
				
				curTagInstanceId = 
				db.insertInto(
					TAGINSTANCE,
						TAGINSTANCE.UUID,
						TAGINSTANCE.TAGDEFINITIONID)
				.values(
					idGenerator.catmaIDToUUIDBytes(tr.getTagInstanceID()),
					tDef.getId())
				.returning(TAGINSTANCE.TAGDEFINITIONID)
				.fetchOne()
				.map(new IDFieldToIntegerMapper(TAGINSTANCE.TAGDEFINITIONID));
				
				tagInstances.put(tr.getTagInstanceID(), curTagInstanceId);
				
				BatchBindStep insertBatch = db.batch(db
				.insertInto(
					PROPERTY,
						PROPERTY.PROPERTYDEFINITIONID,
						PROPERTY.TAGINSTANCEID)
				.values(
					(Integer)null,
					(Integer)null));
				
				boolean authorPresent = false;
				for (Property p : ti.getSystemProperties()) {
					insertBatch.bind(
						p.getPropertyDefinition().getId(), 
						curTagInstanceId);
					authorPresent =
						p.getPropertyDefinition().getName().equals(
							PropertyDefinition.SystemPropertyName.catma_markupauthor.name());
				}
				
				if (!authorPresent) {
					Property authorNameProp = 
						new Property(
							tDef.getPropertyDefinitionByName(
								PropertyDefinition.SystemPropertyName.catma_markupauthor.name()),
							new PropertyValueList(
								dbRepository.getCurrentUser().getIdentifier()));
					ti.addSystemProperty(authorNameProp);
					insertBatch.bind(
						authorNameProp.getPropertyDefinition().getId(), 
						curTagInstanceId);
				}
					 
				for (Property p : ti.getUserDefinedProperties()) {
					if (!p.getPropertyValueList().getValues().isEmpty()) {
						insertBatch.bind(
								p.getPropertyDefinition().getId(), 
								curTagInstanceId);
					}
				}

				insertBatch.execute();
			}
			
			db
			.insertInto(
				TAGREFERENCE,
					TAGREFERENCE.CHARACTERSTART,
					TAGREFERENCE.CHARACTEREND,
					TAGREFERENCE.USERMARKUPCOLLECTIONID,
					TAGREFERENCE.TAGINSTANCEID)
			.values(
				tr.getRange().getStartPoint(),
				tr.getRange().getEndPoint(),
				Integer.valueOf(umc.getId()),
				curTagInstanceId)
			.execute();
		}
	}

	private DBUserUserMarkupCollection getCurrentDBUserUserMarkupCollection(
			DBUserMarkupCollection dbUserMarkupCollection) {

		for (DBUserUserMarkupCollection dbUserUserMarkupCollection :
			dbUserMarkupCollection.getDbUserUserMarkupCollections()) {
			if (dbUserUserMarkupCollection.getDbUser().getUserId().equals(
					dbRepository.getCurrentUser().getUserId())) {
				return dbUserUserMarkupCollection;
			}
		}

		
		return null;
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
			
			DBUserUserMarkupCollection currentUserUserMarkupCollection = 
					getCurrentDBUserUserMarkupCollection(dbUserMarkupCollection);
			
			if (currentUserUserMarkupCollection == null) {
				throw new IllegalStateException(
						"you seem to have no access rights for this collection!");
			}
			Set<DBUserUserMarkupCollection> dbUserUserMarkupCollections =
					dbUserMarkupCollection.getDbUserUserMarkupCollections();
			
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

		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			Integer userMarkupCollectionId = Integer.valueOf(userMarkupCollectionReference.getId());
			
			Field<Integer> totalParticipantsField = db
			.select(DSL.count(USER_USERMARKUPCOLLECTION.USERID))
			.from(USER_USERMARKUPCOLLECTION)
			.where(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))
			.asField("totalParticipants");
			
			
			Record currentUserUmcRecord = db
			.select(
				USER_USERMARKUPCOLLECTION.USER_USERMARKUPCOLLECTIOID, 
				USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID, 
				USER_USERMARKUPCOLLECTION.ACCESSMODE, 
				USER_USERMARKUPCOLLECTION.OWNER,
				totalParticipantsField)
			.from(USER_USERMARKUPCOLLECTION)
			.where(USER_USERMARKUPCOLLECTION.USERID.eq(dbRepository.getCurrentUser().getUserId()))
			.and(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))
			.fetchOne();
		
			db.beginTransaction();
			
			if ((currentUserUmcRecord == null) 
					|| (currentUserUmcRecord.getValue(USER_USERMARKUPCOLLECTION.ACCESSMODE) == null)) {
				throw new IllegalStateException(
						"you seem to have no access rights for this collection!");
			}

			Integer userUmcId =
					currentUserUmcRecord.getValue(USER_USERMARKUPCOLLECTION.USER_USERMARKUPCOLLECTIOID);
			boolean isOwner = 
					currentUserUmcRecord.getValue(USER_USERMARKUPCOLLECTION.OWNER, Boolean.class);
			
			int totalParticipants = 
					(Integer)currentUserUmcRecord.getValue("totalParticipants");
			
			db
			.delete(USER_USERMARKUPCOLLECTION)
			.where(USER_USERMARKUPCOLLECTION.USER_USERMARKUPCOLLECTIOID.eq(userUmcId))
			.execute();
			
			if (isOwner && (totalParticipants == 1)) {
				
				db.batch(
				db
				.delete(PROPERTYVALUE)
				.where(PROPERTYVALUE.PROPERTYID.in(
					db
					.select(PROPERTY.PROPERTYID)
					.from(PROPERTY)
					.join(TAGINSTANCE)
						.on(TAGINSTANCE.TAGINSTANCEID.eq(PROPERTY.TAGINSTANCEID))
					.join(TAGREFERENCE)
						.on(TAGREFERENCE.TAGINSTANCEID.eq(TAGINSTANCE.TAGINSTANCEID))
						.and(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId)))),
				db
				.delete(PROPERTY)
				.where(PROPERTY.TAGINSTANCEID.in(
					db
					.select(TAGINSTANCE.TAGINSTANCEID)
					.from(TAGINSTANCE)
					.join(TAGREFERENCE)
						.on(TAGREFERENCE.TAGINSTANCEID.eq(TAGINSTANCE.TAGINSTANCEID))
						.and(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId)))),
				db
				.delete(TAGINSTANCE)
				.where(TAGINSTANCE.TAGINSTANCEID.in(
					db
					.select(TAGREFERENCE.TAGINSTANCEID)
					.from(TAGREFERENCE)
					.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId)))),
				db
				.delete(TAGREFERENCE)
				.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId)),
				db
				.delete(PROPERTYDEF_POSSIBLEVALUE)
				.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.in(
					db
					.select(PROPERTYDEFINITION.PROPERTYDEFINITIONID)
					.from(PROPERTYDEFINITION)
					.join(TAGDEFINITION)
						.on(TAGDEFINITION.TAGDEFINITIONID.eq(PROPERTYDEFINITION.TAGDEFINITIONID))
					.join(TAGSETDEFINITION)
						.on(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(TAGDEFINITION.TAGSETDEFINITIONID))
					.join(TAGLIBRARY)
						.on(TAGLIBRARY.TAGLIBRARYID.eq(TAGSETDEFINITION.TAGLIBRARYID))
					.join(USERMARKUPCOLLECTION)
						.on(USERMARKUPCOLLECTION.TAGLIBRARYID.eq(TAGLIBRARY.TAGLIBRARYID))
						.and(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId)))),
				db
				.delete(PROPERTYDEFINITION)
				.where(PROPERTYDEFINITION.TAGDEFINITIONID.in(
					db
					.select(TAGDEFINITION.TAGDEFINITIONID)
					.from(TAGDEFINITION)
					.join(TAGSETDEFINITION)
						.on(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(TAGDEFINITION.TAGSETDEFINITIONID))
					.join(TAGLIBRARY)
						.on(TAGLIBRARY.TAGLIBRARYID.eq(TAGSETDEFINITION.TAGLIBRARYID))
					.join(USERMARKUPCOLLECTION)
						.on(USERMARKUPCOLLECTION.TAGLIBRARYID.eq(TAGLIBRARY.TAGLIBRARYID))
						.and(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId)))),
				db
				.delete(TAGDEFINITION)
				.where(TAGDEFINITION.TAGSETDEFINITIONID.in(
					db
					.select(TAGSETDEFINITION.TAGSETDEFINITIONID)
					.from(TAGSETDEFINITION)
					.join(TAGLIBRARY)
						.on(TAGLIBRARY.TAGLIBRARYID.eq(TAGSETDEFINITION.TAGLIBRARYID))
					.join(USERMARKUPCOLLECTION)
						.on(USERMARKUPCOLLECTION.TAGLIBRARYID.eq(TAGLIBRARY.TAGLIBRARYID))
						.and(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId)))),
				db
				.delete(TAGSETDEFINITION)
				.where(TAGSETDEFINITION.TAGLIBRARYID.in(
					db
					.select(TAGLIBRARY.TAGLIBRARYID)
					.from(TAGLIBRARY)
					.join(USERMARKUPCOLLECTION)
						.on(USERMARKUPCOLLECTION.TAGLIBRARYID.eq(TAGLIBRARY.TAGLIBRARYID))
						.and(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId)))))
				.execute();

				Integer tagLibraryId = db
				.select(USERMARKUPCOLLECTION.TAGLIBRARYID)
				.from(USERMARKUPCOLLECTION)
				.where(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))
				.fetchOne()
				.map(new IDFieldToIntegerMapper(USERMARKUPCOLLECTION.TAGLIBRARYID));
				
				db.batch(
				db
				.delete(USERMARKUPCOLLECTION)
				.where(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId)),
				db
				.delete(TAGLIBRARY)
				.where(TAGLIBRARY.TAGLIBRARYID.eq(tagLibraryId)))
				.execute();
				
			}
			
			dbRepository.getIndexer().removeUserMarkupCollection(
					userMarkupCollectionReference.getId());
			
			SourceDocument sd = dbRepository.getSourceDocument(userMarkupCollectionReference);
			sd.removeUserMarkupCollectionReference(userMarkupCollectionReference);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.userMarkupCollectionChanged.name(),
					userMarkupCollectionReference, null);
			db.commitTransaction();
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
	UserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference, boolean refresh) throws IOException {
		if (!refresh) {
			WeakReference<UserMarkupCollection> weakUmc = umcCache.get(userMarkupCollectionReference.getId());
			if (weakUmc != null) {
				UserMarkupCollection umc = weakUmc.get();
				if (umc != null) {
					return umc;
				}
			}
		}
		Integer userMarkupCollectionId = 
				Integer.valueOf(userMarkupCollectionReference.getId());
		
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		
		Record umcRecord = db
		.select()
		.from(USERMARKUPCOLLECTION)
		.join(SOURCEDOCUMENT)
			.on(SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(USERMARKUPCOLLECTION.SOURCEDOCUMENTID))
		.where(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
				.eq(userMarkupCollectionId))
		.fetchOne();
				
		Integer tagLibraryId = umcRecord.getValue(USERMARKUPCOLLECTION.TAGLIBRARYID);
		String localSourceDocURI = umcRecord.getValue(SOURCEDOCUMENT.LOCALURI);
		
		TagLibrary tagLibrary = 
			dbRepository.getDbTagLibraryHandler().loadTagLibrayContent(
				db, 
				new TagLibraryReference(
					String.valueOf(tagLibraryId), 
					new ContentInfoSet(
							umcRecord.getValue(USERMARKUPCOLLECTION.AUTHOR),
							umcRecord.getValue(USERMARKUPCOLLECTION.DESCRIPTION),
							umcRecord.getValue(USERMARKUPCOLLECTION.PUBLISHER),
							umcRecord.getValue(USERMARKUPCOLLECTION.TITLE))));

		Map<Integer, Result<Record>> propertyValueRecordsByPropertyId = db
		.select(PROPERTYVALUE.fields())
		.from(PROPERTYVALUE)
		.join(PROPERTY)
			.on(PROPERTY.PROPERTYID.eq(PROPERTYVALUE.PROPERTYID))
		.join(TAGINSTANCE)
			.on(TAGINSTANCE.TAGINSTANCEID.eq(PROPERTY.TAGINSTANCEID))
			.and(TAGINSTANCE.TAGINSTANCEID
				.in(db
					.select(TAGREFERENCE.TAGINSTANCEID)
					.from(TAGREFERENCE)
					.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))))
		.fetchGroups(PROPERTYVALUE.PROPERTYID);
		
				
		
		Map<Integer, Result<Record>> propertyRecordsByTagInstanceId = db
		.select()
		.from(PROPERTY)
		.join(PROPERTYDEFINITION)
			.on(PROPERTYDEFINITION.PROPERTYDEFINITIONID.eq(PROPERTY.PROPERTYDEFINITIONID))
		.join(TAGINSTANCE)
			.on(TAGINSTANCE.TAGINSTANCEID.eq(PROPERTY.TAGINSTANCEID))
			.and(TAGINSTANCE.TAGINSTANCEID
				.in(db
					.select(TAGREFERENCE.TAGINSTANCEID)
					.from(TAGREFERENCE)
					.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))))
		.fetchGroups(TAGINSTANCE.TAGINSTANCEID);
		
		List<TagInstance> tagInstances = db
		.select()
		.from(TAGINSTANCE)
		.join(TAGDEFINITION)
			.on(TAGDEFINITION.TAGDEFINITIONID.eq(TAGINSTANCE.TAGDEFINITIONID))
		.where(TAGINSTANCE.TAGINSTANCEID
			.in(db
				.select(TAGREFERENCE.TAGINSTANCEID)
				.from(TAGREFERENCE)
				.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))))
		.fetch()
		.map(
			new TagInstanceMapper(
				tagLibrary, 
				propertyRecordsByTagInstanceId, 
				propertyValueRecordsByPropertyId));
				
		List<TagReference> tagReferences = db
		.select()
		.from(TAGREFERENCE)
		.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))
		.fetch()
		.map(new TagReferenceMapper(localSourceDocURI, tagInstances));
		
		UserMarkupCollection userMarkupCollection = 
			new UserMarkupCollectionMapper(tagLibrary, tagReferences).map(umcRecord);
		
		umcCache.put(
				userMarkupCollection.getId(),
				new WeakReference<UserMarkupCollection>(userMarkupCollection));
		
		return userMarkupCollection;
	}
	
	
	// HIER GEHTS WEITER


	void addTagReferences(UserMarkupCollection userMarkupCollection,
			List<TagReference> tagReferences) throws IOException {
		String sourceDocumentID = 
				dbRepository.getDbSourceDocumentHandler().getLocalUriFor(
						new UserMarkupCollectionReference(
								userMarkupCollection.getId(), 
								userMarkupCollection.getContentInfoSet()));
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			checkWriteAccess(session, Integer.valueOf(userMarkupCollection.getId()));

			Set<TagInstance> incomingTagInstances = 
					new HashSet<TagInstance>();
			
			for (TagReference tr : tagReferences) {
				incomingTagInstances.add(tr.getTagInstance());
			}

			session.beginTransaction();
			DBUserMarkupCollection dbUserMarkupCollection = 
					(DBUserMarkupCollection) session.get(
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
			(DBTagDefinition) session.get(
					DBTagDefinition.class, 
					ti.getTagDefinition().getId());
		
		DBTagInstance dbTagInstance = 
			new DBTagInstance(
				idGenerator.catmaIDToUUIDBytes(ti.getUuid()),
				dbTagDefinition);
		
		for (Property prop : ti.getSystemProperties()) {
			DBPropertyDefinition dbPropDef =
					(DBPropertyDefinition) session.get(
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
					(DBPropertyDefinition) session.get(
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
	
	
	AccessMode getUserMarkupCollectionAccessMode(
			DSLContext db, Integer userMarkupCollectionId, boolean checkWriteAccess) 
					throws IOException {
		Record accessModeRecord = db
				.select(USER_USERMARKUPCOLLECTION.ACCESSMODE)
				.from(USER_USERMARKUPCOLLECTION)
				.where(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
						.eq(userMarkupCollectionId))
				.and(USER_USERMARKUPCOLLECTION.USERID
						.eq(dbRepository.getCurrentUser().getUserId()))
				.fetchOne();
			
		Integer existingAccessModeValue = 
				accessModeRecord.getValue(USER_USERMARKUPCOLLECTION.ACCESSMODE);

		if (existingAccessModeValue == null) {
			throw new IOException(
					"You seem to have no access to this collection! " +
					"Please reload the repository!");
		}
		else if (checkWriteAccess && accessModeRecord.getValue(USER_USERMARKUPCOLLECTION.ACCESSMODE) 
							!= AccessMode.WRITE.getNumericRepresentation()) {
			throw new IOException(
					"You seem to have no write access to this collection! " +
					"Please reload this collection!");
		}
		else {
			return AccessMode.getAccessMode(existingAccessModeValue);
		}
	}
	

	boolean hasWriteAccess(DSLContext db, Integer userMarkupCollectionId) throws IOException {
		DBUserMarkupCollection dbUserMarkupCollection = 
				(DBUserMarkupCollection) session.get(
				DBUserMarkupCollection.class, 
				userMarkupCollectionId);
		
		DBUserUserMarkupCollection currentDBUserUmc = 
				getCurrentDBUserUserMarkupCollection(dbUserMarkupCollection);
		
		return (currentDBUserUmc.getAccessMode() 
				== AccessMode.WRITE.getNumericRepresentation());
	}
	
	void checkWriteAccess(Session session, Integer userMarkupCollectionId) throws IOException {
		if (!hasWriteAccess(session, userMarkupCollectionId)) {
			throw new IOException(
					"You seem to have no write access to this collection! " +
					"Please reload this Collection!");
		}
	}	

	void updateTagsetDefinitionInUserMarkupCollections(
			List<UserMarkupCollection> userMarkupCollections,
			TagsetDefinition tagsetDefinition) throws IOException {
		
		Session session = dbRepository.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			for (UserMarkupCollection userMarkupCollection : 
				userMarkupCollections) {

				if (hasWriteAccess(session, Integer.valueOf(userMarkupCollection.getId()))) {
					
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
				checkWriteAccess(
					session,
					dbTagInstance.getDbTagReferences().iterator().next().getDbUserMarkupCollection().getUsermarkupCollectionId());
				
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
							checkWriteAccess(
									session, userMarkupCollectionId);
							
							DBUserMarkupCollection dbUserMarkupCollection =
									(DBUserMarkupCollection) session.get(
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
					(DBUserMarkupCollection) session.get(
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
			checkWriteAccess(
				session,
				dbProperty.getDbTagInstance().getDbTagReferences().iterator().next().getDbUserMarkupCollection().getUsermarkupCollectionId());

			session.beginTransaction();
			updater.updateDbProperty(session, dbProperty, property);
			session.saveOrUpdate(dbProperty);
			dbRepository.getIndexer().updateIndex(tagInstance, property);
			session.getTransaction().commit();

			CloseSafe.close(new CloseableSession(session));
		}
		catch (Exception e) {
			CloseSafe.close(new CloseableSession(session,true));
			throw new IOException(e);
		}
	}
}
