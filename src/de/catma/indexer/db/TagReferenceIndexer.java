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
package de.catma.indexer.db;

import static de.catma.repository.db.jooqgen.catmaindex.Tables.PROPERTY;
import static de.catma.repository.db.jooqgen.catmaindex.Tables.TAGREFERENCE;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jooq.BatchBindStep;
import org.jooq.SQLDialect;

import com.google.common.collect.Collections2;

import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.indexer.TagsetDefinitionUpdateLog;
import de.catma.repository.db.jooq.TransactionalDSLContext;
import de.catma.repository.db.jooq.UUIDtoByteMapper;
import de.catma.repository.db.mapper.FieldToValueMapper;
import de.catma.tag.Property;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.util.Collections3;
import de.catma.util.IDGenerator;

public class TagReferenceIndexer {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private IDGenerator idGenerator;

	private DataSource dataSource;

	public TagReferenceIndexer() throws NamingException {
		this.idGenerator = new IDGenerator();
		Context  context = new InitialContext();
		this.dataSource = (DataSource) context.lookup("catmads");
	}


	public void index(List<TagReference> tagReferences,
			String sourceDocumentID, String userMarkupCollectionID,
			TagLibrary tagLibrary) throws IOException {
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			db.beginTransaction();
			BatchBindStep insertTagRefBatch = db.batch(db
			.insertInto(
				TAGREFERENCE,
					TAGREFERENCE.DOCUMENTID,
					TAGREFERENCE.USERMARKUPCOLLECTIONID,
					TAGREFERENCE.TAGDEFINITIONPATH,
					TAGREFERENCE.TAGDEFINITIONID,
					TAGREFERENCE.TAGINSTANCEID,
					TAGREFERENCE.TAGDEFINITIONVERSION,
					TAGREFERENCE.CHARACTERSTART,
					TAGREFERENCE.CHARACTEREND)
			.values(
				(String)null,
				(String)null,
				(String)null,
				(byte[])null,
				(byte[])null,
				(String)null,
				(Integer)null,
				(Integer)null));
			
			BatchBindStep insertPropertyBatch = db.batch(db
			.insertInto(
				PROPERTY,
					PROPERTY.TAGINSTANCEID,
					PROPERTY.PROPERTYDEFINITIONID,
					PROPERTY.VALUE)
			.values(
				(byte[])null,
				(byte[])null,
				(String)null));
					
			
			for (TagReference tr : tagReferences) {
				byte[] tagInstanceUUIDBytes = idGenerator.catmaIDToUUIDBytes(
						tr.getTagInstanceID());
				insertTagRefBatch.bind(
					sourceDocumentID,
					userMarkupCollectionID,
					tagLibrary.getTagPath(tr.getTagDefinition()),
					idGenerator.catmaIDToUUIDBytes(
							tr.getTagDefinition().getUuid()),
					tagInstanceUUIDBytes,
					tr.getTagDefinition().getVersion().toString(),
					tr.getRange().getStartPoint(),
					tr.getRange().getEndPoint());
			
				for (Property property : tr.getTagInstance().getSystemProperties()) {
					byte[] propertyDefUUIDBytes = idGenerator.catmaIDToUUIDBytes(
							property.getPropertyDefinition().getUuid());
					for (String value : property.getPropertyValueList().getValues()) {
						insertPropertyBatch.bind(
							tagInstanceUUIDBytes,
							propertyDefUUIDBytes,
							value);
					}
				}
				
				//user defined properties get indexed individually!
			}
			
			insertTagRefBatch.execute();
			insertPropertyBatch.execute();
			
			db.commitTransaction();
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
	
	
	public void removeUserMarkupCollection(String userMarkupCollectionID) throws IOException {
		
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			db.beginTransaction();
			
			db
			.delete(PROPERTY)
			.where(PROPERTY.TAGINSTANCEID.in(db
				.select(TAGREFERENCE.TAGINSTANCEID)
				.from(TAGREFERENCE)
				.where(TAGREFERENCE.USERMARKUPCOLLECTIONID
						.eq(userMarkupCollectionID))))
			.execute();
			
			db
			.delete(TAGREFERENCE)
			.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq(userMarkupCollectionID))
			.execute();
				
			db.commitTransaction();
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

	
	public void removeTagReferences(
			List<TagReference> tagReferences) throws IOException {
		
		// we delete full TagInstances and attached properties
		// removal of some TagReferences while keeping others of the same TagInstance
		// should be implemented as an update of a TagInstance
	
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		Set<String> tagInstanceUUIDs = new HashSet<String>();
		for (TagReference tagRef : tagReferences) {
			tagInstanceUUIDs.add(tagRef.getTagInstanceID());
		}
		Set<byte[]> tagInstanceUUIDBytes = new HashSet<byte[]>();
		for (String tagInstanceUUID : tagInstanceUUIDs) {
			tagInstanceUUIDBytes.add(idGenerator.catmaIDToUUIDBytes(tagInstanceUUID));
		}
		
		try {
			db.beginTransaction();

			db
			.delete(PROPERTY)
			.where(PROPERTY.TAGINSTANCEID.in(tagInstanceUUIDBytes))
			.execute();
			
			db
			.delete(TAGREFERENCE)
			.where(TAGREFERENCE.TAGINSTANCEID.in(tagInstanceUUIDBytes))
			.execute();
			
			db.commitTransaction();
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
	
	void reIndexProperty(TagInstance tagInstance, Property property) throws IOException {
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			byte[] tagInstanceUUIDBytes = 
					idGenerator.catmaIDToUUIDBytes(tagInstance.getUuid());
			byte[] propDefUUIDBytes = 
				idGenerator.catmaIDToUUIDBytes(
						property.getPropertyDefinition().getUuid());
			
			List<String> existingValues = db
					.select(PROPERTY.VALUE)
					.from(PROPERTY)
					.where(PROPERTY.TAGINSTANCEID.eq(tagInstanceUUIDBytes))
					.and(PROPERTY.PROPERTYDEFINITIONID.eq(propDefUUIDBytes))
					.fetch()
					.map(new FieldToValueMapper<String>(PROPERTY.VALUE));
			
			Collection<String> toBeIndexed = 
					Collections3.getSetDifference(
						property.getPropertyValueList().getValues(), existingValues);
			
			db.beginTransaction();

			db
			.delete(PROPERTY)
			.where(PROPERTY.TAGINSTANCEID.eq(tagInstanceUUIDBytes))
			.and(PROPERTY.PROPERTYDEFINITIONID.eq(propDefUUIDBytes))
			.and(PROPERTY.VALUE.notIn(property.getPropertyValueList().getValues()))
			.execute();
			
			BatchBindStep insertPropertyBatch = db.batch(db
			.insertInto(
				PROPERTY,
					PROPERTY.TAGINSTANCEID,
					PROPERTY.PROPERTYDEFINITIONID,
					PROPERTY.VALUE)
			.values(
				(byte[])null,
				(byte[])null,
				(String)null));
					
			for (String value : toBeIndexed) {
				insertPropertyBatch.bind(
					tagInstanceUUIDBytes,
					propDefUUIDBytes,
					value);
			}
			
			insertPropertyBatch.execute();
			
			db.commitTransaction();
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
	
	public void reindex(TagsetDefinition tagsetDefinition,
			TagsetDefinitionUpdateLog tagsetDefinitionUpdateLog, 
			UserMarkupCollection userMarkupCollection) throws IOException {
		
		TransactionalDSLContext db = 
				new TransactionalDSLContext(dataSource, SQLDialect.MYSQL);

		try {
			
			db.beginTransaction();
			
			Set<String> updatedTagDefinitionUUIDs = tagsetDefinitionUpdateLog.getUpdatedTagDefinitionUuids();
			
			
			// reindex updated TagDef names and versions
			if (!updatedTagDefinitionUUIDs.isEmpty()) {
				BatchBindStep updateTagDefBatch = db.batch(db
					.update(TAGREFERENCE)
					.set(TAGREFERENCE.TAGDEFINITIONPATH, (String)null)
					.set(TAGREFERENCE.TAGDEFINITIONVERSION, (String)null)
					.where(TAGREFERENCE.TAGDEFINITIONID.eq((byte[])null))
					.and(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq((String)null)));
				
				for (String tagDefUUID : updatedTagDefinitionUUIDs) {
					TagDefinition tagDef = tagsetDefinition.getTagDefinition(tagDefUUID);
					byte[] tagDefUUIDByte = idGenerator.catmaIDToUUIDBytes(tagDefUUID);
					
					updateTagDefBatch.bind(
						tagsetDefinition.getTagPath(tagDef), 
						tagDef.getVersion().toString(),
						tagDefUUIDByte,
						userMarkupCollection.getId());
				}
				updateTagDefBatch.execute();
			}
			
			// reindex updated TagDef System Property values (mainly color)
			
			//tagDefUuid->Set<SystemPropertyDefUuid>
			Map<String, Set<String>> updatedPropertyDefinitionUUIDs = 
					tagsetDefinitionUpdateLog.getUpdatedPropertyDefinitionUuids();
			if (!updatedPropertyDefinitionUUIDs.isEmpty()) {

				BatchBindStep updatePropertyBatch = db.batch(db
				.update(PROPERTY)
				.set(PROPERTY.VALUE, (String)null)
				.where(PROPERTY.PROPERTYDEFINITIONID.eq((byte[])null))
				.and(PROPERTY.TAGINSTANCEID.in(db
					.select(TAGREFERENCE.TAGINSTANCEID)
					.from(TAGREFERENCE)
					.where(TAGREFERENCE.USERMARKUPCOLLECTIONID.eq((String)null)))));
			
				for (Map.Entry<String, Set<String>> entry : updatedPropertyDefinitionUUIDs.entrySet()) {
					String tagDefUuid = entry.getKey();
					TagDefinition tagDef = tagsetDefinition.getTagDefinition(tagDefUuid);
					for (String propDefUuid : entry.getValue()) {
						PropertyDefinition propDef = tagDef.getPropertyDefinition(propDefUuid);
						updatePropertyBatch.bind(
							propDef.getFirstValue(),
							idGenerator.catmaIDToUUID(propDefUuid),
							userMarkupCollection.getId());
					}
				}
				
				updatePropertyBatch.execute();
			}
					
			
			// remove deleted Property Defs 
			Set<String> deletedPropertyDefinitionUUIDs =
					tagsetDefinitionUpdateLog.getDeletedPropertyDefinitionUuids();
			
			if (!deletedPropertyDefinitionUUIDs.isEmpty()) {
				db
				.delete(PROPERTY)
				.where(PROPERTY.PROPERTYDEFINITIONID.in(
					Collections2.transform(deletedPropertyDefinitionUUIDs, new UUIDtoByteMapper())))
				.and(PROPERTY.TAGINSTANCEID.in(db
					.select(TAGREFERENCE.TAGINSTANCEID)
					.from(TAGREFERENCE)
					.where(TAGREFERENCE.USERMARKUPCOLLECTIONID
							.eq(userMarkupCollection.getId()))))
				.execute();
			}
			
			// remove deleted Tag Defs and their Properties
			
			Set<String> deletedTagDefinitionUUIDs = 
					tagsetDefinitionUpdateLog.getDeletedTagDefinitionUuids();
			
			if (!deletedTagDefinitionUUIDs.isEmpty()) {
				Collection<byte[]> deletedTagDefinitionUUIDBytes = 
					Collections2.transform(
							deletedPropertyDefinitionUUIDs, new UUIDtoByteMapper());
				db.batch(
					db
					.delete(PROPERTY)
					.where(PROPERTY.TAGINSTANCEID.in(db
						.select(TAGREFERENCE.TAGINSTANCEID)
						.from(TAGREFERENCE)
						.where(TAGREFERENCE.TAGDEFINITIONID
								.in(deletedTagDefinitionUUIDBytes))
						.and(TAGREFERENCE.USERMARKUPCOLLECTIONID
								.eq(userMarkupCollection.getId())))),
					db
					.delete(TAGREFERENCE)
					.where(TAGREFERENCE.TAGDEFINITIONID
							.in(deletedTagDefinitionUUIDBytes))
					.and(TAGREFERENCE.USERMARKUPCOLLECTIONID
							.eq(userMarkupCollection.getId())))
				.execute();
			}
			
			db.commitTransaction();
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
}
