package de.catma.indexer.db;

import java.util.List;
import java.util.logging.Logger;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import de.catma.core.document.Range;
import de.catma.core.util.IDGenerator;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class TagDefinitionSearcher {

	private SessionFactory sessionFactory;
	private Logger logger = Logger.getLogger(this.getClass().getName());

	public TagDefinitionSearcher(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public QueryResult search(List<String> documentIdList,
			List<String> userMarkupCollectionIdList, String tagDefinitionPath) {
		
		Session session = sessionFactory.openSession();
		
		List<DBTagReference> tagReferences  = 
				searchInUserMarkupCollection(session, null, tagDefinitionPath);
		
		IDGenerator idGenerator = new IDGenerator();
		
		QueryResultRowArray result = new QueryResultRowArray();
		for (DBTagReference tr : tagReferences) {
			result.add(
				new QueryResultRow(
					tr.getDocumentId(),
					new Range(tr.getCharacterStart(), tr.getCharacterEnd()), 
					tr.getUserMarkupColletionId(),
					idGenerator.uuidBytesToCatmaID(tr.getTagDefintionId()),
					idGenerator.uuidBytesToCatmaID(tr.getTagInstanceId())));
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private List<DBTagReference> searchInUserMarkupCollection(
			Session session, String userMarkupCollectionIdList, 
			String tagDefinitionPath) {

		String query =
				" from " +
				DBEntityName.DBTagReference +
				" where tagDefinitionPath like '" + tagDefinitionPath + "%'";
		
		logger.info("query: " + query);
		Query q = 
				session.createQuery(query);
		
		return q.list();
	}

}
