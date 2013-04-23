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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import de.catma.document.Range;
import de.catma.indexer.db.model.DBPosition;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.util.CloseSafe;

class PhraseSearcher {
	private static final int MAX_DIRECT_SEARCH_TERMS = 5;
	private SessionFactory sessionFactory;
//	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public PhraseSearcher(SessionFactory sessionFactory) {
		super();
		this.sessionFactory = sessionFactory;
	}

	public QueryResult search(List<String> documentIdList,
			String phrase, List<String> termList, int limit) throws IOException {
		
		if (documentIdList.isEmpty()) {
			throw new IllegalArgumentException("documentIdList cannot be empty");
		}
		
		final Session session = sessionFactory.openSession();
		try {
			QueryResultRowArray queryResult = new QueryResultRowArray();
			if (limit != 0) {
				for (String documentId : documentIdList) {
					limit -= ((QueryResultRowArray)queryResult).size();
					if  (limit >= 0) {
						queryResult.addAll(
							searchPhrase(
								session, documentId, phrase, termList, false, limit));
					}
				}
			}
			else {
				for (String documentId : documentIdList) {
					queryResult.addAll(
						searchPhrase(
							session, documentId, phrase, termList, false, limit));
				}
			}
			return queryResult;
		}
		finally {
			CloseSafe.close(new Closeable() {
				public void close() throws IOException {
					session.close();
				}
			});
		}
		
	}
	
	private QueryResultRowArray searchPhrase(Session session, String documentId,
			String phrase, List<String> termList, boolean withWildcards, int limit) throws IOException {
		
		if (termList.size() == 0) {
			return new QueryResultRowArray();
		}
		else {
			
			//TODO: case insensitivity

			SQLQuery spSearchPhrase = session.createSQLQuery(
						"call CatmaIndex.searchPhrase(" +
						":term1, :term2, :term3, :term4, :term5, :docID, :wild, :limitresult)");
			
			spSearchPhrase.setParameter("limitresult", limit);
			
			SQLQuery spGetTerms = null;
			
			if (termList.size() > MAX_DIRECT_SEARCH_TERMS) {
				spGetTerms = session.createSQLQuery("call CatmaIndex.getTerms(" +
						":docID, :basePos, :termCount)");
				spGetTerms.setParameter("docID", documentId);
			}
			
			int termCount=0;
			for (termCount=0; 
				termCount<Math.min(MAX_DIRECT_SEARCH_TERMS, 
				termList.size()); termCount++) {
				
				String term = termList.get(termCount);
				if (term.equals("%")) {
					spSearchPhrase.setParameter("term"+(termCount+1), null);
				}
				else {
					spSearchPhrase.setParameter("term"+(termCount+1), termList.get(termCount));
				}
			}
			
			for (int pIdx=termCount; pIdx<MAX_DIRECT_SEARCH_TERMS; pIdx++) {
				spSearchPhrase.setParameter("term"+(pIdx+1), null);
			}
			
			spSearchPhrase.setParameter("docID", documentId);
			spSearchPhrase.setParameter("wild", withWildcards);
			
			@SuppressWarnings("unchecked")
			List<Object[]> result = spSearchPhrase.list();
			
			QueryResultRowArray queryResult = new QueryResultRowArray();
			for (Object[] row : result) {
				if (termList.size() > MAX_DIRECT_SEARCH_TERMS) {
					spGetTerms.setParameter("basePos", (Integer)row[0]+MAX_DIRECT_SEARCH_TERMS-1);
					spGetTerms.setParameter("termCount", termList.size()-MAX_DIRECT_SEARCH_TERMS);
					@SuppressWarnings("unchecked")
					List<Object[]> termResult = spGetTerms.list();
					if (match(termResult, termList, withWildcards)) {
						queryResult.add(
								new QueryResultRow(
									documentId,
									new Range(
										(Integer)row[1],
										(Integer)termResult.get(termResult.size()-1)[2]),
									phrase));
					}
				}
				else {
					queryResult.add(
							new QueryResultRow(
									documentId, 
									new Range(
											(Integer)row[1],
											(Integer)row[2]),
											phrase));
				}
			}
			
			return queryResult;
		
		}		
	}
	
	private boolean match(
			List<Object[]> termResult, List<String> termList, boolean withWildcards) {
		for (int i=0; i<termResult.size(); i++) {
			String curTermResult = (String)termResult.get(i)[0];
			String curTerm = termList.get(MAX_DIRECT_SEARCH_TERMS+i);
			if (!withWildcards) {
				if (!curTermResult.equals(curTerm)) {
					return false;
				}
			}
			else {
				
				String[] percentParts = curTerm.split("((?<=[^\\\\])%)|(^%)");
				StringBuilder pattern = new StringBuilder();
				String percentConc = "";
				
				for (String percentPart : percentParts) {
					pattern.append(percentConc);
	
					String[] underscoreParts = percentPart.split("((?<=[^\\\\])_)|(^_)");
					String underScoreConc = "";
					for (String underScorePart : underscoreParts) {
						pattern.append(underScoreConc);
						if (!underScorePart.isEmpty()) {
							pattern.append(Pattern.quote(underScorePart));
						}
						underScoreConc = ".{1}?";
					}
					
					if ((percentPart.length()>1) && (percentPart.endsWith("_"))) {
						pattern.append(underScoreConc);
					}
					
					percentConc = ".*?";
				}
				
				if ((curTerm.length()>1) && (curTerm.endsWith("%"))) {
					pattern.append(percentConc);
				}
				
				if (!curTermResult.matches(pattern.toString())) {
					return false; 
				}
			}
		}
		return true;
	}
	
	public QueryResultRowArray getPositionsForTerm(
			String term, String documentId, int limit) {
		
		final Session session = sessionFactory.openSession();
		
		QueryResultRowArray result = new QueryResultRowArray();
		try {
			List<DBPosition> positions = 
					getPositionsForTerm(session, term, documentId, false, limit);
			
			for (DBPosition p : positions) {
				result.add(
					new QueryResultRow(
						p.getTerm().getDocumentId(), 
						new Range(p.getCharacterStart(), p.getCharacterEnd()), 
						term));
			}

		}
		finally {
			CloseSafe.close(new Closeable() {
				public void close() throws IOException {
					session.close();
				}
			});
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private List<DBPosition> getPositionsForTerm(
			Session session, String term, String documentId, boolean withWildcard, int limit) {
		
		String query =
				" from "
				+ DBPosition.class.getSimpleName() 
				+ " pos1 where pos1.term.term "
				+ (withWildcard?" like ": "=") 
				+ " :termArg";
		
		if (documentId != null) {
			query += " and pos1.term.documentId = '" + documentId + "'";
		}
		
		Query q = 
				session.createQuery(query);
		
		if (limit > 0) {
			q.setMaxResults(limit);
		}
		
		q.setString("termArg", term);
		
		return q.list();
	}
	
	public QueryResult searchWildcard(List<String> documentIdList,
			List<String> termList, int limit) throws IOException {
		
		final Session session = sessionFactory.openSession();
		QueryResultRowArray queryResult = null;
		
		try {
			
			if ((documentIdList==null) || documentIdList.isEmpty()) {
				queryResult = searchPhrase(session, null, null, termList, true, limit);
			}
			else {
				queryResult = new QueryResultRowArray();
	
				if (limit != 0) {
					for (String documentId : documentIdList) {
						limit -= ((QueryResultRowArray)queryResult).size();
						if  (limit >= 0) {
							queryResult.addAll(
								searchPhrase(
									session, documentId, null, 
									termList, true, limit));
						}
					}
				}
				else {
					for (String documentId : documentIdList) {
						queryResult.addAll(
							searchPhrase(
								session, documentId, null, 
								termList, true, limit));
					}
				}
			}
		}
		finally {
			CloseSafe.close(new Closeable() {
				public void close() throws IOException {
					session.close();
				}
			});
		}
		
		return queryResult;
	}

}
