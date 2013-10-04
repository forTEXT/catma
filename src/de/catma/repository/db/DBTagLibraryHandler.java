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
import static de.catma.repository.db.jooq.catmarepository.Tables.TAGDEFINITION;
import static de.catma.repository.db.jooq.catmarepository.Tables.TAGINSTANCE;
import static de.catma.repository.db.jooq.catmarepository.Tables.TAGLIBRARY;
import static de.catma.repository.db.jooq.catmarepository.Tables.TAGREFERENCE;
import static de.catma.repository.db.jooq.catmarepository.Tables.TAGSETDEFINITION;
import static de.catma.repository.db.jooq.catmarepository.Tables.USER_TAGLIBRARY;
import static de.catma.repository.db.jooq.catmarepository.Tables.USER_USERMARKUPCOLLECTION;

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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

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
	
	private static class PropertyDefinitionToByteUUID implements Function<PropertyDefinition, byte[]> {
		private IDGenerator idGenerator;
		public byte[] apply(PropertyDefinition pd) {
			return idGenerator.catmaIDToUUIDBytes(pd.getUuid());
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
			
			Integer tagLibraryId = 
					createTagLibrary(db, name, null, null, null, true);
			
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
		
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			db.beginTransaction();
			importTagLibrary(db, tagLibrary, true);
			db.commitTransaction();
			
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
	}
	
	private void createDeepTagsetDefinition(
			DSLContext db, TagsetDefinition tagsetDefinition, Integer tagLibraryId) {
		
		Integer tagsetDefinitionId = createTagsetDefinition(
				db,
				tagsetDefinition.getUuid(),
				tagsetDefinition.getName(),
				tagsetDefinition.getVersion(),
				tagLibraryId);
		
		for (TagDefinition tagDefinition : tagsetDefinition) {
			createDeepTagDefinition(
				db,
				tagDefinition,
				tagsetDefinitionId);
		}
		
		de.catma.repository.db.jooq.catmarepository.tables.Tagdefinition td1 = 
				TAGDEFINITION.as("td1"); 
		de.catma.repository.db.jooq.catmarepository.tables.Tagdefinition td2 = 
				TAGDEFINITION.as("td2");
		
		// maintain tagdef hierarchy
		db
		.update(td1)
		.set(
			td1.PARENTID, db.
				select(td2.TAGDEFINITIONID)
				.from(td2)
				.where(td2.UUID.eq(td1.PARENTUUID))
				.and(td2.TAGSETDEFINITIONID.eq(tagsetDefinitionId)))
		.where(td1.PARENTID.isNull())
		.and(td1.PARENTUUID.isNotNull())
		.and(td1.TAGSETDEFINITIONID.eq(tagsetDefinitionId));

		for (TagDefinition tagDefinition : tagsetDefinition) {
			if (tagDefinition.getParentUuid() != null) {
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

	private Integer createTagsetDefinition(DSLContext db, String uuid,
			String name, Version version, Integer tagLibraryId) {
		
		Integer tagsetDefinitionId = db
		.insertInto(
			TAGSETDEFINITION,
				TAGSETDEFINITION.UUID,
				TAGSETDEFINITION.VERSION,
				TAGSETDEFINITION.NAME,
				TAGSETDEFINITION.TAGLIBRARYID)
		.values(
			idGenerator.catmaIDToUUIDBytes(uuid),
			SqlTimestamp.from(version.getDate()),
			name, 
			tagLibraryId)
		.returning(TAGSETDEFINITION.TAGSETDEFINITIONID)
		.fetchOne()
		.map(new IDFieldToIntegerMapper(TAGSETDEFINITION.TAGSETDEFINITIONID));
			
				
		return tagsetDefinitionId;
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
			final TagDefinition tagDefinition) {
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
						
			db.commitTransaction();
		}
		catch (Exception e) {
			db.rollbackTransaction();
			db.close();
			
			dbRepository.setTagManagerListenersEnabled(true);

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
	

	void removeTagsetDefinition(final TagsetDefinition tagsetDefinition) {
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
			
			db.batch(
				db
				.delete(PROPERTYVALUE)
				.where(PROPERTYVALUE.PROPERTYID.in(
					db
					.select(PROPERTY.PROPERTYID)
					.from(PROPERTY)
					.join(TAGINSTANCE)
						.on(TAGINSTANCE.TAGINSTANCEID
								.eq(PROPERTY.TAGINSTANCEID))
					.join(TAGDEFINITION)
						.on(TAGDEFINITION.TAGDEFINITIONID.eq(TAGINSTANCE.TAGDEFINITIONID))
						.and(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId())))),
				db
				.delete(PROPERTY)
				.where(PROPERTY.TAGINSTANCEID.in(
					db
					.select(TAGINSTANCE.TAGINSTANCEID)
					.from(TAGINSTANCE)
					.join(TAGDEFINITION)
						.on(TAGDEFINITION.TAGDEFINITIONID.eq(TAGINSTANCE.TAGDEFINITIONID))
						.and(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId())))),
				db
				.delete(TAGREFERENCE)
				.where(TAGREFERENCE.TAGINSTANCEID.in(
					db
					.select(TAGINSTANCE.TAGINSTANCEID)
					.from(TAGINSTANCE)
					.join(TAGDEFINITION)
						.on(TAGDEFINITION.TAGDEFINITIONID.eq(TAGINSTANCE.TAGDEFINITIONID))
						.and(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId())))),
				db
				.delete(TAGINSTANCE)
				.where(TAGINSTANCE.TAGDEFINITIONID.in(
					db
					.select(TAGDEFINITION.TAGDEFINITIONID)
					.from(TAGDEFINITION)
					.where(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId())))),
				db
				.delete(PROPERTYDEF_POSSIBLEVALUE)
				.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.in(
					db
					.select(PROPERTYDEFINITION.PROPERTYDEFINITIONID)
					.from(PROPERTYDEFINITION)
					.join(TAGDEFINITION)
						.on(TAGDEFINITION.TAGDEFINITIONID
								.eq(PROPERTYDEFINITION.TAGDEFINITIONID))
						.and(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId())))),
				db
				.delete(PROPERTYDEFINITION)
				.where(PROPERTYDEFINITION.TAGDEFINITIONID.in(
					db
					.select(TAGDEFINITION.TAGDEFINITIONID)
					.from(TAGDEFINITION)
					.where(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId())))),
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
			
			dbRepository.setTagManagerListenersEnabled(true);

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

	void createTagsetDefinition(final TagLibrary tagLibrary,
			final TagsetDefinition tagsetDefinition) {
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		try {
			getLibraryAccess(db, tagLibrary.getId(), true);

			Integer tagsetDefinitionId = createTagsetDefinition(
				db,
				tagsetDefinition.getUuid(), 
				tagsetDefinition.getName(),
				tagsetDefinition.getVersion(),
				Integer.valueOf(tagLibrary.getId()));
			
			tagsetDefinition.setId(tagsetDefinitionId);
		}
		catch (Exception e) {
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					e);	
		}
		
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
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		try {
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
		catch (Exception e) {
			dbRepository.getPropertyChangeSupport().firePropertyChange(
					RepositoryChangeEvent.exceptionOccurred.name(),
					null, 
					e);	
		}

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
			boolean isOwner = 
					currentUserTagLibRecord.getValue(
							USER_TAGLIBRARY.OWNER, Boolean.class);
			
			int totalParticipants = 
					(Integer)currentUserTagLibRecord.getValue("totalParticipants");

			
			db.beginTransaction();
			
			db
			.delete(USER_TAGLIBRARY)
			.where(USER_TAGLIBRARY.USER_TAGLIBRARYID.eq(userTagLibId))
			.execute();
			
			if (isOwner && (totalParticipants == 1)) {
				db.batch(
					db
					.delete(PROPERTYDEF_POSSIBLEVALUE)
					.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.in(
						db
						.select(PROPERTYDEFINITION.PROPERTYDEFINITIONID)
						.from(PROPERTYDEFINITION)
						.join(TAGDEFINITION)
							.on(TAGDEFINITION.TAGDEFINITIONID
									.eq(PROPERTYDEFINITION.TAGDEFINITIONID))
						.join(TAGSETDEFINITION)
							.on(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(TAGDEFINITION.TAGSETDEFINITIONID))
							.and(TAGSETDEFINITION.TAGLIBRARYID.eq(tagLibraryId)))),
					db
					.delete(PROPERTYDEFINITION)
					.where(PROPERTYDEFINITION.TAGDEFINITIONID.in(
						db
						.select(TAGDEFINITION.TAGDEFINITIONID)
						.from(TAGDEFINITION)
						.join(TAGSETDEFINITION)
							.on(TAGSETDEFINITION.TAGSETDEFINITIONID.eq(TAGDEFINITION.TAGSETDEFINITIONID))
							.and(TAGSETDEFINITION.TAGLIBRARYID.eq(tagLibraryId)))),
					db
					.delete(TAGDEFINITION)
					.where(TAGDEFINITION.TAGSETDEFINITIONID.in(
						db
						.select(TAGSETDEFINITION.TAGSETDEFINITIONID)
						.from(TAGSETDEFINITION)
						.where(TAGSETDEFINITION.TAGLIBRARYID.eq(tagLibraryId)))),
					db
					.delete(TAGSETDEFINITION)
					.where(TAGSETDEFINITION.TAGLIBRARYID.eq(tagLibraryId)),
					db
					.delete(TAGLIBRARY)
					.where(TAGLIBRARY.TAGLIBRARYID.eq(tagLibraryId)))
				.execute();

			}
			
			db.commitTransaction();

			tagLibraryReferencesById.remove(tagLibraryReference);
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

			db.beginTransaction();
			
			PropertyDefinition colorPropertyDef = 
				tagDefinition.getPropertyDefinitionByName(
					PropertyDefinition.SystemPropertyName.catma_displaycolor.name());
			
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
	
	void removeTagDefinition(final TagDefinition tagDefinition) {
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

			db.beginTransaction();
			
			db.batch(
				db
				.delete(PROPERTYVALUE)
				.where(PROPERTYVALUE.PROPERTYID.in(
					db
					.select(PROPERTY.PROPERTYID)
					.from(PROPERTY)
					.join(TAGINSTANCE)
						.on(TAGINSTANCE.TAGINSTANCEID
								.eq(PROPERTY.TAGINSTANCEID)
						.and(TAGINSTANCE.TAGDEFINITIONID.eq(tagDefinition.getId()))))),
				db
				.delete(PROPERTY)
				.where(PROPERTY.TAGINSTANCEID.in(
					db
					.select(TAGINSTANCE.TAGINSTANCEID)
					.from(TAGINSTANCE)
					.where(TAGINSTANCE.TAGDEFINITIONID.eq(tagDefinition.getId())))),
				db
				.delete(TAGREFERENCE)
				.where(TAGREFERENCE.TAGINSTANCEID.in(
					db
					.select(TAGINSTANCE.TAGINSTANCEID)
					.from(TAGINSTANCE)
					.where(TAGINSTANCE.TAGDEFINITIONID.eq(tagDefinition.getId())))),
				db
				.delete(TAGINSTANCE)
				.where(TAGINSTANCE.TAGDEFINITIONID.eq(tagDefinition.getId())),
				db
				.delete(PROPERTYDEF_POSSIBLEVALUE)
				.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.in(
					db
					.select(PROPERTYDEFINITION.PROPERTYDEFINITIONID)
					.from(PROPERTYDEFINITION)
					.join(TAGDEFINITION)
						.on(TAGDEFINITION.TAGDEFINITIONID
								.eq(PROPERTYDEFINITION.TAGDEFINITIONID))
						.and(TAGDEFINITION.TAGDEFINITIONID.eq(tagDefinition.getId())))),
				db
				.delete(PROPERTYDEFINITION)
				.where(PROPERTYDEFINITION.TAGDEFINITIONID.in(
					db
					.select(TAGDEFINITION.TAGDEFINITIONID)
					.from(TAGDEFINITION)
					.where(TAGDEFINITION.TAGDEFINITIONID.eq(tagDefinition.getId())))),
				db
				.delete(TAGDEFINITION)
				.where(TAGDEFINITION.TAGDEFINITIONID.eq(tagDefinition.getId())))
			.execute();
			
			db.commitTransaction();
		}
		catch (Exception e) {
			db.rollbackTransaction();
			db.close();
			
			dbRepository.setTagManagerListenersEnabled(true);

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

	Set<byte[]> updateTagsetDefinition(DSLContext db, TagLibrary tagLibrary,
			TagsetDefinition tagsetDefinition) throws IOException {
		Set<byte[]> deletedUuids = new HashSet<byte[]>();
		
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
			updateDeepTagsetDefinition(db, tagsetDefinition);
		}
		return null;
	}

	private void updateDeepTagsetDefinition(DSLContext db,
			TagsetDefinition tagsetDefinition) {
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
				updateDeepTagDefinition(db, tagDefinition);
			}
		}
		
		List<String> oldTagDefUUIDs = db
		.select(TAGDEFINITION.UUID)
		.from(TAGDEFINITION)
		.where(TAGDEFINITION.TAGSETDEFINITIONID.eq(tagsetDefinition.getId()))
		.fetch()
		.map(new UUIDByteToStringFieldMapper());
		
		
		HashSet<byte[]> toBeDeleted = new HashSet<byte[]>();
		for (String uuid : oldTagDefUUIDs) {
			if (!tagsetDefinition.hasTagDefinition(uuid)) {
				toBeDeleted.add(idGenerator.catmaIDToUUIDBytes(uuid));
			}
			
		}
		
		db.batch(
			db
			.delete(PROPERTYDEF_POSSIBLEVALUE)
			.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.in(db
				.select(PROPERTYDEFINITION.PROPERTYDEFINITIONID)
				.from(PROPERTYDEFINITION)
				.join(TAGDEFINITION)
					.on(TAGDEFINITION.TAGDEFINITIONID.eq(PROPERTYDEFINITION.TAGDEFINITIONID))
					.and(TAGDEFINITION.UUID.in(toBeDeleted)))),
			db
			.delete(PROPERTYDEFINITION)
			.where(PROPERTYDEFINITION.TAGDEFINITIONID.in(db
				.select(TAGDEFINITION.TAGDEFINITIONID)
				.from(TAGDEFINITION)
				.where(TAGDEFINITION.UUID.in(toBeDeleted)))),
			db
			.delete(TAGDEFINITION)
			.where(TAGDEFINITION.UUID.in(toBeDeleted)))
		.execute();
	}

	private void updateDeepTagDefinition(
			DSLContext db, TagDefinition tagDefinition) {
		
		db
		.update(TAGDEFINITION)
		.set(TAGDEFINITION.NAME, tagDefinition.getName())
		.set(TAGDEFINITION.VERSION, SqlTimestamp.from(tagDefinition.getVersion().getDate()))
		.where(TAGDEFINITION.TAGDEFINITIONID.eq(tagDefinition.getId()))
		.execute();
		
		for (PropertyDefinition pd : tagDefinition.getSystemPropertyDefinitions()) {
			if (pd.getId() == null) {
				createDeepPropertyDefinition(db, pd, tagDefinition.getId(), true);
			}
			else {
				updateDeepPropertyDefinition(db, pd, true);
			}
		}
		
		for (PropertyDefinition pd : tagDefinition.getUserDefinedPropertyDefinitions()) {
			if (pd.getId() == null) {
				createDeepPropertyDefinition(db, pd, tagDefinition.getId(), false);
			}
			else {
				updateDeepPropertyDefinition(db, pd, false);
			}
		}
		
		HashSet<byte[]> existingPropertyDefinitionUUIDs = 
				new HashSet<byte[]>();
				
		PropertyDefinitionToByteUUID pd2ByteUUID = new PropertyDefinitionToByteUUID();
		
		existingPropertyDefinitionUUIDs.addAll(
			Collections2.transform(
				tagDefinition.getSystemPropertyDefinitions(),
				pd2ByteUUID));
		existingPropertyDefinitionUUIDs.addAll(
			Collections2.transform(
				tagDefinition.getUserDefinedPropertyDefinitions(),
				pd2ByteUUID));
		
		List<byte[]> oldPropertyDefinitionUUIDs = 
		db
		.select(PROPERTYDEFINITION.UUID)
		.from(PROPERTYDEFINITION)
		.where(PROPERTYDEFINITION.TAGDEFINITIONID.eq(tagDefinition.getId()))
		.fetch()
		.map(new FieldToValueMapper<byte[]>(PROPERTYDEFINITION.UUID));
		
		Collection<byte[]> toBeDeleted = 
			Collections3.getSetDifference(
				oldPropertyDefinitionUUIDs, existingPropertyDefinitionUUIDs);
		db.batch(
			db
			.delete(PROPERTYDEF_POSSIBLEVALUE)
			.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.in(db
				.select(PROPERTYDEFINITION.PROPERTYDEFINITIONID)
				.from(PROPERTYDEFINITION)
				.where(PROPERTYDEFINITION.UUID.in(toBeDeleted)))),
			db
			.delete(PROPERTYDEFINITION)
			.where(PROPERTYDEFINITION.UUID.in(toBeDeleted)))
		.execute();

	}

	private void updateDeepPropertyDefinition(DSLContext db,
			PropertyDefinition pd, boolean isSystemProperty) {

		if (!isSystemProperty) {
			db
			.update(PROPERTYDEFINITION)
			.set(PROPERTYDEFINITION.NAME, pd.getName())
			.where(PROPERTYDEFINITION.PROPERTYDEFINITIONID.eq(pd.getId()))
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
		
		db
		.delete(PROPERTYDEF_POSSIBLEVALUE)
		.where(PROPERTYDEF_POSSIBLEVALUE.PROPERTYDEFINITIONID.eq(pd.getId()))
		.and(PROPERTYDEF_POSSIBLEVALUE.VALUE.in(toBeDeleted))
		.execute();
		
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
	
	
	public void update(final TagLibraryReference tagLibraryReference) {
		//HIER GEHTS WEITER
	}

	
	
	public void update2(final TagLibraryReference tagLibraryReference) {
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
