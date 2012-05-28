package de.catma.indexer.db;

import java.util.List;

import org.hibernate.Session;

import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.tag.ITagLibrary;
import de.catma.util.IDGenerator;

public class TagReferenceIndexer {

	public void index(Session session, List<TagReference> tagReferences,
			String sourceDocumentID, String userMarkupCollectionID,
			ITagLibrary tagLibrary) {
		
		IDGenerator idGenerator = new IDGenerator();
		
		session.beginTransaction();

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
		
		session.getTransaction().commit();
	}
}
