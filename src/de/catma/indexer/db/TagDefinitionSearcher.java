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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;

import de.catma.db.CloseableSession;
import de.catma.document.Range;
import de.catma.indexer.db.model.DBIndexProperty;
import de.catma.indexer.db.model.DBIndexTagReference;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.TagQueryResult;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.util.CloseSafe;
import de.catma.util.IDGenerator;

public class TagDefinitionSearcher {

	private SessionFactory sessionFactory;
	private Logger logger = Logger.getLogger(this.getClass().getName());

	public TagDefinitionSearcher(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public QueryResult search(
			List<String> userMarkupCollectionIdList, String tagDefinitionPath) {
		
		Session session = sessionFactory.openSession();
		try {
			List<DBIndexTagReference> tagReferences  = 
					searchInUserMarkupCollection(
							session, userMarkupCollectionIdList, tagDefinitionPath);
			logger.info(
				"Query for " + tagDefinitionPath + " has " + 
						tagReferences.size() + " results.");
			return createTagQueryResult(tagReferences, tagDefinitionPath);
		}
		finally {
			CloseSafe.close(new CloseableSession(session));
		}
	}
	
	private QueryResult createTagQueryResult(
			List<DBIndexTagReference> tagReferences, String group) {
		TagQueryResult result = new TagQueryResult(group);
		
		HashMap<String, Set<DBIndexTagReference>> groupByInstance = 
				new HashMap<String, Set<DBIndexTagReference>>();
		for (DBIndexTagReference tr : tagReferences) {
			String tagInstanceId = 
					tr.getCatmaTagInstanceId();
			
			if (!groupByInstance.containsKey(tagInstanceId)) {
				groupByInstance.put(
						tagInstanceId, new HashSet<DBIndexTagReference>());
			}
			groupByInstance.get(tagInstanceId).add(tr);
		}
		
		for (Map.Entry<String,Set<DBIndexTagReference>> entry :
			groupByInstance.entrySet()) {
			
			SortedSet<Range> ranges = new TreeSet<Range>();
			for (DBIndexTagReference tr : entry.getValue()) {
				ranges.add(
					new Range(tr.getCharacterStart(), tr.getCharacterEnd()));
			}
			DBIndexTagReference firstDBTagRef = entry.getValue().iterator().next();
			
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
	private List<DBIndexTagReference> searchInUserMarkupCollection(
			Session session, List<String> userMarkupCollectionIdList, 
			String tagDefinitionPath) {
		
		if (!tagDefinitionPath.startsWith("/")) {
			tagDefinitionPath = "%" + tagDefinitionPath;
		}
		
		Criteria criteria = session.createCriteria(DBIndexTagReference.class).
				add(Restrictions.ilike("tagDefinitionPath", tagDefinitionPath));
		
		if (!userMarkupCollectionIdList.isEmpty()) {
			criteria.add(
				Restrictions.in(
					"userMarkupCollectionId", userMarkupCollectionIdList));
		}
		
		return criteria.list();
	}


	public QueryResult searchProperties(
			List<String> userMarkupCollectionIdList, 
			Set<String> propertyDefinitionIDs,
			String propertyName,
			String propertyValue, String tagValue) {
		
		IDGenerator idGenerator = new IDGenerator();
		List<byte[]> byteUuidSet = new ArrayList<byte[]>();
		
		for (String propertyDefinitionID : propertyDefinitionIDs) {
			byteUuidSet.add(idGenerator.catmaIDToUUIDBytes(propertyDefinitionID));
		}
		
		String hql = " select tr from " + 
				DBIndexTagReference.class.getSimpleName() + " tr, " +
				DBIndexProperty.class.getSimpleName() + " p " +
				" where tr.tagInstanceId = p.tagInstanceId and " +
				" p.propertyDefinitionId in :byteUuidSet " +
				" and tr.userMarkupCollectionId in :umcList ";
		
		if ((propertyValue != null) && (!propertyValue.isEmpty())) {
			hql += " and p.value = :propertyValue ";
		}
		
		if ((tagValue != null) && (!tagValue.isEmpty())) {
			if (!tagValue.startsWith("/")) {
				tagValue = "%" + tagValue;
			}
			hql += " and lower(tr.tagDefinitionPath) like :tagValue "; 
		}
		Session session = sessionFactory.openSession();
		try {
			Query query = session.createQuery(hql);
			
			query.setParameterList("byteUuidSet", byteUuidSet);
			query.setParameterList("umcList", userMarkupCollectionIdList);
			
			if ((propertyValue != null) && (!propertyValue.isEmpty())) {
				query.setParameter("propertyValue", propertyValue);
			}
			if ((tagValue != null) && (!tagValue.isEmpty())) {
				query.setParameter("tagValue", tagValue.toLowerCase());
			}
			
			@SuppressWarnings("unchecked")
			List<DBIndexTagReference> dbTagReferences = query.list();
		
			return createTagQueryResult(
				dbTagReferences, 
				propertyName + 
					(((propertyValue==null)||propertyValue.isEmpty())?"":
						(":"+propertyValue)));
		}
		finally {
			CloseSafe.close(new CloseableSession(session));
		}
	}

}
