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

import static de.catma.repository.db.jooqgen.catmarepository.Tables.CORPUS_USERMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTY;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYDEF_POSSIBLEVALUE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYVALUE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGINSTANCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGLIBRARY;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGREFERENCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGSETDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_USERMARKUPCOLLECTION;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import de.catma.document.repository.AccessMode;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.TagsetDefinitionUpdateLog;
import de.catma.repository.db.jooq.TransactionalDSLContext;
import de.catma.repository.db.mapper.FieldToValueMapper;
import de.catma.repository.db.mapper.IDFieldToIntegerMapper;
import de.catma.repository.db.mapper.IDFieldsToIntegerPairMapper;
import de.catma.repository.db.mapper.PropertyValueMapper;
import de.catma.repository.db.mapper.TagInstanceMapper;
import de.catma.repository.db.mapper.TagReferenceMapper;
import de.catma.repository.db.mapper.UserMarkupCollectionMapper;
import de.catma.repository.db.mapper.UserMarkupCollectionReferenceMapper;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagsetDefinition;
import de.catma.util.Collections3;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

class UserMarkupCollectionHandler {
	
	private DBRepository dbRepository;
	private IDGenerator idGenerator;
//	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Map<String,WeakReference<UserMarkupCollection>> umcCache;
	private DataSource dataSource;
	
	UserMarkupCollectionHandler(DBRepository dbRepository) throws NamingException {
		this.dbRepository = dbRepository;
		this.idGenerator = new IDGenerator();
		this.umcCache = new HashMap<String, WeakReference<UserMarkupCollection>>();
		Context  context = new InitialContext();
		this.dataSource = (DataSource) context.lookup(CatmaDataSourceName.CATMADS.name());
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
			db.beginTransaction();
			
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
		catch (Exception dae) {
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
				db, umc.getTagLibrary(), false);
			
			UserMarkupCollectionReference umcRef = importUserMarkupCollection(
					db, umc, sourceDocument);

			db.commitTransaction();
			
			// index the imported collection
			dbRepository.getIndexer().index(
					umc.getTagReferences(), 
					sourceDocument.getID(),
					umc.getId(),
					umc.getTagLibrary());
			
			dbRepository.setTagManagerListenersEnabled(true);

			dbRepository.getPropertyChangeSupport().firePropertyChange(
				RepositoryChangeEvent.userMarkupCollectionChanged.name(),
				null, new Pair<UserMarkupCollectionReference, SourceDocument>(
						umcRef, sourceDocument));

		}
		catch (Exception dae) {
			db.rollbackTransaction();
			db.close();
			dbRepository.setTagManagerListenersEnabled(true);

			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					new IOException(dae));				
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	}
	
	
	private UserMarkupCollectionReference importUserMarkupCollection(
			final DSLContext db, final UserMarkupCollection umc,
			final SourceDocument sourceDocument) throws IOException {
		
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
		
		addTagReferences(db, umc);

		UserMarkupCollectionReference umcRef = 
				new UserMarkupCollectionReference(
						String.valueOf(userMarkupCollectionId), 
						umc.getContentInfoSet());
		sourceDocument.addUserMarkupCollectionReference(umcRef);
		
		return umcRef;
	}

	private void addTagReferences(
			DSLContext db,
			UserMarkupCollection umc) {
		addTagReferences(db, umc, umc.getTagReferences());
	}

	private void addTagReferences(DSLContext db, UserMarkupCollection umc,
			List<TagReference> tagReferences) {
		
		HashMap<String, Integer> tagInstances = new HashMap<String, Integer>();
		Integer curTagInstanceId = null;
		for (TagReference tr : tagReferences) {

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
				.returning(TAGINSTANCE.TAGINSTANCEID)
				.fetchOne()
				.map(new IDFieldToIntegerMapper(TAGINSTANCE.TAGINSTANCEID));
				
				tagInstances.put(tr.getTagInstanceID(), curTagInstanceId);

				BatchBindStep pValueInsertBatch = db.batch(db
						.insertInto(
							PROPERTYVALUE,
								PROPERTYVALUE.PROPERTYID,
								PROPERTYVALUE.VALUE)
						.values(
							(Integer)null,
							(String)null));

				boolean authorPresent = false;
				for (Property p : ti.getSystemProperties()) {
					
					if (p.getPropertyDefinition().getName().equals(
							PropertyDefinition.SystemPropertyName.catma_markupauthor.name())) {
						p.setPropertyValueList(new PropertyValueList(
								dbRepository.getCurrentUser().getIdentifier()));
						authorPresent = true;
					}
					
					Integer propertyId = db
						.insertInto(
							PROPERTY,
								PROPERTY.PROPERTYDEFINITIONID,
								PROPERTY.TAGINSTANCEID)
						.values(
							p.getPropertyDefinition().getId(),
							curTagInstanceId)
						.returning(PROPERTY.PROPERTYID)
						.fetchOne()
						.map(new IDFieldToIntegerMapper(PROPERTY.PROPERTYID));
				
					
					for (String value : p.getPropertyValueList().getValues()) {
						pValueInsertBatch.bind(propertyId, value);
					}
					
				}
				
				if (!authorPresent) {
					Property authorNameProp = 
						new Property(
							tDef.getPropertyDefinitionByName(
								PropertyDefinition.SystemPropertyName.catma_markupauthor.name()),
							new PropertyValueList(
								dbRepository.getCurrentUser().getIdentifier()));
					ti.addSystemProperty(authorNameProp);
					
					Integer propertyId = db
						.insertInto(
							PROPERTY,
								PROPERTY.PROPERTYDEFINITIONID,
								PROPERTY.TAGINSTANCEID)
						.values(
							authorNameProp.getPropertyDefinition().getId(),
							curTagInstanceId)
						.returning(PROPERTY.PROPERTYID)
						.fetchOne()
						.map(new IDFieldToIntegerMapper(PROPERTY.PROPERTYID));

					pValueInsertBatch.bind(
						propertyId, 
						authorNameProp.getPropertyValueList().getFirstValue());
				}
					 
				for (Property p : ti.getUserDefinedProperties()) {
					if (!p.getPropertyValueList().getValues().isEmpty()) {
						Integer propertyId = db
							.insertInto(
								PROPERTY,
									PROPERTY.PROPERTYDEFINITIONID,
									PROPERTY.TAGINSTANCEID)
							.values(
								p.getPropertyDefinition().getId(),
								curTagInstanceId)
							.returning(PROPERTY.PROPERTYID)
							.fetchOne()
							.map(new IDFieldToIntegerMapper(PROPERTY.PROPERTYID));
					
						
						for (String value : p.getPropertyValueList().getValues()) {
							pValueInsertBatch.bind(propertyId, value);
						}
					}
				}

				pValueInsertBatch.execute();
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

	void delete(
			UserMarkupCollectionReference userMarkupCollectionReference) throws IOException {

		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			Integer userMarkupCollectionId = 
					Integer.valueOf(userMarkupCollectionReference.getId());
			
			Field<Integer> totalParticipantsField = db
			.select(DSL.count(USER_USERMARKUPCOLLECTION.USERID))
			.from(USER_USERMARKUPCOLLECTION)
			.where(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
					.eq(userMarkupCollectionId))
			.asField("totalParticipants");
			
			
			Record currentUserUmcRecord = db
			.select(
				USER_USERMARKUPCOLLECTION.USER_USERMARKUPCOLLECTIOID, 
				USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID, 
				USER_USERMARKUPCOLLECTION.ACCESSMODE, 
				USER_USERMARKUPCOLLECTION.OWNER,
				totalParticipantsField)
			.from(USER_USERMARKUPCOLLECTION)
			.where(USER_USERMARKUPCOLLECTION.USERID
					.eq(dbRepository.getCurrentUser().getUserId()))
			.and(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))
			.fetchOne();
		
			db.beginTransaction();
			
			if ((currentUserUmcRecord == null) 
					|| (currentUserUmcRecord.getValue(
							USER_USERMARKUPCOLLECTION.ACCESSMODE) == null)) {
				throw new IllegalStateException(
						"you seem to have no access rights for this collection!");
			}

			Integer userUmcId =
					currentUserUmcRecord.getValue(
							USER_USERMARKUPCOLLECTION.USER_USERMARKUPCOLLECTIOID);
			boolean isOwner = 
					currentUserUmcRecord.getValue(
							USER_USERMARKUPCOLLECTION.OWNER, Boolean.class);
			
			int totalParticipants = 
					(Integer)currentUserUmcRecord.getValue("totalParticipants");
			
			db
			.delete(USER_USERMARKUPCOLLECTION)
			.where(USER_USERMARKUPCOLLECTION.USER_USERMARKUPCOLLECTIOID.eq(userUmcId))
			.execute();
			
//			if (isOwner && (totalParticipants == 1)) {
//				
//				List<Integer> tagInstanceIds = db
//				.select(TAGINSTANCE.TAGINSTANCEID)
//				.from(TAGINSTANCE)
//				.where(TAGINSTANCE.TAGINSTANCEID.in(
//					db
//					.select(TAGREFERENCE.TAGINSTANCEID)
//					.from(TAGREFERENCE)
//					.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))))
//				.fetch()
//				.map(new IDFieldToIntegerMapper(TAGINSTANCE.TAGINSTANCEID));
//
//				if (tagInstanceIds.isEmpty()) {
//					tagInstanceIds = Collections.singletonList(-1);
//				}
//						
//				db.batch(
//					db
//					.delete(PROPERTYVALUE)
//					.where(PROPERTYVALUE.PROPERTYID.in(
//						db
//						.select(PROPERTY.PROPERTYID)
//						.from(PROPERTY)
//						.join(TAGINSTANCE)
//							.on(TAGINSTANCE.TAGINSTANCEID
//									.eq(PROPERTY.TAGINSTANCEID))
//						.join(TAGREFERENCE)
//							.on(TAGREFERENCE.TAGINSTANCEID
//									.eq(TAGINSTANCE.TAGINSTANCEID))
//							.and(TAGREFERENCE.USERMARKUPCOLLECTIONID
//									.eq(userMarkupCollectionId)))),
//					db
//					.delete(PROPERTY)
//					.where(PROPERTY.TAGINSTANCEID.in(
//						db
//						.select(TAGINSTANCE.TAGINSTANCEID)
//						.from(TAGINSTANCE)
//						.join(TAGREFERENCE)
//							.on(TAGREFERENCE.TAGINSTANCEID
//									.eq(TAGINSTANCE.TAGINSTANCEID))
//							.and(TAGREFERENCE.USERMARKUPCOLLECTIONID
//									.eq(userMarkupCollectionId)))),
//					db
//					.delete(TAGREFERENCE)
//					.where(TAGREFERENCE.USERMARKUPCOLLECTIONID
//							.eq(userMarkupCollectionId)),
//					db
//					.delete(TAGINSTANCE)
//					.where(TAGINSTANCE.TAGINSTANCEID.in(tagInstanceIds)),
//					db
//					.delete(PROPERTYDEF_POSSIBLEVALUE)
//					.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.in(
//						db
//						.select(PROPERTYDEFINITION.PROPERTYDEFINITIONID)
//						.from(PROPERTYDEFINITION)
//						.join(TAGDEFINITION)
//							.on(TAGDEFINITION.TAGDEFINITIONID
//									.eq(PROPERTYDEFINITION.TAGDEFINITIONID))
//						.join(TAGSETDEFINITION)
//							.on(TAGSETDEFINITION.TAGSETDEFINITIONID
//									.eq(TAGDEFINITION.TAGSETDEFINITIONID))
//						.join(TAGLIBRARY)
//							.on(TAGLIBRARY.TAGLIBRARYID
//									.eq(TAGSETDEFINITION.TAGLIBRARYID))
//						.join(USERMARKUPCOLLECTION)
//							.on(USERMARKUPCOLLECTION.TAGLIBRARYID
//									.eq(TAGLIBRARY.TAGLIBRARYID))
//							.and(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
//									.eq(userMarkupCollectionId)))),
//					db
//					.delete(PROPERTYDEFINITION)
//					.where(PROPERTYDEFINITION.TAGDEFINITIONID.in(
//						db
//						.select(TAGDEFINITION.TAGDEFINITIONID)
//						.from(TAGDEFINITION)
//						.join(TAGSETDEFINITION)
//							.on(TAGSETDEFINITION.TAGSETDEFINITIONID
//									.eq(TAGDEFINITION.TAGSETDEFINITIONID))
//						.join(TAGLIBRARY)
//							.on(TAGLIBRARY.TAGLIBRARYID
//									.eq(TAGSETDEFINITION.TAGLIBRARYID))
//						.join(USERMARKUPCOLLECTION)
//							.on(USERMARKUPCOLLECTION.TAGLIBRARYID
//									.eq(TAGLIBRARY.TAGLIBRARYID))
//							.and(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
//									.eq(userMarkupCollectionId)))),
//					db
//					.update(TAGDEFINITION)
//					.set(TAGDEFINITION.PARENTID, (Integer)null)
//					.where(TAGDEFINITION.TAGSETDEFINITIONID.in(
//							db
//							.select(TAGSETDEFINITION.TAGSETDEFINITIONID)
//							.from(TAGSETDEFINITION)
//							.join(TAGLIBRARY)
//								.on(TAGLIBRARY.TAGLIBRARYID
//										.eq(TAGSETDEFINITION.TAGLIBRARYID))
//							.join(USERMARKUPCOLLECTION)
//								.on(USERMARKUPCOLLECTION.TAGLIBRARYID
//										.eq(TAGLIBRARY.TAGLIBRARYID))
//								.and(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
//										.eq(userMarkupCollectionId)))),
//					db
//					.delete(TAGDEFINITION)
//					.where(TAGDEFINITION.TAGSETDEFINITIONID.in(
//						db
//						.select(TAGSETDEFINITION.TAGSETDEFINITIONID)
//						.from(TAGSETDEFINITION)
//						.join(TAGLIBRARY)
//							.on(TAGLIBRARY.TAGLIBRARYID
//									.eq(TAGSETDEFINITION.TAGLIBRARYID))
//						.join(USERMARKUPCOLLECTION)
//							.on(USERMARKUPCOLLECTION.TAGLIBRARYID
//									.eq(TAGLIBRARY.TAGLIBRARYID))
//							.and(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
//									.eq(userMarkupCollectionId)))),
//					db
//					.delete(TAGSETDEFINITION)
//					.where(TAGSETDEFINITION.TAGLIBRARYID.in(
//						db
//						.select(TAGLIBRARY.TAGLIBRARYID)
//						.from(TAGLIBRARY)
//						.join(USERMARKUPCOLLECTION)
//							.on(USERMARKUPCOLLECTION.TAGLIBRARYID
//									.eq(TAGLIBRARY.TAGLIBRARYID))
//							.and(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
//									.eq(userMarkupCollectionId)))))
//				.execute();
//
//				Integer tagLibraryId = db
//				.select(USERMARKUPCOLLECTION.TAGLIBRARYID)
//				.from(USERMARKUPCOLLECTION)
//				.where(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
//						.eq(userMarkupCollectionId))
//				.fetchOne()
//				.map(new IDFieldToIntegerMapper(USERMARKUPCOLLECTION.TAGLIBRARYID));
//				
//				db.batch(
//					db
//					.delete(CORPUS_USERMARKUPCOLLECTION)
//					.where(CORPUS_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
//							.eq(userMarkupCollectionId)),
//					db
//					.delete(USERMARKUPCOLLECTION)
//					.where(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
//							.eq(userMarkupCollectionId)),
//					db
//					.delete(TAGLIBRARY)
//					.where(TAGLIBRARY.TAGLIBRARYID.eq(tagLibraryId)))
//				.execute();
//				
//			}
			
			db.commitTransaction();

//			if (isOwner && (totalParticipants == 1)) {
//				dbRepository.getIndexer().removeUserMarkupCollection(
//						userMarkupCollectionReference.getId());
//			}			
			

			SourceDocument sd = 
					dbRepository.getSourceDocument(userMarkupCollectionReference);
			sd.removeUserMarkupCollectionReference(userMarkupCollectionReference);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.userMarkupCollectionChanged.name(),
					userMarkupCollectionReference, null);
		}
		catch (Exception e) {
			db.rollbackTransaction();
			db.close();
			throw new IOException(e);
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	}

	UserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference, 
			boolean refresh) throws IOException {
		if (!refresh) {
			WeakReference<UserMarkupCollection> weakUmc = 
					umcCache.get(userMarkupCollectionReference.getId());
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
			.on(SOURCEDOCUMENT.SOURCEDOCUMENTID
					.eq(USERMARKUPCOLLECTION.SOURCEDOCUMENTID))
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
			.on(PROPERTYDEFINITION.PROPERTYDEFINITIONID
					.eq(PROPERTY.PROPERTYDEFINITIONID))
		.join(TAGINSTANCE)
			.on(TAGINSTANCE.TAGINSTANCEID.eq(PROPERTY.TAGINSTANCEID))
			.and(TAGINSTANCE.TAGINSTANCEID
				.in(db
					.select(TAGREFERENCE.TAGINSTANCEID)
					.from(TAGREFERENCE)
					.where(TAGREFERENCE.USERMARKUPCOLLECTIONID
							.eq(userMarkupCollectionId))))
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
				.where(TAGREFERENCE.USERMARKUPCOLLECTIONID
						.eq(userMarkupCollectionId))))
		.fetch()
		.map(
			new TagInstanceMapper(
				tagLibrary, 
				propertyRecordsByTagInstanceId, 
				propertyValueRecordsByPropertyId));
				
		@SuppressWarnings("unchecked")
		List<TagReference> tagReferences = db
		.select(Collections3.getUnion(TAGREFERENCE.fields(), TAGINSTANCE.UUID))
		.from(TAGREFERENCE)
		.join(TAGINSTANCE)
			.on(TAGINSTANCE.TAGINSTANCEID.eq(TAGREFERENCE.TAGINSTANCEID))
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
	

	void addTagReferences(UserMarkupCollection userMarkupCollection,
			List<TagReference> tagReferences) throws IOException {

		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
		Integer userMarkupCollectionId = 
				Integer.valueOf(userMarkupCollection.getId());
		try {
			getUserMarkupCollectionAccessMode(
				db, userMarkupCollectionId, true);

			String sourceDocumentID = db
			.select(SOURCEDOCUMENT.LOCALURI)
			.from(SOURCEDOCUMENT)
			.join(USERMARKUPCOLLECTION)
				.on(USERMARKUPCOLLECTION.SOURCEDOCUMENTID.eq(SOURCEDOCUMENT.SOURCEDOCUMENTID))
				.and(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))
			.fetchOne()
			.map(new FieldToValueMapper<String>(SOURCEDOCUMENT.LOCALURI));
			
			db.beginTransaction();
			addTagReferences(db, userMarkupCollection, tagReferences);
			db.commitTransaction();

			dbRepository.getIndexer().index(
					tagReferences, sourceDocumentID, 
					userMarkupCollection.getId(), 
					userMarkupCollection.getTagLibrary());

		}
		catch (Exception dae) {
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
		else if (checkWriteAccess
				&& accessModeRecord.getValue(USER_USERMARKUPCOLLECTION.ACCESSMODE) 
							!= AccessMode.WRITE.getNumericRepresentation()) {
			throw new IOException(
					"You seem to have no write access to this collection! " +
					"Please reload this collection!");
		}
		else {
			return AccessMode.getAccessMode(existingAccessModeValue);
		}
	}
	


	void updateTagsetDefinitionInUserMarkupCollections(
			List<UserMarkupCollection> userMarkupCollections,
			final TagsetDefinition tagsetDefinition) throws IOException {
		List<Callable<Void>> reindexJobs = new ArrayList<Callable<Void>>();
		
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			
//			db.beginTransaction();
			for (final UserMarkupCollection userMarkupCollection : 
				userMarkupCollections) {
				
				Integer userMarkupCollectionId = 
						Integer.valueOf(userMarkupCollection.getId());
				getUserMarkupCollectionAccessMode(db, userMarkupCollectionId, true);
			
				
				TagLibrary tagLibrary = 
						userMarkupCollection.getTagLibrary();
				
				Set<byte[]> relevantTagInstanceUUIDs = 
						new HashSet<byte[]>();
				
				Set<TagInstance> relevantTagInstances = 
						new HashSet<TagInstance>();
				
				for (TagReference tr : userMarkupCollection.getTagReferences()) {
					relevantTagInstanceUUIDs.add(
							idGenerator.catmaIDToUUIDBytes(tr.getTagInstanceID()));
					relevantTagInstances.add(tr.getTagInstance());
				}
 
				SelectConditionStep<?> selectObsoleteTagInstanceIdsQuery = db
				.select(TAGINSTANCE.TAGINSTANCEID)
				.from(TAGINSTANCE)
				.where(TAGINSTANCE.TAGINSTANCEID.in(
					db
					.select(TAGREFERENCE.TAGINSTANCEID)
					.from(TAGREFERENCE)
					.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))));
					
				// get list of TagInstances which are no longer present in the umc
				
				if (!relevantTagInstances.isEmpty()) {
					selectObsoleteTagInstanceIdsQuery = 
						selectObsoleteTagInstanceIdsQuery
							.and(TAGINSTANCE.UUID.notIn(relevantTagInstanceUUIDs));
				}
				
				List<Integer> obsoleteTagInstanceIds = 
					selectObsoleteTagInstanceIdsQuery
					.fetch()
					.map(new IDFieldToIntegerMapper(TAGINSTANCE.TAGINSTANCEID));
				
				if (!obsoleteTagInstanceIds.isEmpty()) {
					// handle deleted TagInstances/TagReferences and their content
					db.batch(
						// delete obsolete tag references
						db
						.delete(TAGREFERENCE)
						.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))
						.and(TAGREFERENCE.TAGINSTANCEID.in(obsoleteTagInstanceIds)),
						// delete the prop values of obsolete tag instances
						db
						.delete(PROPERTYVALUE)
						.where(PROPERTYVALUE.PROPERTYID
							.in(db
								.select(PROPERTY.PROPERTYID)
								.from(PROPERTY)
								.where(PROPERTY.TAGINSTANCEID.in(obsoleteTagInstanceIds)))),
						// delete the properties of obsolete tag instances
						db
						.delete(PROPERTY)
						.where(PROPERTY.TAGINSTANCEID.in(obsoleteTagInstanceIds)),
						// delete obsolete taginstances
						db
						.delete(TAGINSTANCE)
						.where(TAGINSTANCE.TAGINSTANCEID.in(obsoleteTagInstanceIds)))
					.execute();
				}
				// handle deleted Properties
				for (TagInstance ti : relevantTagInstances) {
					// get all propertyDef IDs
					Collection<Integer> relevantUserDefPropertyDefIds = 
							Collections2.transform(ti.getUserDefinedProperties(),
									new Function<Property, Integer>() {
								public Integer apply(
										Property property) {
									return property.getPropertyDefinition().getId();
								}});
					
					byte[] tagIntanceUUIDbin = 
							idGenerator.catmaIDToUUIDBytes(ti.getUuid()); 
					
					if (relevantUserDefPropertyDefIds.isEmpty()) { // no restriction
						db.batch(
							// delete property values of properties
							db
							.delete(PROPERTYVALUE)
							.where(PROPERTYVALUE.PROPERTYID
								.in(db
									.select(PROPERTY.PROPERTYID)
									.from(PROPERTY)
									.join(TAGINSTANCE)
										.on(TAGINSTANCE.TAGINSTANCEID
												.eq(PROPERTY.TAGINSTANCEID))
										.and(TAGINSTANCE.UUID.eq(tagIntanceUUIDbin)))),
							// delete properties 
							db
							.delete(PROPERTY)
							.where(PROPERTY.TAGINSTANCEID.eq(db
									.select(TAGINSTANCE.TAGINSTANCEID)
									.from(TAGINSTANCE)
									.where(TAGINSTANCE.UUID.eq(tagIntanceUUIDbin)))))
						.execute();
					}
					else {
						db.batch(
							// delete property values of properties which are no longer present
							db
							.delete(PROPERTYVALUE)
							.where(PROPERTYVALUE.PROPERTYID
								.in(db
									.select(PROPERTY.PROPERTYID)
									.from(PROPERTY)
									.join(TAGINSTANCE)
										.on(TAGINSTANCE.TAGINSTANCEID
												.eq(PROPERTY.TAGINSTANCEID))
										.and(TAGINSTANCE.UUID.eq(tagIntanceUUIDbin))
									.where(PROPERTY.PROPERTYDEFINITIONID
											.notIn(relevantUserDefPropertyDefIds)))),
							// delete properties which are no longer present
							db
							.delete(PROPERTY)
							.where(PROPERTY.TAGINSTANCEID.eq(db
									.select(TAGINSTANCE.TAGINSTANCEID)
									.from(TAGINSTANCE)
									.where(TAGINSTANCE.UUID.eq(tagIntanceUUIDbin))))
							.and(PROPERTY.PROPERTYDEFINITIONID.notIn(relevantUserDefPropertyDefIds)))
						.execute();

					}
					// addition of new properties with default values is not 
					// represented on the database side
					
				}

				final TagsetDefinitionUpdateLog tagsetDefinitionUpdateLog = 
						dbRepository.getDbTagLibraryHandler().updateTagsetDefinition(
								db, tagLibrary,
								tagLibrary.getTagsetDefinition(tagsetDefinition.getUuid()));
				

				if (!tagsetDefinitionUpdateLog.isEmpty()) {
					reindexJobs.add(
							new Callable<Void>() {
								public Void call() throws Exception {
									dbRepository.getIndexer().reindex(
											tagsetDefinition, 
											tagsetDefinitionUpdateLog,
											userMarkupCollection);
									return null;
								}
							});
				}
			}
			
//			db.commitTransaction();
			db.close();
			
			for (Callable<Void> reindexJob : reindexJobs) {
				reindexJob.call();
			}
		}
		catch (Exception dae) {
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
	
	void removeTagReferences(
			List<TagReference> tagReferences) throws IOException {

		
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {

			Set<byte[]> relevantTagInstanceUUIDs = 
					new HashSet<byte[]>();
			
			for (TagReference tr : tagReferences) {
				relevantTagInstanceUUIDs.add(
						idGenerator.catmaIDToUUIDBytes(tr.getTagInstanceID()));
			}

			List<Integer> relevantUserMarkupCollectionIds = db
			.select(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)
			.from(USERMARKUPCOLLECTION)
			.where(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
				.in(db
					.select(TAGREFERENCE.USERMARKUPCOLLECTIONID)
					.from(TAGREFERENCE)
					.join(TAGINSTANCE)
						.on(TAGINSTANCE.TAGINSTANCEID.eq(TAGREFERENCE.TAGINSTANCEID))
						.and(TAGINSTANCE.UUID.in(relevantTagInstanceUUIDs))))
			.fetch()
			.map(new IDFieldToIntegerMapper(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID));
			
			for (Integer userMarkupCollectionId : relevantUserMarkupCollectionIds) {
				getUserMarkupCollectionAccessMode(db, userMarkupCollectionId, true);
			}
			
			List<Integer> relevantTagInstanceIDs = db
			.select(TAGINSTANCE.TAGINSTANCEID)
			.from(TAGINSTANCE)
			.where(TAGINSTANCE.UUID.in(relevantTagInstanceUUIDs))
			.fetch()
			.map(new IDFieldToIntegerMapper(TAGINSTANCE.TAGINSTANCEID));
			
			db.beginTransaction();

			db.batch(
				db
				.delete(PROPERTYVALUE)
				.where(PROPERTYVALUE.PROPERTYID.in(
					db
					.select(PROPERTY.PROPERTYID)
					.from(PROPERTY)
					.where(PROPERTY.TAGINSTANCEID.in(relevantTagInstanceIDs)))),
				db
				.delete(PROPERTY)
				.where(PROPERTY.TAGINSTANCEID.in(relevantTagInstanceIDs)),
				db
				.delete(TAGREFERENCE)
				.where(TAGREFERENCE.TAGINSTANCEID.in(relevantTagInstanceIDs)),
				db
				.delete(TAGINSTANCE)
				.where(TAGINSTANCE.TAGINSTANCEID.in(relevantTagInstanceIDs)))
			.execute();
			
			dbRepository.getIndexer().removeTagReferences(tagReferences);
		
			db.commitTransaction();
		}
		catch (Exception e) {
			db.rollbackTransaction();
			db.close();
			throw new IOException(e);
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	}

	void update(
			final UserMarkupCollectionReference userMarkupCollectionReference, 
			final ContentInfoSet contentInfoSet) {
		
		final Integer userMarkupCollectionId = 
			Integer.valueOf(userMarkupCollectionReference.getId());

		final String author = contentInfoSet.getAuthor();
		final String publisher = contentInfoSet.getPublisher();
		final String title = contentInfoSet.getTitle();
		final String description = contentInfoSet.getDescription();

		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		try {
			getUserMarkupCollectionAccessMode(db, userMarkupCollectionId, true);
			
			db
			.update(USERMARKUPCOLLECTION)
			.set(USERMARKUPCOLLECTION.AUTHOR, author)
			.set(USERMARKUPCOLLECTION.PUBLISHER, publisher)
			.set(USERMARKUPCOLLECTION.TITLE, title)
			.set(USERMARKUPCOLLECTION.DESCRIPTION, description)
			.where(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
					.eq(userMarkupCollectionId))
			.execute();

			ContentInfoSet oldContentInfoSet = 
					userMarkupCollectionReference.getContentInfoSet();

			userMarkupCollectionReference.setContentInfoSet(contentInfoSet);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.userMarkupCollectionChanged.name(),
					oldContentInfoSet, userMarkupCollectionReference);
		}
		catch(IOException e) {
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, e);
		}
	}

	List<UserMarkupCollectionReference> getWritableUserMarkupCollectionRefs(
			SourceDocument sd) {
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
	
		return db
		.select(USERMARKUPCOLLECTION.fields())
		.from(USERMARKUPCOLLECTION)
		.join(USER_USERMARKUPCOLLECTION)
			.on(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
					.eq(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID))
			.and(USER_USERMARKUPCOLLECTION.USERID
					.eq(dbRepository.getCurrentUser().getUserId()))
			.and(USER_USERMARKUPCOLLECTION.ACCESSMODE
					.eq(AccessMode.WRITE.getNumericRepresentation())
				.or(USER_USERMARKUPCOLLECTION.OWNER.eq((byte)1)))
		.join(SOURCEDOCUMENT)
			.on(SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(USERMARKUPCOLLECTION.SOURCEDOCUMENTID))
			.and(SOURCEDOCUMENT.LOCALURI.eq(sd.getID()))
		.fetch()
		.map(new UserMarkupCollectionReferenceMapper());
	}
	
	void updateProperty(TagInstance tagInstance, Property property) throws IOException {
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
		byte[] tagInstanceIDbin = 
				idGenerator.catmaIDToUUIDBytes(tagInstance.getUuid());
		try {
			Integer userMarkupCollectionId = db
			.select(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)
			.from(USERMARKUPCOLLECTION)
			.where(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
				.in(db
					.select(TAGREFERENCE.USERMARKUPCOLLECTIONID)
					.from(TAGREFERENCE)
					.join(TAGINSTANCE)
						.on(TAGINSTANCE.TAGINSTANCEID.eq(TAGREFERENCE.TAGINSTANCEID))
						.and(TAGINSTANCE.UUID.eq(tagInstanceIDbin))))
			.fetchOne()
			.map(new IDFieldToIntegerMapper(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID));

			getUserMarkupCollectionAccessMode(db, userMarkupCollectionId, true);
			
			Pair<Integer,Integer> tagInstanceIdpropertyId = db
				.select(TAGINSTANCE.TAGINSTANCEID, PROPERTY.PROPERTYID)
				.from(TAGINSTANCE)
				.leftOuterJoin(PROPERTY)
					.on(PROPERTY.TAGINSTANCEID.eq(TAGINSTANCE.TAGINSTANCEID))
					.and(PROPERTY.PROPERTYDEFINITIONID
							.eq(property.getPropertyDefinition().getId()))
				.where(TAGINSTANCE.UUID.eq(tagInstanceIDbin))
				.fetchOne()
				.map(new IDFieldsToIntegerPairMapper(
						TAGINSTANCE.TAGINSTANCEID, PROPERTY.PROPERTYID));
			Integer propertyId = tagInstanceIdpropertyId.getSecond();
			
			db.beginTransaction();
			if (propertyId != null) {
				DeleteConditionStep<Record> deleteQuery = 
				db
				.delete(PROPERTYVALUE)
				.where(PROPERTYVALUE.PROPERTYID.eq(propertyId));
					
				if (!property.getPropertyValueList().getValues().isEmpty()) {
					deleteQuery = deleteQuery.and(PROPERTYVALUE.VALUE.notIn(property.getPropertyValueList().getValues()));
				}
				
				deleteQuery.execute();
			}
			
			List<String> existingValues = Collections.emptyList();
			if (propertyId != null) {
				existingValues = 
					db
					.select(PROPERTYVALUE.VALUE)
					.from(PROPERTYVALUE)
					.where(PROPERTYVALUE.PROPERTYID.eq(propertyId))
					.fetch()
					.map(new PropertyValueMapper());
			}
			else {
				propertyId = db
					.insertInto(
						PROPERTY,
							PROPERTY.PROPERTYDEFINITIONID,
							PROPERTY.TAGINSTANCEID)
					.values(
						property.getPropertyDefinition().getId(),
						tagInstanceIdpropertyId.getFirst())
					.returning(PROPERTY.PROPERTYID)
					.fetchOne()
					.map(new IDFieldToIntegerMapper(PROPERTY.PROPERTYID));
			}
			
			for(String value : 
				Collections3.getSetDifference(
					property.getPropertyValueList().getValues(), existingValues)) {
				db
				.insertInto(
					PROPERTYVALUE,
						PROPERTYVALUE.PROPERTYID,
						PROPERTYVALUE.VALUE)
				.values(
					propertyId,
					value)
				.execute();
			}
			
			db.commitTransaction();

			dbRepository.getIndexer().updateIndex(tagInstance, property);
		}
		catch (Exception e) {
			db.rollbackTransaction();
			db.close();
			throw new IOException(e);
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	}
}
