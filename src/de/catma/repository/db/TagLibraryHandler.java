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

import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.PROPERTYDEF_POSSIBLEVALUE;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGLIBRARY;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.TAGSETDEFINITION;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_TAGLIBRARY;
import static de.catma.repository.db.jooqgen.catmarepository.Tables.USER_USERMARKUPCOLLECTION;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import de.catma.document.AccessMode;
import de.catma.document.repository.Repository.RepositoryChangeEvent;
import de.catma.document.source.ContentInfoSet;
import de.catma.indexer.TagsetDefinitionUpdateLog;
import de.catma.repository.db.jooq.ResultUtil;
import de.catma.repository.db.jooq.SqlTimestamp;
import de.catma.repository.db.jooq.TransactionalDSLContext;
import de.catma.repository.db.jooq.UUIDFieldKeyMapper;
import de.catma.repository.db.jooq.UUIDtoByteMapper;
import de.catma.repository.db.mapper.FieldToValueMapper;
import de.catma.repository.db.mapper.IDFieldToIntegerMapper;
import de.catma.repository.db.mapper.TagLibraryReferenceMapper;
import de.catma.repository.db.mapper.TagsetDefinitionMapper;
import de.catma.repository.db.mapper.UUIDByteToStringFieldMapper;
import de.catma.serialization.TagLibrarySerializationHandler;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyPossibleValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagLibraryReference;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.util.Collections3;
import de.catma.util.IDGenerator;

class TagLibraryHandler {

	private static final Logger LOGGER = Logger.getLogger(TagLibraryHandler.class.getName());

	private static class PropertyDefinitionToUUID implements Function<PropertyDefinition, String> {
		
		public String apply(PropertyDefinition pd) {
			return pd.getUuid();
		}
	}
	
	private static class TagLibRefNameComparator implements Comparator<TagLibraryReference> {
		public int compare(TagLibraryReference o1, TagLibraryReference o2) {
			return o1.toString().compareToIgnoreCase(o2.toString());
		}
	}
	
	private DBRepository dbRepository;
	private HashMap<String, TagLibraryReference> tagLibraryReferencesById;
	private IDGenerator idGenerator;
	private DataSource dataSource;

	public TagLibraryHandler(
		DBRepository dbRepository, IDGenerator idGenerator) {
		this.dbRepository = dbRepository;
		this.idGenerator = idGenerator;
		tagLibraryReferencesById = new HashMap<String, TagLibraryReference>();
		this.dataSource = CatmaDataSourceName.CATMADS.getDataSource();
	}

	public void createTagLibrary(String name) throws IOException {
		
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			db.beginTransaction();
			
			Integer tagLibraryId = 
					createTagLibrary(db, name, null, null, null, true);
			
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
	
	private Integer createTagLibrary(DSLContext db, String title,
			String author, String description, String publisher, boolean independent) {
		Integer tagLibraryId = db
		.insertInto(
			TAGLIBRARY,
				TAGLIBRARY.TITLE,
				TAGLIBRARY.AUTHOR,
				TAGLIBRARY.DESCRIPTION,
				TAGLIBRARY.PUBLISHER, 
				TAGLIBRARY.INDEPENDENT)
		.values(
			title, 
			author, 
			description, 
			publisher,
			(byte)(independent?1:0))
		.returning(TAGLIBRARY.TAGLIBRARYID)
		.fetchOne()
		.map(new IDFieldToIntegerMapper(TAGLIBRARY.TAGLIBRARYID));
		
		return tagLibraryId;
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
		
		Map<String, List<Record>> possValuesByPropDefUuid = ResultUtil.asGroups(
			new UUIDFieldKeyMapper(PROPERTYDEFINITION.UUID), db
			.select(Collections3.getUnion(
					PROPERTYDEF_POSSIBLEVALUE.fields(), 
					PROPERTYDEFINITION.fields()))
			.from(PROPERTYDEF_POSSIBLEVALUE)
			.join(PROPERTYDEFINITION)
				.on(PROPERTYDEFINITION.PROPERTYDEFINITIONID
						.eq(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID))
			.join(TAGDEFINITION)
				.on(TAGDEFINITION.TAGDEFINITIONID.eq(PROPERTYDEFINITION.TAGDEFINITIONID))
			.join(TAGSETDEFINITION)
				.on(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(TAGDEFINITION.TAGSETDEFINITIONID))
				.and(TAGSETDEFINITION.TAGLIBRARYID.eq(tagLibraryId))
			.fetch());
		
		Map<String, List<Record>> propertyDefByTagDefUuid = ResultUtil.asGroups(
			new UUIDFieldKeyMapper(TAGDEFINITION.UUID), db
			.select(Collections3.getUnion(PROPERTYDEFINITION.fields(), TAGDEFINITION.fields()))
			.from(PROPERTYDEFINITION)
			.join(TAGDEFINITION)
				.on(TAGDEFINITION.TAGDEFINITIONID.eq(PROPERTYDEFINITION.TAGDEFINITIONID))
			.join(TAGSETDEFINITION)
				.on(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(TAGDEFINITION.TAGSETDEFINITIONID))
				.and(TAGSETDEFINITION.TAGLIBRARYID.eq(tagLibraryId))
			.fetch());
		
	
		Map<String, List<Record>> tagDefByTagsetDefUuid = ResultUtil.asGroups(
			new UUIDFieldKeyMapper(TAGSETDEFINITION.UUID), db
			.select()
			.from(TAGDEFINITION)
			.join(TAGSETDEFINITION)
				.on(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(TAGDEFINITION.TAGSETDEFINITIONID))
				.and(TAGSETDEFINITION.TAGLIBRARYID.eq(tagLibraryId))
			.fetch());
		
		List<TagsetDefinition> tagsetDefinitions = db
		.select(TAGSETDEFINITION.fields())
		.from(TAGSETDEFINITION)
		.where(TAGSETDEFINITION.TAGLIBRARYID.eq(tagLibraryId))
		.fetch()
		.map(new TagsetDefinitionMapper(
			tagDefByTagsetDefUuid, 
			propertyDefByTagDefUuid, 
			possValuesByPropDefUuid));
		
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
		
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			importTagLibrary(db, tagLibrary, true);
			
			dbRepository.setTagManagerListenersEnabled(true);
			
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
		catch (Exception dae) {
			db.rollbackTransaction();
			db.close();
			
			dbRepository.setTagManagerListenersEnabled(true);

			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					dae);				
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}

	}
		

	void importTagLibrary(
			DSLContext db,
			final TagLibrary tagLibrary, 
			final boolean independent) {
		
		Integer tagLibraryId = 
			createTagLibrary(
				db, 
				tagLibrary.getName(), 
				tagLibrary.getContentInfoSet().getAuthor(), 
				tagLibrary.getContentInfoSet().getDescription(), 
				tagLibrary.getContentInfoSet().getPublisher(),
				independent);
		tagLibrary.setId(String.valueOf(tagLibraryId));
		
		for (TagsetDefinition tagsetDefinition : tagLibrary) {
			createDeepTagsetDefinition(db, tagsetDefinition, tagLibraryId);
		}
		
		if (independent) {
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
		}
	}
	
	private void createDeepTagsetDefinition(
			DSLContext db, TagsetDefinition tagsetDefinition, Integer tagLibraryId) {
		
		Integer tagsetDefinitionId = db
		.insertInto(
			TAGSETDEFINITION,
				TAGSETDEFINITION.UUID,
				TAGSETDEFINITION.VERSION,
				TAGSETDEFINITION.NAME,
				TAGSETDEFINITION.TAGLIBRARYID)
		.values(
			idGenerator.catmaIDToUUIDBytes(tagsetDefinition.getUuid()),
			SqlTimestamp.from(tagsetDefinition.getVersion().getDate()),
			tagsetDefinition.getName(), 
			tagLibraryId)
		.returning(TAGSETDEFINITION.TAGSETDEFINITIONID)
		.fetchOne()
		.map(new IDFieldToIntegerMapper(TAGSETDEFINITION.TAGSETDEFINITIONID));
		tagsetDefinition.setId(tagsetDefinitionId);
		
		if (!tagsetDefinition.isEmpty()) {

			for (TagDefinition tagDefinition : tagsetDefinition) {
				createDeepTagDefinition(
						db,
						tagDefinition,
						tagsetDefinitionId);
			}
			
			maintainTagDefinitionHierarchy(db, tagsetDefinition);
		}
	}

	private void maintainTagDefinitionHierarchy(DSLContext db, TagsetDefinition tagsetDefinition) {
		// maintain tagdef hierarchy

		int tagsetDefinitionId = tagsetDefinition.getId();
		
		de.catma.repository.db.jooqgen.catmarepository.tables.Tagdefinition td1 = 
				TAGDEFINITION.as("td1"); 
		de.catma.repository.db.jooqgen.catmarepository.tables.Tagdefinition td2 = 
				TAGDEFINITION.as("td2");
		db
		.update(td1
		.join(td2)
			.on(td2.UUID.eq(td1.PARENTUUID))
			.and(td2.TAGSETDEFINITIONID.eq(tagsetDefinitionId)))
		.set(td1.PARENTID, td2.TAGDEFINITIONID)
		.where(td1.PARENTID.isNull())
		.and(td1.PARENTUUID.isNotNull())
		.and(td1.TAGSETDEFINITIONID.eq(tagsetDefinitionId))
		.execute();

		for (TagDefinition tagDefinition : tagsetDefinition) {
			if (!tagDefinition.getParentUuid().isEmpty()) {
				TagDefinition parentTagDef = 
					tagsetDefinition.getTagDefinition(
							tagDefinition.getParentUuid());
				tagDefinition.setParentId(parentTagDef.getId());
			}
		}
	}
	private void createPossibleValue(DSLContext db, String value,
			Integer propertyDefinitionId) {
		db
		.insertInto(
			PROPERTYDEF_POSSIBLEVALUE,
				PROPERTYDEF_POSSIBLEVALUE.VALUE,
				PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID)
		.values(
			value,
			propertyDefinitionId)
		.execute();
		
	}

	private void createDeepPropertyDefinition(
		DSLContext db, 
		PropertyDefinition propDef, Integer tagDefinitionId, 
		boolean isSystemProperty) {

		Integer propertyDefId = db
		.insertInto(
			PROPERTYDEFINITION,
				PROPERTYDEFINITION.UUID,
				PROPERTYDEFINITION.NAME,
				PROPERTYDEFINITION.TAGDEFINITIONID,
				PROPERTYDEFINITION.SYSTEMPROPERTY)
		.values(
			idGenerator.catmaIDToUUIDBytes(propDef.getUuid()),
			propDef.getName(),
			tagDefinitionId,
			(byte)(isSystemProperty?1:0))
		.returning(PROPERTYDEFINITION.PROPERTYDEFINITIONID)
		.fetchOne()
		.map(new IDFieldToIntegerMapper(PROPERTYDEFINITION.PROPERTYDEFINITIONID));
				
		propDef.setId(propertyDefId);
		
		for (String value : propDef.getPossibleValueList()
				.getPropertyValueList().getValues()) {
			
			createPossibleValue(db, value, propertyDefId);
		}
	}

	private void createDeepTagDefinition(
		DSLContext db, TagDefinition tagDefinition, Integer tagsetDefinitionId) {

		Integer tagDefinitionId = db
		.insertInto(
			TAGDEFINITION,
				TAGDEFINITION.UUID,
				TAGDEFINITION.VERSION,
				TAGDEFINITION.NAME,
				TAGDEFINITION.TAGSETDEFINITIONID,
				TAGDEFINITION.PARENTID,
				TAGDEFINITION.PARENTUUID)
		.values(
			idGenerator.catmaIDToUUIDBytes(tagDefinition.getUuid()),
			SqlTimestamp.from(tagDefinition.getVersion().getDate()),
			tagDefinition.getName(),
			tagsetDefinitionId,
			tagDefinition.getParentId(),
			idGenerator.catmaIDToUUIDBytes(tagDefinition.getParentUuid()))
		.returning(TAGDEFINITION.TAGDEFINITIONID)
		.fetchOne()
		.map(new IDFieldToIntegerMapper(TAGDEFINITION.TAGDEFINITIONID));
				
		
		tagDefinition.setId(tagDefinitionId);

		addAuthorIfAbsent(tagDefinition);
		
		for (PropertyDefinition propDef : 
			tagDefinition.getSystemPropertyDefinitions()) {
			
			createDeepPropertyDefinition(
					db,
					propDef,
					tagDefinitionId,
					true);
		}
		
		for (PropertyDefinition propDef : 
				tagDefinition.getUserDefinedPropertyDefinitions()) {
			
			createDeepPropertyDefinition(
					db,
					propDef,
					tagDefinitionId,
					false);
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



	void createTagDefinition(final TagsetDefinition tagsetDefinition,
			final TagDefinition tagDefinition) throws IOException {
		addAuthorIfAbsent(tagDefinition);

		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
				
		try {
			Integer tagLibraryId = db
					.select(TAGSETDEFINITION.TAGLIBRARYID)
					.from(TAGSETDEFINITION)
					.where(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId()))
					.fetchOne()
					.map(new IDFieldToIntegerMapper(TAGSETDEFINITION.TAGLIBRARYID));

			getLibraryAccess(db, tagLibraryId, true);

			db.beginTransaction();

			createDeepTagDefinition(
				db, 
				tagDefinition, 
				tagsetDefinition.getId());
						
			db
			.update(TAGSETDEFINITION)
			.set(TAGSETDEFINITION.VERSION, 
				SqlTimestamp.from(tagsetDefinition.getVersion().getDate()))
			.where(TAGSETDEFINITION.TAGSETDEFINITIONID
					.eq(tagsetDefinition.getId()))
			.execute();

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
	

	void removeTagsetDefinition(final TagsetDefinition tagsetDefinition) throws IOException {
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
				
		try {
			Integer tagLibraryId = db
					.select(TAGSETDEFINITION.TAGLIBRARYID)
					.from(TAGSETDEFINITION)
					.where(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId()))
					.fetchOne()
					.map(new IDFieldToIntegerMapper(TAGSETDEFINITION.TAGLIBRARYID));

			getLibraryAccess(db, tagLibraryId, true);

			List<Integer> obsoleteTagDefinitionIds = db
				.select(TAGDEFINITION.TAGDEFINITIONID)
				.from(TAGDEFINITION)
				.where(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId()))
				.fetch()
				.map(new IDFieldToIntegerMapper(TAGDEFINITION.TAGDEFINITIONID));
			
			List<Integer> obsoletePropertyDefinitionIds = db
				.select(PROPERTYDEFINITION.PROPERTYDEFINITIONID)
				.from(PROPERTYDEFINITION)
				.where(PROPERTYDEFINITION.TAGDEFINITIONID.in(obsoleteTagDefinitionIds))
				.fetch()
				.map(new IDFieldToIntegerMapper(PROPERTYDEFINITION.PROPERTYDEFINITIONID));
			
			db.beginTransaction();
			
			db.batch(
				db
				.delete(PROPERTYDEF_POSSIBLEVALUE)
				.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.in(
						obsoletePropertyDefinitionIds)),
				db
				.delete(PROPERTYDEFINITION)
				.where(PROPERTYDEFINITION.TAGDEFINITIONID.in(
						obsoleteTagDefinitionIds)),
				db
				.update(TAGDEFINITION)
				.set(TAGDEFINITION.PARENTID, (Integer)null)
				.where(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId())),
				db
				.delete(TAGDEFINITION)
				.where(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId())),
				db
				.delete(TAGSETDEFINITION)
				.where(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId())))
			.execute();
			
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

	void createTagsetDefinition(final TagLibrary tagLibrary,
			final TagsetDefinition tagsetDefinition) throws IOException {
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		getLibraryAccess(db, tagLibrary.getId(), true);

		createDeepTagsetDefinition(
			db,
			tagsetDefinition,
			Integer.valueOf(tagLibrary.getId()));
		
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
					"Please reload your Tag Type Library using the Tag Type Manager!");
		}
		else if (!independent) {
			return AccessMode.WRITE;
		}
		else {
			return AccessMode.getAccessMode(existingAccessModeValue);
		}
	}
	
	void updateTagsetDefinition(final TagsetDefinition tagsetDefinition) throws IOException {
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		Integer tagLibraryId = db
				.select(TAGSETDEFINITION.TAGLIBRARYID)
				.from(TAGSETDEFINITION)
				.where(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId()))
				.fetchOne()
				.map(new IDFieldToIntegerMapper(TAGSETDEFINITION.TAGLIBRARYID));

		getLibraryAccess(db, tagLibraryId, true);
		
		db
		.update(TAGSETDEFINITION)
		.set(TAGSETDEFINITION.NAME, tagsetDefinition.getName())
		.set(TAGSETDEFINITION.VERSION, 
				SqlTimestamp.from(tagsetDefinition.getVersion().getDate()))
		.where(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId()))
		.execute();
	}


	void delete(TagManager tagManager, TagLibraryReference tagLibraryReference)
			throws IOException {
	
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
				
		try {
			Integer tagLibraryId = Integer.valueOf(tagLibraryReference.getId());
			
			Field<Integer> totalParticipantsField = db
			.select(DSL.count(USER_TAGLIBRARY.USERID))
			.from(USER_TAGLIBRARY)
			.where(USER_TAGLIBRARY.TAGLIBRARYID
					.eq(tagLibraryId))
			.asField("totalParticipants");
			
			Record currentUserTagLibRecord = db
			.select(
				USER_TAGLIBRARY.USER_TAGLIBRARYID, 
				USER_TAGLIBRARY.TAGLIBRARYID, 
				USER_TAGLIBRARY.ACCESSMODE, 
				USER_TAGLIBRARY.OWNER,
				totalParticipantsField)
			.from(USER_TAGLIBRARY)
			.where(USER_TAGLIBRARY.USERID
					.eq(dbRepository.getCurrentUser().getUserId()))
			.and(USER_TAGLIBRARY.TAGLIBRARYID.eq(tagLibraryId))
			.fetchOne();

			if ((currentUserTagLibRecord == null) 
					|| (currentUserTagLibRecord.getValue(
							USER_USERMARKUPCOLLECTION.ACCESSMODE) == null)) {
				throw new IllegalStateException(
						"you seem to have no access rights for this collection!");
			}

			Integer userTagLibId =
					currentUserTagLibRecord.getValue(
							USER_TAGLIBRARY.USER_TAGLIBRARYID);
			
			db.beginTransaction();
			
			db
			.delete(USER_TAGLIBRARY)
			.where(USER_TAGLIBRARY.USER_TAGLIBRARYID.eq(userTagLibId))
			.execute();
			
			db.commitTransaction();

			tagLibraryReferencesById.remove(tagLibraryReference.getId());
			tagManager.removeTagLibrary(tagLibraryReference);
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.tagLibraryChanged.name(),
					tagLibraryReference, null);	

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

	void updateTagDefinition(final TagDefinition tagDefinition) {
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
				
		try {
			Integer tagLibraryId = db
					.select(TAGSETDEFINITION.TAGLIBRARYID)
					.from(TAGSETDEFINITION)
					.join(TAGDEFINITION)
						.on(TAGDEFINITION.TAGSETDEFINITIONID.eq(TAGSETDEFINITION.TAGSETDEFINITIONID))
						.and(TAGDEFINITION.TAGDEFINITIONID.eq(tagDefinition.getId()))
					.fetchOne()
					.map(new IDFieldToIntegerMapper(TAGSETDEFINITION.TAGLIBRARYID));

			getLibraryAccess(db, tagLibraryId, true);

			PropertyDefinition colorPropertyDef = 
				tagDefinition.getPropertyDefinitionByName(
					PropertyDefinition.SystemPropertyName.catma_displaycolor.name());

			db.beginTransaction();
			
			db.batch(
				db
				.update(PROPERTYDEF_POSSIBLEVALUE)
				.set(PROPERTYDEF_POSSIBLEVALUE.VALUE, colorPropertyDef.getFirstValue())
				.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.eq(colorPropertyDef.getId())),
				db
				.update(TAGDEFINITION)
				.set(TAGDEFINITION.NAME, tagDefinition.getName())
				.set(TAGDEFINITION.VERSION, SqlTimestamp.from(tagDefinition.getVersion().getDate()))
				.where(TAGDEFINITION.TAGDEFINITIONID.eq(tagDefinition.getId())))
			.execute();

			db.commitTransaction();
		}
		catch (Exception e) {
			db.rollbackTransaction();
			db.close();
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					e);	
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	}
	
	void removeTagDefinition(
		final TagsetDefinition tagsetDefinition, 
		final TagDefinition tagDefinition) throws IOException {
		
		
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
		
		try {
			Set<Integer> toBeDeletedTagDefIds = new HashSet<Integer>();
			toBeDeletedTagDefIds.add(tagDefinition.getId());
			
			Result<Record> children = 
					db.fetch(
						"call CatmaRepository.getTagDefinitionChildren(" 
								+ tagDefinition.getId() + ")");
			
			for (Record r : children) {
				toBeDeletedTagDefIds.add((Integer)r.getValue("tagDefinitionID"));
			}
			
			Integer tagLibraryId = db
				.select(TAGSETDEFINITION.TAGLIBRARYID)
				.from(TAGSETDEFINITION)
				.join(TAGDEFINITION)
					.on(TAGDEFINITION.TAGSETDEFINITIONID
							.eq(TAGSETDEFINITION.TAGSETDEFINITIONID))
					.and(TAGDEFINITION.TAGDEFINITIONID.eq(tagDefinition.getId()))
				.fetchOne()
				.map(new IDFieldToIntegerMapper(TAGSETDEFINITION.TAGLIBRARYID));

			getLibraryAccess(db, tagLibraryId, true);

			List<Integer> toBeDeletedPropertyDefIds = db
			.select(PROPERTYDEFINITION.PROPERTYDEFINITIONID)
			.from(PROPERTYDEFINITION)
			.where(PROPERTYDEFINITION.TAGDEFINITIONID.in(toBeDeletedTagDefIds))
			.fetch()
			.map(new IDFieldToIntegerMapper(PROPERTYDEFINITION.PROPERTYDEFINITIONID));
			
			db.beginTransaction();
			
			db.batch(
				db
				.delete(PROPERTYDEF_POSSIBLEVALUE)
				.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.in(
						toBeDeletedPropertyDefIds)),
				db
				.delete(PROPERTYDEFINITION)
				.where(PROPERTYDEFINITION.TAGDEFINITIONID.in(toBeDeletedTagDefIds)),
				db
				.update(TAGDEFINITION)
				.set(TAGDEFINITION.PARENTID, (Integer)null)
				.where(TAGDEFINITION.TAGDEFINITIONID.in(toBeDeletedTagDefIds)),
				db
				.delete(TAGDEFINITION)
				.where(TAGDEFINITION.TAGDEFINITIONID.in(toBeDeletedTagDefIds)),
				db
				.update(TAGSETDEFINITION)
				.set(TAGSETDEFINITION.VERSION, 
					SqlTimestamp.from(tagsetDefinition.getVersion().getDate()))
				.where(TAGSETDEFINITION.TAGSETDEFINITIONID
						.eq(tagsetDefinition.getId())))
			.execute();
			
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

	TagsetDefinitionUpdateLog updateTagsetDefinition(DSLContext db, TagLibrary tagLibrary,
			TagsetDefinition tagsetDefinition) throws IOException {

		TagsetDefinitionUpdateLog tagsetDefinitionUpdateLog = 
				new TagsetDefinitionUpdateLog();
		
		Integer tagLibraryId = db
			.select(TAGSETDEFINITION.TAGLIBRARYID)
			.from(TAGSETDEFINITION)
			.where(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId()))
			.fetchOne()
			.map(new IDFieldToIntegerMapper(TAGSETDEFINITION.TAGLIBRARYID));

		getLibraryAccess(db, tagLibraryId, true);
		
		if (tagsetDefinition.getId() == null) {
			createDeepTagsetDefinition(
				db,
				tagsetDefinition,
				tagLibraryId);
		}
		else {
			updateDeepTagsetDefinition(db, tagsetDefinition, tagsetDefinitionUpdateLog);
		}
		
		return tagsetDefinitionUpdateLog;
	}

	private void updateDeepTagsetDefinition(DSLContext db,
			TagsetDefinition tagsetDefinition, 
			TagsetDefinitionUpdateLog tagsetDefinitionUpdateLog) {
		
		db
		.update(TAGSETDEFINITION)
		.set(TAGSETDEFINITION.NAME, tagsetDefinition.getName())
		.set(TAGSETDEFINITION.VERSION, 
				SqlTimestamp.from(tagsetDefinition.getVersion().getDate()))
		.where(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId()))
		.execute();

		for (TagDefinition tagDefinition : tagsetDefinition) {
			if (tagDefinition.getId() == null) {
				createDeepTagDefinition(db, tagDefinition, tagsetDefinition.getId());
			}
			else {
				updateDeepTagDefinition(db, tagDefinition, tagsetDefinitionUpdateLog);
			}
		}
		
		maintainTagDefinitionHierarchy(db, tagsetDefinition);
		
		List<String> oldTagDefUUIDs = db
		.select(TAGDEFINITION.UUID)
		.from(TAGDEFINITION)
		.where(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId()))
		.fetch()
		.map(new UUIDByteToStringFieldMapper());
		
		HashSet<String> toBeDeletedTagDefUUID = new HashSet<String>();
		HashSet<byte[]> toBeDeletedTagDefByteUUID = new HashSet<byte[]>();
		for (String uuid : oldTagDefUUIDs) {
			if (!tagsetDefinition.hasTagDefinition(uuid)) {
				toBeDeletedTagDefUUID.add(uuid);
				toBeDeletedTagDefByteUUID.add(idGenerator.catmaIDToUUIDBytes(uuid));
			}
			
		}
		
		List<Integer> toBeDeletedTagDefIds = db
			.select(TAGDEFINITION.TAGDEFINITIONID)
			.from(TAGDEFINITION)
			.where(TAGDEFINITION.UUID.in(toBeDeletedTagDefByteUUID))
			.and(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId()))
			.fetch()
			.map(new IDFieldToIntegerMapper(TAGDEFINITION.TAGDEFINITIONID));
		
		List<Integer> toBeDeletedPropertyDefIds = db
			.select(PROPERTYDEFINITION.PROPERTYDEFINITIONID)
			.from(PROPERTYDEFINITION)
			.where(PROPERTYDEFINITION.TAGDEFINITIONID.in(toBeDeletedTagDefIds))
			.fetch()
			.map(new IDFieldToIntegerMapper(PROPERTYDEFINITION.PROPERTYDEFINITIONID));
		
		db.batch(
			db
			.delete(PROPERTYDEF_POSSIBLEVALUE)
			.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.in(toBeDeletedPropertyDefIds)),
			db
			.delete(PROPERTYDEFINITION)
			.where(PROPERTYDEFINITION.TAGDEFINITIONID.in(toBeDeletedTagDefIds)),
			db
			.update(TAGDEFINITION)
			.set(TAGDEFINITION.PARENTID, (Integer)null)
			.where(TAGDEFINITION.UUID.in(toBeDeletedTagDefByteUUID)
			.and(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId()))),
			db
			.delete(TAGDEFINITION)
			.where(TAGDEFINITION.UUID.in(toBeDeletedTagDefByteUUID))
			.and(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId())))
		.execute();
		
		tagsetDefinitionUpdateLog.setDeletedTagDefinitionUuids(toBeDeletedTagDefUUID);
	}

	private void updateDeepTagDefinition(
			DSLContext db, TagDefinition tagDefinition, 
			TagsetDefinitionUpdateLog tagsetDefinitionUpdateLog) {
		
		boolean propDefChanged = false;
		
		for (PropertyDefinition pd : tagDefinition.getSystemPropertyDefinitions()) {
			if (pd.getId() == null) {
				createDeepPropertyDefinition(db, pd, tagDefinition.getId(), true);
				propDefChanged = true;
			}
			else {
				if (updateDeepPropertyDefinition(db, pd, true, tagsetDefinitionUpdateLog)) {
					tagsetDefinitionUpdateLog.addUpdatedPropertyDefinition(
							pd.getUuid(), tagDefinition.getUuid());
					propDefChanged = true;
				}
			}
		}
		
		for (PropertyDefinition pd : tagDefinition.getUserDefinedPropertyDefinitions()) {
			if (pd.getId() == null) {
				createDeepPropertyDefinition(db, pd, tagDefinition.getId(), false);
				propDefChanged = true;
			}
			else {
				if (updateDeepPropertyDefinition(db, pd, false, tagsetDefinitionUpdateLog)) {
					tagsetDefinitionUpdateLog.addUpdatedPropertyDefinition(
							pd.getUuid(), tagDefinition.getUuid());
					propDefChanged = true;
				}
			}
		}
		
		int tagDefUpdated = db
		.update(TAGDEFINITION)
		.set(TAGDEFINITION.NAME, tagDefinition.getName())
		.set(TAGDEFINITION.VERSION, SqlTimestamp.from(tagDefinition.getVersion().getDate()))
		.where(TAGDEFINITION.TAGDEFINITIONID.eq(tagDefinition.getId()))
		.and(TAGDEFINITION.NAME.ne(tagDefinition.getName())
				.or(DSL.val(propDefChanged).eq(true))
				.or(TAGDEFINITION.VERSION.ne(SqlTimestamp.from(tagDefinition.getVersion().getDate()))))
		.execute();
		
		if (tagDefUpdated>0) {
			tagsetDefinitionUpdateLog.addUpdatedTagDefinition(
					tagDefinition.getUuid());
		}

		HashSet<String> existingPropertyDefinitionUUIDs = 
				new HashSet<String>();
				
		PropertyDefinitionToUUID pd2ByteUUID = new PropertyDefinitionToUUID();
		
		existingPropertyDefinitionUUIDs.addAll(
			Collections2.transform(
				tagDefinition.getSystemPropertyDefinitions(),
				pd2ByteUUID));
		existingPropertyDefinitionUUIDs.addAll(
			Collections2.transform(
				tagDefinition.getUserDefinedPropertyDefinitions(),
				pd2ByteUUID));
		
		List<String> oldPropertyDefinitionUUIDs = 
		db
		.select(PROPERTYDEFINITION.UUID)
		.from(PROPERTYDEFINITION)
		.where(PROPERTYDEFINITION.TAGDEFINITIONID.eq(tagDefinition.getId()))
		.fetch()
		.map(new UUIDByteToStringFieldMapper());
		
		Collection<String> toBeDeletedPropertyDefUUIDs = 
			Collections3.getSetDifference(
				oldPropertyDefinitionUUIDs, existingPropertyDefinitionUUIDs);
		
		Collection<byte[]> toBeDeletedPropertyDefByteUUIDs = 
				Collections2.transform(toBeDeletedPropertyDefUUIDs, new UUIDtoByteMapper());

		if (!toBeDeletedPropertyDefByteUUIDs.isEmpty()) {
			
			List<Integer> toBeDeletedPropertyDefIds = db
				.select(PROPERTYDEFINITION.PROPERTYDEFINITIONID)
				.from(PROPERTYDEFINITION)
				.where(PROPERTYDEFINITION.UUID.in(toBeDeletedPropertyDefByteUUIDs))
				.and(PROPERTYDEFINITION.TAGDEFINITIONID.eq(tagDefinition.getId()))
				.fetch()
				.map(new IDFieldToIntegerMapper(PROPERTYDEFINITION.PROPERTYDEFINITIONID));
			
			
			db.batch(
				db
				.delete(PROPERTYDEF_POSSIBLEVALUE)
				.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.in(toBeDeletedPropertyDefIds)),
				db
				.delete(PROPERTYDEFINITION)
				.where(PROPERTYDEFINITION.PROPERTYDEFINITIONID.in(toBeDeletedPropertyDefIds)))
			.execute();
		}		
		tagsetDefinitionUpdateLog.addDeletedPropertyDefinitions(toBeDeletedPropertyDefUUIDs);
	}

	private boolean updateDeepPropertyDefinition(DSLContext db,
			PropertyDefinition pd, boolean isSystemProperty,
			TagsetDefinitionUpdateLog tagsetDefinitionUpdateLog) {
		
		int noOfChangedRecords = 0;
		
		if (!isSystemProperty) {
			noOfChangedRecords += db
			.update(PROPERTYDEFINITION)
			.set(PROPERTYDEFINITION.NAME, pd.getName())
			.where(PROPERTYDEFINITION.PROPERTYDEFINITIONID.eq(pd.getId()))
			.and(PROPERTYDEFINITION.NAME.ne(pd.getName()))
			.execute();
		}

		List<String> oldValues = db
		.select(PROPERTYDEF_POSSIBLEVALUE.VALUE)
		.from(PROPERTYDEF_POSSIBLEVALUE)
		.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.eq(pd.getId()))
		.fetch()
		.map(new FieldToValueMapper<String>(PROPERTYDEF_POSSIBLEVALUE.VALUE));
		
		List<String> newValues = pd.getPossibleValueList().getPropertyValueList().getValues();
		
		Collection<String> toBeDeleted = Collections3.getSetDifference(oldValues, newValues);
		Collection<String> toBeInserted = Collections3.getSetDifference(newValues, oldValues);
		
		if (!toBeDeleted.isEmpty()) {
			noOfChangedRecords += db
			.delete(PROPERTYDEF_POSSIBLEVALUE)
			.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.eq(pd.getId()))
			.and(PROPERTYDEF_POSSIBLEVALUE.VALUE.in(toBeDeleted))
			.execute();
		}
		
		if (!toBeInserted.isEmpty()) {
			BatchBindStep insertBatch = db.batch(db
			.insertInto(
				PROPERTYDEF_POSSIBLEVALUE,
					PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID,
					PROPERTYDEF_POSSIBLEVALUE.VALUE)
			.values(
				(Integer)null, 
				null));
			
			for (String value : toBeInserted) {
				insertBatch.bind(pd.getId(), value);
			}
			
			insertBatch.execute();
		}
		
		return noOfChangedRecords != 0;
	}
	
	
	public void update(final TagLibraryReference tagLibraryReference) {
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		try {
			final Integer tagLibraryId = 
					Integer.valueOf(tagLibraryReference.getId());
			ContentInfoSet contentInfoSet = tagLibraryReference.getContentInfoSet();
	
			
			db
			.update(TAGLIBRARY)
			.set(TAGLIBRARY.TITLE, contentInfoSet.getTitle())
			.set(TAGLIBRARY.AUTHOR, contentInfoSet.getAuthor())
			.set(TAGLIBRARY.PUBLISHER, contentInfoSet.getPublisher())
			.set(TAGLIBRARY.DESCRIPTION, contentInfoSet.getDescription())
			.where(TAGLIBRARY.TAGLIBRARYID.eq(tagLibraryId))
			.execute();
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.tagLibraryChanged.name(),
					contentInfoSet, tagLibraryReference);
		}
		catch (Exception e) {
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, e);
		}
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
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
				
		try {
			Integer tagLibraryId = db
					.select(TAGSETDEFINITION.TAGLIBRARYID)
					.from(TAGSETDEFINITION)
					.join(TAGDEFINITION)
						.on(TAGDEFINITION.TAGSETDEFINITIONID.eq(TAGSETDEFINITION.TAGSETDEFINITIONID))
						.and(TAGDEFINITION.TAGDEFINITIONID.eq(td.getId()))
					.fetchOne()
					.map(new IDFieldToIntegerMapper(
							TAGSETDEFINITION.TAGLIBRARYID));

			getLibraryAccess(db, tagLibraryId, true);
			
			db.beginTransaction();

			db
			.update(TAGDEFINITION)
			.set(TAGDEFINITION.VERSION, SqlTimestamp.from(td.getVersion().getDate()))
			.where(TAGDEFINITION.TAGDEFINITIONID.eq(td.getId()))
			.execute();
			
			createDeepPropertyDefinition(db, pd, td.getId(), false);
			
			db.commitTransaction();
		}
		catch (Exception e) {
			db.rollbackTransaction();
			db.close();
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					e);				
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}

	}
	
	void updatePropertyDefinition(
			final PropertyDefinition pd, final TagDefinition td) {
	
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
				
		try {
			Integer tagLibraryId = db
				.select(TAGSETDEFINITION.TAGLIBRARYID)
				.from(TAGSETDEFINITION)
				.join(TAGDEFINITION)
					.on(TAGDEFINITION.TAGSETDEFINITIONID
							.eq(TAGSETDEFINITION.TAGSETDEFINITIONID))
					.and(TAGDEFINITION.TAGDEFINITIONID.eq(td.getId()))
				.fetchOne()
				.map(new IDFieldToIntegerMapper(
						TAGSETDEFINITION.TAGLIBRARYID));

			getLibraryAccess(db, tagLibraryId, true);
			
			db.beginTransaction();

			db
			.update(TAGDEFINITION)
			.set(TAGDEFINITION.VERSION, SqlTimestamp.from(td.getVersion().getDate()))
			.where(TAGDEFINITION.TAGDEFINITIONID.eq(td.getId()))
			.execute();
			
			updateDeepPropertyDefinition(db, pd, false, new TagsetDefinitionUpdateLog());
			
			db.commitTransaction();
		}
		catch (Exception e) {
			db.rollbackTransaction();
			db.close();
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					e);				
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	
	
	}

	public void removePropertyDefinition(
			final PropertyDefinition propertyDefinition, final TagDefinition tagDefinition) {
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);
				
		try {
			Integer tagLibraryId = db
					.select(TAGSETDEFINITION.TAGLIBRARYID)
					.from(TAGSETDEFINITION)
					.join(TAGDEFINITION)
						.on(TAGDEFINITION.TAGSETDEFINITIONID.eq(TAGSETDEFINITION.TAGSETDEFINITIONID))
						.and(TAGDEFINITION.TAGDEFINITIONID.eq(tagDefinition.getId()))
					.fetchOne()
					.map(new IDFieldToIntegerMapper(
							TAGSETDEFINITION.TAGLIBRARYID));

			getLibraryAccess(db, tagLibraryId, true);
			
			db.beginTransaction();

			db.batch(
				db
				.update(TAGDEFINITION)
				.set(TAGDEFINITION.VERSION, SqlTimestamp.from(tagDefinition.getVersion().getDate()))
				.where(TAGDEFINITION.TAGDEFINITIONID.eq(tagDefinition.getId())),
				db
				.delete(PROPERTYDEF_POSSIBLEVALUE)
				.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.eq(propertyDefinition.getId())),
				db
				.delete(PROPERTYDEFINITION)
				.where(PROPERTYDEFINITION.PROPERTYDEFINITIONID.eq(propertyDefinition.getId())))
			.execute();
			
			db.commitTransaction();
		}
		catch (Exception e) {
			db.rollbackTransaction();
			db.close();
			
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					e);				
		}
		finally {
			if (db!=null) {
				db.close();
			}
		}
	

	}
	
	public void reloadTagLibraryReferences(DSLContext db) {
		HashMap<String, TagLibraryReference> result = 
				getTagLibraryReferences(db);
		
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
				ContentInfoSet contentInfoSet = 
						entry.getValue().getContentInfoSet();
				tagLibraryReferencesById.put(
						entry.getKey(), entry.getValue());
				dbRepository.getPropertyChangeSupport().firePropertyChange(
						RepositoryChangeEvent.tagLibraryChanged.name(),
						contentInfoSet, entry.getValue());
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

	void copyTagLibraries(int sourceUserId) throws IOException {

		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);

		List<TagLibraryReference> tagLibReferences = db
		.select()
		.from(TAGLIBRARY)
		.join(USER_TAGLIBRARY)
			.on(USER_TAGLIBRARY.TAGLIBRARYID.eq(TAGLIBRARY.TAGLIBRARYID)
			.and(USER_TAGLIBRARY.USERID.eq(sourceUserId)))
		.where(TAGLIBRARY.INDEPENDENT.eq((byte)1))
		.fetch()
		.map(new TagLibraryReferenceMapper());
		
		for (TagLibraryReference tagLibraryReference : tagLibReferences) {
			TagLibrary tagLibrary = getTagLibrary(tagLibraryReference);
			TagLibrary copyTagLibrary = new TagLibrary(tagLibrary);
			importTagLibrary(db, copyTagLibrary, true);
			
			TagLibraryReference ref = new TagLibraryReference(
					copyTagLibrary.getId(), copyTagLibrary.getContentInfoSet());
			tagLibraryReferencesById.put(ref.getId(), ref);
		}
		
	}
	
}
