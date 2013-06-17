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

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.indexer.db.model.DBIndexProperty;
import de.catma.indexer.db.model.DBIndexTagReference;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.util.IDGenerator;

public class TagReferenceIndexer {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private IDGenerator idGenerator;

	public TagReferenceIndexer() {
		this.idGenerator = new IDGenerator();
	}

	public void index(Session session, List<TagReference> tagReferences,
			String sourceDocumentID, String userMarkupCollectionID,
			TagLibrary tagLibrary) {
		
		IDGenerator idGenerator = new IDGenerator();
		
		boolean transactionStartedLocally = false;
		if (!session.getTransaction().isActive()) {
			session.beginTransaction();
			transactionStartedLocally = true;
		}
		
		for (TagReference tr : tagReferences) {
			logger.info("indexing " + tr);
			DBIndexTagReference dbTagReference = 
				new DBIndexTagReference(
					sourceDocumentID, 
					userMarkupCollectionID, 
					tagLibrary.getTagPath(tr.getTagDefinition()),
					idGenerator.catmaIDToUUIDBytes(
							tr.getTagDefinition().getUuid()),
					tr.getTagDefinition().getVersion().toString(),
					idGenerator.catmaIDToUUIDBytes(
							tr.getTagInstanceID()),
					tr.getRange().getStartPoint(),
					tr.getRange().getEndPoint());
			session.save(dbTagReference);
			for (Property property : tr.getTagInstance().getSystemProperties()) {
				indexProperty(
					session, property, 
					idGenerator.catmaIDToUUIDBytes(tr.getTagInstanceID()));
			}
			
			//user defined properties get indexed individually!
		}
		
		if (transactionStartedLocally) {
			session.getTransaction().commit();
		}
	}
	
	private void indexProperty(Session session, Property property, byte[] tagInstanceUuid) {
		for (String value : property.getPropertyValueList().getValues()) {
			DBIndexProperty dbIndexProperty = 
					new DBIndexProperty(
							tagInstanceUuid,
							idGenerator.catmaIDToUUIDBytes(
									property.getPropertyDefinition().getUuid()),
							value);
			session.save(dbIndexProperty);
		}
	}

	public void removeUserMarkupCollection(
			Session session, String userMarkupCollectionID) {
		
		boolean tranStartedLocally = false;
		if (!session.getTransaction().isActive()) {
			session.beginTransaction();
			tranStartedLocally = true;
		}
		SQLQuery sqlQuery = session.createSQLQuery(
				"DELETE p FROM " + DBIndexProperty.TABLENAME + " p "
				+ " JOIN "
				+ DBIndexTagReference.TABLENAME + " r "
				+ " ON p.tagInstanceID = r.tagInstanceID and " 
				+ " r.userMarkupCollectionID =  '" + userMarkupCollectionID + "'" 
				);
		sqlQuery.executeUpdate();
		
		Query query = session.createQuery(
				"delete from " + DBIndexTagReference.class.getSimpleName() 
				+ " where userMarkupCollectionId = '" + userMarkupCollectionID 
				+ "'");
		query.executeUpdate();
		if (tranStartedLocally) {
			session.getTransaction().commit();
		}
	}

	public void removeTagReferences(Session session,
			List<TagReference> tagReferences) {
		
		Query trDelQuery = session.createQuery(
				"delete from " + DBIndexTagReference.class.getSimpleName() + 
				" where tagInstanceId = :curTagInstanceId " +
				" and characterStart = :curCharacterStart " +
				" and characterEnd = :curCharacterEnd ");
		
		Query propDelQuery = session.createQuery(
				"delete from " + DBIndexProperty.class.getSimpleName() +
				" where tagInstanceId = :curTagInstanceId ");

		session.beginTransaction();
		for (TagReference tagReference : tagReferences) {
			byte[] tagInstanceID = 
					idGenerator.catmaIDToUUIDBytes(tagReference.getTagInstanceID());
			trDelQuery.setBinary("curTagInstanceId", tagInstanceID);
			trDelQuery.setInteger("curCharacterStart", tagReference.getRange().getStartPoint());
			trDelQuery.setInteger("curCharacterEnd", tagReference.getRange().getEndPoint());
			int rowCount = trDelQuery.executeUpdate();
			if (rowCount > 1) {
				throw new IllegalStateException(
					"deleted more than one row at a time, expected exactly one row");
			}
			//TODO: this works if all refs of an instance get deleted at once (which is the case right now) 
			propDelQuery.setBinary("curTagInstanceId", tagInstanceID);
			propDelQuery.executeUpdate();
		}
		session.getTransaction().commit();
	}
	
	void reIndexProperty(Session session, TagInstance tagInstance, Property property) {
		Query propDelQuery = session.createQuery(
				"delete from " + DBIndexProperty.class.getSimpleName() +
				" where tagInstanceId = :curTagInstanceId " +
				" and propertyDefinitionId = :curPropDefId ");
		byte[] tagInstanceUuid = idGenerator.catmaIDToUUIDBytes(tagInstance.getUuid()); 
		propDelQuery.setBinary("curTagInstanceId", tagInstanceUuid);
		byte[] propDefUuid = idGenerator.catmaIDToUUIDBytes(property.getPropertyDefinition().getUuid());
		propDelQuery.setBinary("curPropDefId", propDefUuid);
		
		propDelQuery.executeUpdate();
		
		indexProperty(session, property, tagInstanceUuid);
	}

	public void reindex(Session session, TagsetDefinition tagsetDefinition,
			Set<byte[]> deletedTagDefinitionUuids, 
			UserMarkupCollection userMarkupCollection, String sourceDocumentID) {
		
		SQLQuery delPropQuery = session.createSQLQuery("" +
				"DELETE p FROM " + DBIndexProperty.TABLENAME + " p " 
				+ " JOIN "
				+ DBIndexTagReference.TABLENAME + " r "
				+ " ON p.tagInstanceID = r.tagInstanceID and " 
				+ " r.tagDefinitionID = :curTagDefinitionId and "
				+ " r.userMarkupCollectionID = '" + userMarkupCollection.getId() + "'");
		
		Query  delTrQuery = session.createQuery(
				"delete from " + DBIndexTagReference.class.getSimpleName() +
				" where tagDefinitionId = :curTagDefinitionId and "
				+ " userMarkupCollectionId = '" + userMarkupCollection.getId() + "'");
		
		session.beginTransaction();		
		
		for (TagDefinition td : tagsetDefinition) {
			logger.info("reindexing: deleting refs for " + td);
			delTrQuery.setBinary("curTagDefinitionId", idGenerator.catmaIDToUUIDBytes(td.getUuid()));
			delTrQuery.executeUpdate();
			//TODO: handle deleted properties
		}

		for (byte[] uuid : deletedTagDefinitionUuids) {
			logger.info(
				"reindexing: deleting refs for deleted TagDef " 
						+ idGenerator.uuidBytesToCatmaID(uuid));
			delPropQuery.setBinary("curTagDefinitionId", uuid);
			delPropQuery.executeUpdate();
			delTrQuery.setBinary("curTagDefinitionId", uuid);
			delTrQuery.executeUpdate();
		}
		
		this.index(
				session, 
				userMarkupCollection.getTagReferences(tagsetDefinition), 
				sourceDocumentID, 
				userMarkupCollection.getId(), 
				userMarkupCollection.getTagLibrary());
		
		session.getTransaction().commit();
	}
}
