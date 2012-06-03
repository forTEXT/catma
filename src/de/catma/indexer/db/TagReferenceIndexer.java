package de.catma.indexer.db;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.util.IDGenerator;

public class TagReferenceIndexer {
	
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
			DBTagReference dbTagReference = 
				new DBTagReference(
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
		}
		
		if (transactionStartedLocally) {
			session.getTransaction().commit();
		}
	}

	public void removeUserMarkupCollection(
			Session session, String userMarkupCollectionID) {
		
		Query query = session.createQuery(
				"delete from " + DBTagReference.class.getSimpleName() 
				+ " where userMarkupCollectionId = '" + userMarkupCollectionID 
				+ "'");
		session.beginTransaction();
		query.executeUpdate();
		session.getTransaction().commit();
	}

	public void removeTagReferences(Session session,
			List<TagReference> tagReferences) {
		
		Query query = session.createQuery(
				"delete from " + DBTagReference.class.getSimpleName() + 
				" where tagInstanceId = :curTagInstanceId " +
				" and characterStart = :curCharacterStart " +
				" and characterEnd = :curCharacterEnd ");

		session.beginTransaction();
		for (TagReference tagReference : tagReferences) {
			query.setBinary(
				"curTagInstanceId", 
				idGenerator.catmaIDToUUIDBytes(tagReference.getTagInstanceID()));
			query.setInteger("curCharacterStart", tagReference.getRange().getStartPoint());
			query.setInteger("curCharacterEnd", tagReference.getRange().getEndPoint());
			int rowCount = query.executeUpdate();
			if (rowCount != 1) {
				throw new IllegalStateException(
					"deleted more than one row at a time, expected exactly one row");
			}
		}
		session.getTransaction().commit();
	}

	public void reindex(Session session, TagsetDefinition tagsetDefinition,
			UserMarkupCollection userMarkupCollection, String sourceDocumentID) {
		
		Query  query = session.createQuery(
				"delete from " + DBTagReference.class.getSimpleName() +
				" where tagDefinitionId = :curTagDefinitionId");
		
		session.beginTransaction();		
		
		for (TagDefinition td : tagsetDefinition) {
			query.setBinary("curTagDefinitionId",
					idGenerator.catmaIDToUUIDBytes(td.getUuid()));
			query.executeUpdate();
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
