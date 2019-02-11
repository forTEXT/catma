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
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYVALUE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.SOURCEDOCUMENT;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGINSTANCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGLIBRARY;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGREFERENCE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USERMARKUPCOLLECTION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_USERMARKUPCOLLECTION;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.sql.DataSource;

import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import de.catma.document.AccessMode;
import de.catma.document.Corpus;
import de.catma.document.repository.Repository;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;
import de.catma.indexer.TagsetDefinitionUpdateLog;
import de.catma.repository.db.MaintenanceSemaphore.Type;
import de.catma.repository.db.jooq.TransactionalDSLContext;
import de.catma.repository.db.mapper.FieldToValueMapper;
import de.catma.repository.db.mapper.IDFieldToIntegerMapper;
import de.catma.repository.db.mapper.PropertyValueMapper;
import de.catma.repository.db.mapper.TagInstanceMapper;
import de.catma.repository.db.mapper.TagReferenceMapper;
import de.catma.repository.db.mapper.UserMarkupCollectionMapper;
import de.catma.repository.db.mapper.UserMarkupCollectionReferenceMapper;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyDefinition.SystemPropertyName;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagsetDefinition;
import de.catma.util.Collections3;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

@Deprecated
class UserMarkupCollectionHandler {
	
	private final static class ColorPropertyValueInfo {
		private TagDefinition tagDefinition;
		private PropertyDefinition propertyDefinition;

		private List<Integer> propertyValueIds = new ArrayList<>();
		
		public ColorPropertyValueInfo(TagDefinition tagDefinition,
				PropertyDefinition propertyDefinition) {
			super();
			this.tagDefinition = tagDefinition;
			this.propertyDefinition = propertyDefinition;
		}
		
		public void addIfChanged(Integer propertyValueId, String currentColorValue) {
			if (!currentColorValue.equals(propertyDefinition.getFirstValue())) {
				propertyValueIds.add(propertyValueId);
			}
		}
		
		public PropertyDefinition getPropertyDefinition() {
			return propertyDefinition;
		}
		
		public List<Integer> getPropertyValueIds() {
			return propertyValueIds;
		}
		
		public TagDefinition getTagDefinition() {
			return tagDefinition;
		}
	}
	
	private static class PropertyCollectionToPropertyDefIdCollection 
		implements Function<Property, Integer> {
		
		@Override
		public Integer apply(Property property) {
			return 0; //property.getPropertyDefinition().getId(); obsolete
		}
	}
	
	private DBRepository dbRepository;
	private IDGenerator idGenerator;
//	private Logger logger = Logger.getLogger(this.getClass().getName());
	private LoadingCache<String, UserMarkupCollection> umcCache;
	private volatile DataSource dataSource;
	
	UserMarkupCollectionHandler(DBRepository dbRepository) {
		this.dbRepository = dbRepository;
		this.idGenerator = new IDGenerator();
		this.umcCache = 
			CacheBuilder
				.newBuilder()
				.maximumSize(10)
				.weakValues()
				.build(new CacheLoader<String, UserMarkupCollection>() {
					@Override
					public UserMarkupCollection load(String userMarkupCollectionId) throws Exception {
						return getUserMarkupCollection(userMarkupCollectionId);
					}
				});
		this.dataSource = CatmaDataSourceName.CATMADS.getDataSource();
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
					USERMARKUPCOLLECTION.TAGLIBRARYID,
					USERMARKUPCOLLECTION.UUID)
			.values(
				name,
				sourceDocumentId,
				tagLibraryId,
				idGenerator.catmaIDToUUIDBytes(idGenerator.generate()))
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
							null,
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
		UserMarkupCollectionSerializationHandler userMarkupCollectionSerializationHandler = 
				dbRepository.getSerializationHandlerFactory().getUserMarkupCollectionSerializationHandler();
		importUserMarkupCollection(inputStream, sourceDocument, userMarkupCollectionSerializationHandler);
	}
	
	void importUserMarkupCollection(
			InputStream inputStream,
			final SourceDocument sourceDocument, 
			UserMarkupCollectionSerializationHandler userMarkupCollectionSerializationHandler) 
					throws IOException {
		
		dbRepository.setTagManagerListenersEnabled(false);
		UserMarkupCollection umc = null;
		try {
			umc = userMarkupCollectionSerializationHandler.deserialize(null, inputStream);
		}
		finally {
			dbRepository.setTagManagerListenersEnabled(true);
		}
		
		// the import is not a transaction by intention
		// import can become a pretty long running operation
		// and would block other users 
		// the last DB action prior to indexing 
		// is the linking between the umc and the user
		// so in case of failure the user won't have access to the
		// corrupt umc and the DB cleaner job will take care of it
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		
		importAndIndexUserMarkupCollection(db, umc, sourceDocument, null);
		
		umcCache.put(umc.getId(), umc);
	}
	
	
	private void importAndIndexUserMarkupCollection(
			DSLContext db, 
			final UserMarkupCollection umc, 
			final SourceDocument sourceDocument, final Integer corpusId) {
		
		MaintenanceSemaphore mSem = new MaintenanceSemaphore(Type.IMPORT);
		try {
			if (!mSem.hasAccess()) {
				throw new IllegalStateException(
					"Currently we cannot import the collection, couldn't get sem access!");	
			}
			
			dbRepository.getDbTagLibraryHandler().importTagLibrary(
				db, umc.getTagLibrary(), false);
			
			UserMarkupCollectionReference umcRef = importUserMarkupCollection(
					db, umc, sourceDocument);

			mSem.release();
			
			if (corpusId != null) {
				db
				.insertInto(
					CORPUS_USERMARKUPCOLLECTION,
						CORPUS_USERMARKUPCOLLECTION.CORPUSID,
						CORPUS_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)
				.values(
					corpusId,
					Integer.valueOf(umcRef.getId()))
				.execute();
			}
			
			// index the imported collection
			dbRepository.getIndexer().index(
					umc.getTagReferences(), 
					sourceDocument.getID(),
					umc.getId(),
					umc.getTagLibrary());
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
				RepositoryChangeEvent.userMarkupCollectionChanged.name(),
				null, new Pair<UserMarkupCollectionReference, SourceDocument>(
						umcRef, sourceDocument));

		}
		catch (Exception dae) {
			mSem.release();

			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					new IOException(dae));				
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
				USERMARKUPCOLLECTION.AUTHOR,
				USERMARKUPCOLLECTION.DESCRIPTION,
				USERMARKUPCOLLECTION.PUBLISHER,
				USERMARKUPCOLLECTION.SOURCEDOCUMENTID,
				USERMARKUPCOLLECTION.TAGLIBRARYID,
				USERMARKUPCOLLECTION.UUID)
		.values(
			umc.getName(),
			umc.getContentInfoSet().getAuthor(),
			umc.getContentInfoSet().getDescription(),
			umc.getContentInfoSet().getPublisher(),
			sourceDocumentId,
			Integer.valueOf(umc.getTagLibrary().getId()),
			idGenerator.catmaIDToUUIDBytes(idGenerator.generate()))
		.returning(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID)
		.fetchOne()
		.map(new IDFieldToIntegerMapper(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID));
		

//		umc.setId(String.valueOf(userMarkupCollectionId));
		//obsolete
		
		addTagReferences(db, umc);

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
		
		UserMarkupCollectionReference umcRef = 
				new UserMarkupCollectionReference(
						String.valueOf(userMarkupCollectionId), 
						null,
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

				TagDefinition tDef = null;
//					umc.getTagLibrary().getTagDefinition(
//						tr.getTagInstance().getTagDefinition().getUuid());
				
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
//				for (Property p : ti.getSystemProperties()) {
//					
//					if (p.getPropertyDefinitionId().equals(
//							PropertyDefinition.SystemPropertyName.catma_markupauthor.name())) {
//						p.setPropertyValueList(Collections.singleton(
//								dbRepository.getCurrentUser().getIdentifier()));
//						authorPresent = true;
//					}
//					
//					Integer propertyId = db
//						.insertInto(
//							PROPERTY,
//								PROPERTY.PROPERTYDEFINITIONID,
//								PROPERTY.TAGINSTANCEID)
//						.values(
//							0, //p.getPropertyDefinition().getId(), obsolete
//							curTagInstanceId)
//						.returning(PROPERTY.PROPERTYID)
//						.fetchOne()
//						.map(new IDFieldToIntegerMapper(PROPERTY.PROPERTYID));
//				
//					
//					for (String value : p.getPropertyValueList()) {
//						pValueInsertBatch.bind(propertyId, value);
//					}
//					
//				}
				
//				if (!authorPresent) {
//					Property authorNameProp = 
//						new Property(
//							PropertyDefinition.SystemPropertyName.catma_markupauthor.name(),
//							Collections.singleton(
//								dbRepository.getCurrentUser().getIdentifier()));
//					ti.addSystemProperty(authorNameProp);
//					
//					Integer propertyId = db
//						.insertInto(
//							PROPERTY,
//								PROPERTY.PROPERTYDEFINITIONID,
//								PROPERTY.TAGINSTANCEID)
//						.values(
//							0, //authorNameProp.getPropertyDefinition().getId(), obsolete
//							curTagInstanceId)
//						.returning(PROPERTY.PROPERTYID)
//						.fetchOne()
//						.map(new IDFieldToIntegerMapper(PROPERTY.PROPERTYID));
//
//					pValueInsertBatch.bind(
//						propertyId, 
//						authorNameProp.getFirstValue());
				}
					 
//				for (Property p : ti.getUserDefinedProperties()) {
//					if (!p.getPropertyValueList().isEmpty()) {
//						Integer propertyId = db
//							.insertInto(
//								PROPERTY,
//									PROPERTY.PROPERTYDEFINITIONID,
//									PROPERTY.TAGINSTANCEID)
//							.values(
//								0, //p.getPropertyDefinition().getId(), obsolete
//								curTagInstanceId)
//							.returning(PROPERTY.PROPERTYID)
//							.fetchOne()
//							.map(new IDFieldToIntegerMapper(PROPERTY.PROPERTYID));
//					
//						
//						for (String value : p.getPropertyValueList()) {
//							pValueInsertBatch.bind(propertyId, value);
//						}
//					}
//				}
//
//				pValueInsertBatch.execute();
			}
			
//			db
//			.insertInto(
//				TAGREFERENCE,
//					TAGREFERENCE.CHARACTERSTART,
//					TAGREFERENCE.CHARACTEREND,
//					TAGREFERENCE.USERMARKUPCOLLECTIONID,
//					TAGREFERENCE.TAGINSTANCEID)
//			.values(
//				tr.getRange().getStartPoint(),
//				tr.getRange().getEndPoint(),
//				Integer.valueOf(umc.getId()),
//				curTagInstanceId)
//			.execute();
//		}
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

			db
			.delete(USER_USERMARKUPCOLLECTION)
			.where(USER_USERMARKUPCOLLECTION.USER_USERMARKUPCOLLECTIOID.eq(userUmcId))
			.execute();
			
			db.commitTransaction();

			SourceDocument sd = 
					dbRepository.getSourceDocument(userMarkupCollectionReference);
			sd.removeUserMarkupCollectionReference(userMarkupCollectionReference);
			
			umcCache.invalidate(userMarkupCollectionReference.getId());
			
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
		
		if (refresh) {
			umcCache.invalidate(userMarkupCollectionReference.getId());
		}
		
		try {
			return umcCache.get(userMarkupCollectionReference.getId());
		} catch (ExecutionException e) {
			throw new IOException(e);
		}
	}
	
	private UserMarkupCollection getUserMarkupCollection(String userMarkupCollectionIdString) throws IOException {
		Integer userMarkupCollectionId = 
				Integer.valueOf(userMarkupCollectionIdString);
		
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		
		AccessMode accessMode = 
			getUserMarkupCollectionAccessMode(db, userMarkupCollectionId, false);
		
		UserMarkupCollection userMarkupCollection = getUserMarkupCollection(db, userMarkupCollectionId, accessMode);
		
		
		return userMarkupCollection;

	}
	

	@SuppressWarnings("unchecked")
	private UserMarkupCollection getUserMarkupCollection(DSLContext db, Integer userMarkupCollectionId, AccessMode accessMode) 
			throws IllegalArgumentException, IOException {
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

		List<Integer> tagInstanceIDs = db 
		.select(TAGREFERENCE.TAGINSTANCEID)
		.from(TAGREFERENCE)
		.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))
		.fetch()
		.map(new IDFieldToIntegerMapper(TAGREFERENCE.TAGINSTANCEID));

		Map<Integer, Result<Record>> propertyValueRecordsByPropertyId = Collections.emptyMap();
		
		if (!tagInstanceIDs.isEmpty()) {
			propertyValueRecordsByPropertyId = db
			.select(PROPERTYVALUE.fields())
			.from(PROPERTYVALUE)
			.join(PROPERTY)
				.on(PROPERTY.PROPERTYID.eq(PROPERTYVALUE.PROPERTYID))
				.and(PROPERTY.TAGINSTANCEID.in(tagInstanceIDs))
			.fetchGroups(PROPERTYVALUE.PROPERTYID);
		}
		
		Map<Integer, Result<Record>> propertyRecordsByTagInstanceId =
				Collections.emptyMap();
		
		if (!tagInstanceIDs.isEmpty()) {
			propertyRecordsByTagInstanceId = db
				.select()
				.from(PROPERTY)
				.join(PROPERTYDEFINITION)
					.on(PROPERTYDEFINITION.PROPERTYDEFINITIONID
							.eq(PROPERTY.PROPERTYDEFINITIONID))
				.where(PROPERTY.TAGINSTANCEID.in(tagInstanceIDs))
				.fetchGroups(TAGINSTANCE.TAGINSTANCEID);
		}

		List<TagInstance> tagInstances = Collections.emptyList();
		
		if (!tagInstanceIDs.isEmpty()) {
			tagInstances = db
				.select()
				.from(TAGINSTANCE)
				.join(TAGDEFINITION)
					.on(TAGDEFINITION.TAGDEFINITIONID.eq(TAGINSTANCE.TAGDEFINITIONID))
				.where(TAGINSTANCE.TAGINSTANCEID.in(tagInstanceIDs))
				.fetch()
				.map(
					new TagInstanceMapper(
						tagLibrary, 
						propertyRecordsByTagInstanceId, 
						propertyValueRecordsByPropertyId));
		}
		
		List<TagReference> tagReferences = Lists.newArrayList();
		
		if (!tagInstanceIDs.isEmpty()) {
			tagReferences = db
				.select(Collections3.getUnion(TAGREFERENCE.fields(), TAGINSTANCE.UUID))
				.from(TAGREFERENCE)
				.join(TAGINSTANCE)
					.on(TAGINSTANCE.TAGINSTANCEID.eq(TAGREFERENCE.TAGINSTANCEID))
				.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))
				.fetch()
				.map(new TagReferenceMapper(
						localSourceDocURI, tagInstances,
						idGenerator.uuidBytesToCatmaID(umcRecord.getValue(USERMARKUPCOLLECTION.UUID)))
				);
		}
		
		return	new UserMarkupCollectionMapper(
				tagLibrary, tagReferences, accessMode).map(umcRecord);
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
		
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			
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
			
				// establish mapping color propertyDefinitionId to Info object
				Map<Integer, ColorPropertyValueInfo> colorPropertyDefIdToValueInfoMap = new HashMap<>();
				for (TagDefinition td : 
						userMarkupCollection.getTagLibrary().getTagsetDefinition(
								tagsetDefinition.getUuid())) {
					
					PropertyDefinition colorPropertyDefinition = 
							td.getPropertyDefinition(
									SystemPropertyName.catma_displaycolor.name());
					
					ColorPropertyValueInfo colorPropertyValueInfo = 
							new ColorPropertyValueInfo(td, colorPropertyDefinition);
					
					colorPropertyDefIdToValueInfoMap.put(
							0, //colorPropertyDefinition.getId(), obsolete
							colorPropertyValueInfo);
				}
				
				// get all propertyValueIDs and their propertyDefinitionIDs
				// for color properties in the current collection
				Result<Record3<Integer, String, Integer>> colorPropertyValueRecords = db
				.select(PROPERTYVALUE.PROPERTYVALUEID, PROPERTYVALUE.VALUE, PROPERTYDEFINITION.PROPERTYDEFINITIONID)
				.from(PROPERTYVALUE)
				.join(PROPERTY)
					.on(PROPERTY.PROPERTYID.eq(PROPERTYVALUE.PROPERTYID))
				.join(PROPERTYDEFINITION)
					.on(PROPERTYDEFINITION.PROPERTYDEFINITIONID
							.eq(PROPERTY.PROPERTYDEFINITIONID))
				.join(TAGINSTANCE)
					.on(TAGINSTANCE.TAGINSTANCEID.eq(PROPERTY.TAGINSTANCEID))
				.join(TAGREFERENCE)
					.on(TAGREFERENCE.TAGINSTANCEID.eq(TAGINSTANCE.TAGINSTANCEID))
					.and(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))
				.where(PROPERTYDEFINITION.PROPERTYDEFINITIONID.in(colorPropertyDefIdToValueInfoMap.keySet()))
				.fetch();
				
				// fill with corresponding propertyValueId and current color value
				for (Record3<Integer, String, Integer> colorPropertyValueRecord : colorPropertyValueRecords) {
					ColorPropertyValueInfo colorPropertyValueInfo = 
							colorPropertyDefIdToValueInfoMap
							.get(colorPropertyValueRecord.value3());
					
					colorPropertyValueInfo.addIfChanged(
						colorPropertyValueRecord.value1(), colorPropertyValueRecord.value2());
				}

				for (TagReference tr : userMarkupCollection.getTagReferences()) {
					relevantTagInstanceUUIDs.add(
							idGenerator.catmaIDToUUIDBytes(tr.getTagInstanceID()));
					relevantTagInstances.add(tr.getTagInstance());
				}
 
				SelectConditionStep<?> selectObsoleteTagInstanceIdsQuery = null;
					
				// get list of TagInstances which are no longer present in the umc
				// to handle deleted TagInstances (see further below)
				if (!relevantTagInstances.isEmpty()) {
					selectObsoleteTagInstanceIdsQuery = db
						.selectDistinct(TAGINSTANCE.TAGINSTANCEID)
						.from(TAGINSTANCE)
						.join(TAGREFERENCE)
							.on(TAGREFERENCE.TAGINSTANCEID.eq(TAGINSTANCE.TAGINSTANCEID))
							.and(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId))
						.where(TAGINSTANCE.UUID.notIn(relevantTagInstanceUUIDs));
				}
				else {
					selectObsoleteTagInstanceIdsQuery = db
						.selectDistinct(TAGREFERENCE.TAGINSTANCEID)
						.from(TAGREFERENCE)
						.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionId));
				}
				
				List<Integer> obsoleteTagInstanceIds = 
					selectObsoleteTagInstanceIdsQuery
					.fetch()
					.map(new IDFieldToIntegerMapper(TAGINSTANCE.TAGINSTANCEID));
				
				// collect tagInstanceUUIDs and valid user def prop IDs 
				// to handle deleted properties (see further below)
				List<byte[]> tagInstanceUUIDbinList = new ArrayList<byte[]>();
				Set<Integer> relevantUserDefPropertyIdList = new HashSet<Integer>();
				
				for (TagInstance ti : relevantTagInstances) {
					// get all valid propertyDef IDs
					Collection<Integer> relevantUserDefPropertyDefIds = 
							Collections2.transform(ti.getUserDefinedProperties(),
									new Function<Property, Integer>() {
								public Integer apply(
										Property property) {
									return 0; // property.getPropertyDefinition().getId(); obsolete
								}});
					relevantUserDefPropertyIdList.addAll(relevantUserDefPropertyDefIds);
					
					byte[] tagInstanceUUIDbin = 
							idGenerator.catmaIDToUUIDBytes(ti.getUuid()); 
					// remember TagInstance UUID
					tagInstanceUUIDbinList.add(tagInstanceUUIDbin);
				}
					
				List<Integer> obsoletePropertyIds = Collections.emptyList();
				if (!obsoleteTagInstanceIds.isEmpty()) {
					obsoletePropertyIds = db
						.select(PROPERTY.PROPERTYID)
						.from(PROPERTY)
						.where(PROPERTY.TAGINSTANCEID.in(obsoleteTagInstanceIds))
						.fetch()
						.map(new IDFieldToIntegerMapper(PROPERTY.PROPERTYID));
				}
				

				// handle deleted TagInstances/TagReferences and their content
				if (!obsoleteTagInstanceIds.isEmpty()) {
					try {
						db.beginTransaction();
						
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
								.in(obsoletePropertyIds)),
							// delete the properties of obsolete tag instances
							db
							.delete(PROPERTY)
							.where(PROPERTY.TAGINSTANCEID.in(obsoleteTagInstanceIds)),
							// delete obsolete taginstances
							db
							.delete(TAGINSTANCE)
							.where(TAGINSTANCE.TAGINSTANCEID.in(obsoleteTagInstanceIds)))
						.execute();
						
						db.commitTransaction();
					}
					catch (Exception dae) {
						db.rollbackTransaction();
						db.close();
						throw new IOException(dae);
					}
				}

				
				// handle deleted properties
				if (relevantUserDefPropertyIdList.isEmpty()) { // no restriction->delete all user def properties+values
					//collect propertyIDs
					List<Integer> toBeDeletedPropertyIDs = db
						.select(PROPERTY.PROPERTYID)
						.from(PROPERTY)
						.join(PROPERTYDEFINITION)
							.on(PROPERTYDEFINITION.PROPERTYDEFINITIONID
									.eq(PROPERTY.PROPERTYDEFINITIONID))
							.and(PROPERTYDEFINITION.SYSTEMPROPERTY.eq((byte)0))
						.join(TAGINSTANCE)
							.on(TAGINSTANCE.TAGINSTANCEID
									.eq(PROPERTY.TAGINSTANCEID))
							.and(TAGINSTANCE.UUID.in(tagInstanceUUIDbinList))
						.fetch()
						.map(new IDFieldToIntegerMapper(PROPERTY.PROPERTYID));
					
					if (!toBeDeletedPropertyIDs.isEmpty()) {
						try {
							db.beginTransaction();
							db.batch(
								// delete property values of properties
								db
								.delete(PROPERTYVALUE)
								.where(PROPERTYVALUE.PROPERTYID
									.in(toBeDeletedPropertyIDs)),
								// delete properties 
								db
								.delete(PROPERTY)
								.where(PROPERTY.PROPERTYID
									.in(toBeDeletedPropertyIDs))
							)
							.execute();
							db.commitTransaction();
						}
						catch (Exception dae) {
							db.rollbackTransaction();
							db.close();
							throw new IOException(dae);
						}							
					}
				}
				else { // delete all properties+values which are no longer relevant
					//collect propertyIDs
					List<Integer> toBeDeletedPropertyIDs = db
						.select(PROPERTY.PROPERTYID)
						.from(PROPERTY)
						.join(PROPERTYDEFINITION)
							.on(PROPERTYDEFINITION.PROPERTYDEFINITIONID
									.eq(PROPERTY.PROPERTYDEFINITIONID))
							.and(PROPERTYDEFINITION.SYSTEMPROPERTY.eq((byte)0))
							.and(PROPERTYDEFINITION.PROPERTYDEFINITIONID
									.notIn(relevantUserDefPropertyIdList))
						.join(TAGINSTANCE)
							.on(TAGINSTANCE.TAGINSTANCEID
									.eq(PROPERTY.TAGINSTANCEID))
							.and(TAGINSTANCE.UUID.in(tagInstanceUUIDbinList))
						.fetch()
						.map(new IDFieldToIntegerMapper(PROPERTY.PROPERTYID));
	
					if (!toBeDeletedPropertyIDs.isEmpty()) {
						try {
							db.beginTransaction();
							db.batch(
								// delete property values of properties which are no longer present
								db
								.delete(PROPERTYVALUE)
								.where(PROPERTYVALUE.PROPERTYID
									.in(toBeDeletedPropertyIDs)),
								// delete properties which are no longer present
								db
								.delete(PROPERTY)
								.where(PROPERTY.PROPERTYID.in(
										toBeDeletedPropertyIDs))
							)
							.execute();
							db.commitTransaction();
						}
						catch (Exception dae) {
							db.rollbackTransaction();
							db.close();
							throw new IOException(dae);
						}						
					}

				}

				// update all modified color property values
				for (ColorPropertyValueInfo colorPropertyValueInfo : colorPropertyDefIdToValueInfoMap.values()) {
					if (!colorPropertyValueInfo.getPropertyValueIds().isEmpty()) {
						db
						.update(PROPERTYVALUE)
						.set(
							PROPERTYVALUE.VALUE,
							colorPropertyValueInfo.getPropertyDefinition().getFirstValue())
						.where(PROPERTYVALUE.PROPERTYVALUEID.in(colorPropertyValueInfo.getPropertyValueIds()))
						.execute();
					}
				}
				
				// addition of new properties with default values is not 
				// represented within the umc on the database side
				// only the tag library will be affected (see below)
				
				try {
					db.beginTransaction();
					
					// handle changes in the tag library
					final TagsetDefinitionUpdateLog tagsetDefinitionUpdateLog = 
							dbRepository.getDbTagLibraryHandler().updateTagsetDefinition(
									db, tagLibrary,
									tagLibrary.getTagsetDefinition(tagsetDefinition.getUuid()));
					
					db.commitTransaction();
					
					// add changed color properties to update log for reindexing
					for (ColorPropertyValueInfo colorPropertyValueInfo : colorPropertyDefIdToValueInfoMap.values())  {
						if (!colorPropertyValueInfo.getPropertyValueIds().isEmpty()) {
							tagsetDefinitionUpdateLog.addUpdatedPropertyDefinition(
								colorPropertyValueInfo.getPropertyDefinition().getName(), 
								colorPropertyValueInfo.getTagDefinition().getUuid());
						}
					}
					
					// reindex if necessary
					if (!tagsetDefinitionUpdateLog.isEmpty()) {
						dbRepository.getIndexer().reindex(
								tagsetDefinition, 
								tagsetDefinitionUpdateLog,
								userMarkupCollection);
					}
				}
				catch (Exception dae) {
					db.rollbackTransaction();
					db.close();
					throw new IOException(dae);
				}						
				
			}
			
			db.close();
		}
		catch (Exception dae) {
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
				.where(TAGREFERENCE.TAGINSTANCEID.in(relevantTagInstanceIDs)))
			.execute();

			db
			.delete(TAGINSTANCE)
			.where(TAGINSTANCE.TAGINSTANCEID.in(relevantTagInstanceIDs))
			.execute();

			db.commitTransaction();

			dbRepository.getIndexer().removeTagReferences(tagReferences);
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
	
	void updateProperty(TagInstance tagInstance, Collection<Property> properties) throws IOException {
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
			
			Integer tagInstanceId = db
				.select(TAGINSTANCE.TAGINSTANCEID)
				.from(TAGINSTANCE)
				.where(TAGINSTANCE.UUID.eq(tagInstanceIDbin))
				.fetchOne()
				.map(new IDFieldToIntegerMapper(
						TAGINSTANCE.TAGINSTANCEID));

			
			Map<Integer,Integer> propertyDefintionIdToProperyIdMap = db 
				.select(PROPERTY.PROPERTYDEFINITIONID, PROPERTY.PROPERTYID)
				.from(PROPERTY)
				.where(PROPERTY.TAGINSTANCEID.eq(tagInstanceId))
				.and(PROPERTY.PROPERTYDEFINITIONID.in(
					Collections2.transform(properties, new PropertyCollectionToPropertyDefIdCollection())))
				.fetchMap(PROPERTY.PROPERTYDEFINITIONID, PROPERTY.PROPERTYID);
				
			
			db.beginTransaction();
			for (Property property : properties) {
				Integer propertyId = 0; 
//						propertyDefintionIdToProperyIdMap.get(property.getPropertyDefinition().getId()); obsolete
				
				if (propertyId != null) {
					DeleteConditionStep<Record> deleteQuery = 
					db
					.delete(PROPERTYVALUE)
					.where(PROPERTYVALUE.PROPERTYID.eq(propertyId));
						
					if (!property.getPropertyValueList().isEmpty()) {
						deleteQuery = deleteQuery.and(PROPERTYVALUE.VALUE.notIn(property.getPropertyValueList()));
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
							0, //property.getPropertyDefinition().getId(), obsolete
							tagInstanceId)
						.returning(PROPERTY.PROPERTYID)
						.fetchOne()
						.map(new IDFieldToIntegerMapper(PROPERTY.PROPERTYID));
				}
				
				for(String value : 
					Collections3.getSetDifference(
						property.getPropertyValueList(), existingValues)) {
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
			}			
			db.commitTransaction();

			dbRepository.getIndexer().updateIndex(tagInstance, properties);
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

	public int getNewUserMarkupCollectionRefs(Corpus corpus) {
		
		Set<Integer> knownUserMarkupCollectionIds = new HashSet<>();
		
		for (UserMarkupCollectionReference ref : corpus.getUserMarkupCollectionRefs()) {
			knownUserMarkupCollectionIds.add(Integer.valueOf(ref.getId()));
		}
		
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		
		Map<String,Record> sourceDocIdToMarkupCollRecord = null;
		//TODO: refactor when upgrading on newer jooq version
		if (knownUserMarkupCollectionIds.isEmpty()) {
			sourceDocIdToMarkupCollRecord = db
				.select()
				.from(USERMARKUPCOLLECTION)
				.join(USER_USERMARKUPCOLLECTION)
					.on(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
							.eq(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID))
					.and(USER_USERMARKUPCOLLECTION.USERID
							.eq(dbRepository.getCurrentUser().getUserId()))
				.join(CORPUS_USERMARKUPCOLLECTION)
					.on(CORPUS_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
							.eq(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID))
					.and(CORPUS_USERMARKUPCOLLECTION.CORPUSID
							.eq(Integer.valueOf(corpus.getId())))
				.join(SOURCEDOCUMENT)
					.on(SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(USERMARKUPCOLLECTION.SOURCEDOCUMENTID))
				.fetchMap(SOURCEDOCUMENT.LOCALURI);
		}
		else {
			sourceDocIdToMarkupCollRecord = db
				.select()
				.from(USERMARKUPCOLLECTION)
				.join(USER_USERMARKUPCOLLECTION)
					.on(USER_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
							.eq(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID))
					.and(USER_USERMARKUPCOLLECTION.USERID
							.eq(dbRepository.getCurrentUser().getUserId()))
				.join(CORPUS_USERMARKUPCOLLECTION)
					.on(CORPUS_USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
							.eq(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID))
					.and(CORPUS_USERMARKUPCOLLECTION.CORPUSID
							.eq(Integer.valueOf(corpus.getId())))
				.join(SOURCEDOCUMENT)
					.on(SOURCEDOCUMENT.SOURCEDOCUMENTID.eq(USERMARKUPCOLLECTION.SOURCEDOCUMENTID))
				.where(USERMARKUPCOLLECTION.USERMARKUPCOLLECTIONID
						.notIn(knownUserMarkupCollectionIds))
				.fetchMap(SOURCEDOCUMENT.LOCALURI);
		}
		
		UserMarkupCollectionReferenceMapper mapper = new UserMarkupCollectionReferenceMapper();
		for (Map.Entry<String, Record> mappedRecord : sourceDocIdToMarkupCollRecord.entrySet()) {
			String sourceDocId = mappedRecord.getKey();
			Record umcRecord = mappedRecord.getValue();
			
			UserMarkupCollectionReference umcRef = mapper.map(umcRecord);
			SourceDocument sd = dbRepository.getDbSourceDocumentHandler().getSourceDocument(sourceDocId);
			
			sd.addUserMarkupCollectionReference(umcRef);
			corpus.addUserMarkupCollectionReference(umcRef);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.userMarkupCollectionChanged.name(),
					null, new Pair<UserMarkupCollectionReference, SourceDocument>(
							umcRef, sd));
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
				Repository.RepositoryChangeEvent.corpusChanged.name(),
				umcRef, corpus);
		}
		
		return sourceDocIdToMarkupCollRecord.size();
	}

	void importUserMarkupCollection(
			DSLContext db, 
			Integer userMarkupCollectionId, 
			SourceDocument sd, Integer corpusId) throws IllegalArgumentException, IOException {
		
		UserMarkupCollection userMarkupCollection = 
			getUserMarkupCollection(db, userMarkupCollectionId, AccessMode.WRITE);
		
		try {
			UserMarkupCollection copiedUmc = new UserMarkupCollection(userMarkupCollection);
			
			importAndIndexUserMarkupCollection(db, copiedUmc, sd, corpusId);
		}
		catch (URISyntaxException ue) {
			throw new IOException(ue);
		}
	}
}
