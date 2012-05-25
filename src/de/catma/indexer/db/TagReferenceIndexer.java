package de.catma.indexer.db;

import java.util.List;

import org.hibernate.Session;

import de.catma.core.document.standoffmarkup.usermarkup.TagReference;
import de.catma.core.tag.ITagLibrary;
import de.catma.core.util.IDGenerator;

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
							tr.getTagDefinition().getID()),
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
