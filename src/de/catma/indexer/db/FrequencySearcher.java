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

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import de.catma.indexer.db.model.DBPosition;
import de.catma.indexer.db.model.DBTerm;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;

class FrequencySearcher {
	
	private SessionFactory sessionFactory;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public FrequencySearcher(SessionFactory sessionFactory) {
		super();
		this.sessionFactory = sessionFactory;
	}

	public QueryResult search(
			List<String> documentIdList, CompareOperator comp1, int freq1,
			CompareOperator comp2, int freq2) {

		HashMap<String, LazyDBPhraseQueryResult> phraseResultMapping = 
				new HashMap<String, LazyDBPhraseQueryResult>();
		Session session = sessionFactory.openSession();
		
		if (documentIdList.isEmpty()) {
			throw new IllegalArgumentException("documentIdList cannot be empty");
		}
		else {
			StringBuilder documentIdListAsString = new StringBuilder(" (");
			String conc = "";
			for (String documentId : documentIdList) {
				documentIdListAsString.append(conc);
				documentIdListAsString.append("'");
				documentIdListAsString.append(documentId);
				documentIdListAsString.append("'");
				conc = ",";
			}
			documentIdListAsString.append( ") ");
			
			for (String documentId : documentIdList) {
				addResultToMapping(
					getResultsFor(
							documentIdListAsString.toString(),
							documentId, session, comp1, freq1, comp2, freq2), 
					phraseResultMapping);
			}
		}
		
		GroupedQueryResultSet groupedQueryResultSet = new GroupedQueryResultSet();
		groupedQueryResultSet.addAll(phraseResultMapping.values());
		
		return groupedQueryResultSet;
	}
	
	@SuppressWarnings("rawtypes")
	private List getResultsFor(
			String documentIdList,
			String documentId,
			Session session, CompareOperator comp1, int freq1,
			CompareOperator comp2, int freq2) {
		StringBuilder builder = new StringBuilder();
		builder.append(
				" select t, count(p) as freq from "
				+ DBTerm.class.getSimpleName() + " t, "
				+ DBPosition.class.getSimpleName() 
				+ " p " +
				" where t = p.term ");
		
		builder.append(" and t.documentId = '");
		builder.append(documentId);
		builder.append("'");

		if ((freq1 > 0) || (!comp1.equals(CompareOperator.GREATERTHAN))) {
			builder.append(
					" and (" +
						" select count(p2) " +
						" from "
					+ DBPosition.class.getSimpleName() 
					+ " p2, "
					+ DBTerm.class.getSimpleName() + " t2 " +
						" where p2.term = t2 and t2.term = t.term ");
			
			if (documentId != null) {
				builder.append(" and t2.documentId in ");
				builder.append(documentIdList);
			}		
			builder.append(") ");
			builder.append(comp1);
			builder.append(" ");
			builder.append(freq1);
		}
		
		if (comp2 != null) {
			builder.append(
					" and (" +
						" select count(p2) " +
						" from "
				+ DBPosition.class.getSimpleName() 
				+ " p2, "
				+ DBTerm.class.getSimpleName() + " t2 " +
						" where p2.term = t2 and t2.term = t.term ");
			if (documentId != null) {
				builder.append(" and t2.documentId in ");
				builder.append(documentIdList);
			}		
			builder.append(") ");
			builder.append(comp2);
			builder.append(" ");
			builder.append(freq2);
		}
		builder.append(" group by t");
		String query = builder.toString();
		logger.info("query: " + query);
		
		Query q = 
				session.createQuery(query);
		
		return q.list();
	}

	private void addResultToMapping(@SuppressWarnings("rawtypes") List list,
			HashMap<String, LazyDBPhraseQueryResult> phraseResultMapping) {
		
		for (Object resultRow : list) {
			DBTerm t = (DBTerm)((Object[])resultRow)[0];
			Integer freq = Integer.valueOf(((Object[])resultRow)[1].toString());
			
			//TODO: remove freq column from table Term or are we running in performance problems then? 
			if (t.getFrequency() != freq) {
				throw new IllegalStateException(
					"t.getFrequency() != freq : " + t.getFrequency() + " != " + freq );
			}
			
			t.setFrequency(freq);
			
			if (!phraseResultMapping.containsKey(t.getTerm())) {
				phraseResultMapping.put(
					t.getTerm(), 
					new LazyDBPhraseQueryResult(sessionFactory, t.getTerm()));
			}
			
			phraseResultMapping.get(t.getTerm()).addTerm(t);
		}
		
	}
	
	

}
