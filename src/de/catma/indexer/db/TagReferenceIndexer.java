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
				"DELETE FROM " + DBIndexProperty.TABLENAME 
				+ " WHERE tagInstanceID in ( SELECT r.tagInstanceID from "
				+ DBIndexTagReference.TABLENAME
				+ " WHERE r.userMarkupCollectionID = '" + userMarkupCollectionID + "')" 
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
				" where tagInstanceId = :curTagInstanceId ");
		byte[] tagInstanceUuid = idGenerator.catmaIDToUUIDBytes(tagInstance.getUuid()); 
		propDelQuery.setBinary("curTagInstanceId", tagInstanceUuid);
		propDelQuery.executeUpdate();
		
		indexProperty(session, property, tagInstanceUuid);
	}

	public void reindex(Session session, TagsetDefinition tagsetDefinition,
			Set<byte[]> deletedTagDefinitionUuids, 
			UserMarkupCollection userMarkupCollection, String sourceDocumentID) {
		
		SQLQuery delPropQuery = session.createSQLQuery("" +
				"DELETE FROM " + DBIndexProperty.TABLENAME 
				+ " WHERE tagInstanceID IN (SELECT r.tagInstanceID FROM "
				+ DBIndexTagReference.TABLENAME 
				+ " r WHERE r.tagDefinitionId = :curTagDefinitionId)");
		
		Query  delTrQuery = session.createQuery(
				"delete from " + DBIndexTagReference.class.getSimpleName() +
				" where tagDefinitionId = :curTagDefinitionId");
		
		session.beginTransaction();		
		
		for (TagDefinition td : tagsetDefinition) {
			logger.info("reindexing: deleting refs for " + td);
			byte[] tagDefUuid = idGenerator.catmaIDToUUIDBytes(td.getUuid());
			delTrQuery.setBinary("curTagDefinitionId", tagDefUuid);
			delTrQuery.executeUpdate();
			//TODO: handle deleted properties
		}

		for (byte[] uuid : deletedTagDefinitionUuids) {
			logger.info(
				"reindexing: deleting refs for deleted TagDef " 
						+ idGenerator.uuidBytesToCatmaID(uuid));
			delTrQuery.setBinary("curTagDefinitionId", uuid);
			delTrQuery.executeUpdate();
			delPropQuery.setBinary("curTagDefinitionId", uuid);
			delPropQuery.executeUpdate();
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
