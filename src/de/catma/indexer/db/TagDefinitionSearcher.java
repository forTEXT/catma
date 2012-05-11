package de.catma.indexer.db;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import de.catma.core.document.Range;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;

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
		logger.info(
			"Query for " + tagDefinitionPath + " has " + 
					tagReferences.size() + " results.");
		
		QueryResultRowArray result = new QueryResultRowArray();
		HashMap<String, Set<DBTagReference>> groupByInstance = 
				new HashMap<String, Set<DBTagReference>>();
		for (DBTagReference tr : tagReferences) {
			String tagInstanceId = 
					tr.getCatmaTagInstanceId();
			
			if (!groupByInstance.containsKey(tagInstanceId)) {
				groupByInstance.put(
						tagInstanceId, new HashSet<DBTagReference>());
			}
			groupByInstance.get(tagInstanceId).add(tr);
		}
		
		for (Map.Entry<String,Set<DBTagReference>> entry :
			groupByInstance.entrySet()) {
			
			SortedSet<Range> ranges = new TreeSet<Range>();
			for (DBTagReference tr : entry.getValue()) {
				ranges.add(
					new Range(tr.getCharacterStart(), tr.getCharacterEnd()));
			}
			DBTagReference firstDBTagRef = entry.getValue().iterator().next();
			
			List<Range> mergedRanges = Range.mergeRanges(ranges);
			result.add(
				new TagQueryResultRow(
					firstDBTagRef.getDocumentId(),
					mergedRanges, 
					firstDBTagRef.getUserMarkupCollectionId(),
					firstDBTagRef.getCatmaTagDefinitionId(),
					entry.getKey()));
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private List<DBTagReference> searchInUserMarkupCollection(
			Session session, String userMarkupCollectionIdList, 
			String tagDefinitionPath) {
		if (!tagDefinitionPath.startsWith("/")) {
			tagDefinitionPath = "%" + tagDefinitionPath;
		}
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
