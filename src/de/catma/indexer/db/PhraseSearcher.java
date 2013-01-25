package de.catma.indexer.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import de.catma.util.Pair;

class PhraseSearcher {
	
	private SessionFactory sessionFactory;
//	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public PhraseSearcher(SessionFactory sessionFactory) {
		super();
		this.sessionFactory = sessionFactory;
	}

	public QueryResult search(List<String> documentIdList,
			String phrase, List<String> termList, int limit) throws IOException {
		
		
		final Session session = sessionFactory.openSession();
		QueryResultRowArray queryResult = null;
		
		try {
			
			if ((documentIdList==null) || documentIdList.isEmpty()) {
				queryResult = searchPhrase(session, null, phrase, termList, false, limit);
			}
			else {
				queryResult = new QueryResultRowArray();
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
	
	private QueryResultRowArray searchPhrase2(Session session, String documentId,
			String phrase, List<String> termList, boolean withWildcards, int limit) throws IOException {
		
		if (termList.size() == 0) {
			return new QueryResultRowArray();
		}
		else if (termList.size() == 1) {
			
			List<DBPosition> positions = 
					getPositionsForTerm(
						session, termList.get(0), documentId, withWildcards, limit);
			QueryResultRowArray result = new QueryResultRowArray();
			for (DBPosition p : positions) {
				result.add(
					new QueryResultRow(
						p.getTerm().getDocumentId(), 
						new Range(p.getCharacterStart(), p.getCharacterEnd()), 
						phrase));
			}
			return result;
		}
		else {
			
			//NEXT: wildcards, limit, termsize>5

			SQLQuery spSearchPhrase = session.createSQLQuery(
						"call CatmaIndex.searchPhrase(" +
						":term1, :term2, :term3, :term4, :term5, :docID)");
		
			int termCount=0;
			for (termCount=0; termCount<Math.min(5, termList.size()); termCount++) {
				spSearchPhrase.setParameter("term"+(termCount+1), termList.get(termCount));
			}
			
			for (int pIdx=termCount; pIdx<5; pIdx++) {
				spSearchPhrase.setParameter("term"+(pIdx+1), null);
			}
			
			spSearchPhrase.setParameter("docID", documentId);
			
			@SuppressWarnings("unchecked")
			List<Object[]> result = spSearchPhrase.list();
			
			QueryResultRowArray queryResult = new QueryResultRowArray();
			for (Object[] row : result) {
				queryResult.add(
					new QueryResultRow(
						documentId, 
						new Range(
							(Integer)row[1],
							(Integer)row[2]),
						phrase));
			}
			
			return queryResult;
		
		}		
	}
	private QueryResultRowArray searchPhrase(Session session, String documentId,
			String phrase, List<String> termList, boolean withWildcards, int limit) throws IOException {
		if (termList.size() == 0) {
			return new QueryResultRowArray();
		}
		else if (termList.size() == 1) {
			
			List<DBPosition> positions = 
					getPositionsForTerm(
						session, termList.get(0), documentId, withWildcards, limit);
			QueryResultRowArray result = new QueryResultRowArray();
			for (DBPosition p : positions) {
				result.add(
					new QueryResultRow(
						p.getTerm().getDocumentId(), 
						new Range(p.getCharacterStart(), p.getCharacterEnd()), 
						phrase));
			}
			return result;
		}
		else {
			
			Pair<String, Integer> startTermWithPosition = 
					new Pair<String, Integer>(termList.get(0),0);
			
			if (withWildcards) {
				startTermWithPosition = getNextNonMatchAllTerm(0, termList);
			}
			
			if (startTermWithPosition == null) {
				//TODO: get all terms of the documents
			}
			
			Pair<String, Integer> secondTermWithPosition = 
					new Pair<String, Integer>(termList.get(1),1);
			if (withWildcards) {
					secondTermWithPosition = 
						getNextNonMatchAllTerm(1, termList);
			}
			
			// get the matching positions for the first term that has the second
			// term at the adjacent following position
			List<DBPosition> matchList =  
					getPositionsForTerm(
							session, 
							termList.get(0), // first term for start positions!
							secondTermWithPosition.getFirst(), // second term
							secondTermWithPosition.getSecond(), // adjacent position 
							documentId,
							true, // results for first term
							withWildcards, 
							limit); 
			
			// get matching positions for the first term that has the i-th
			// term at the i-th position ...
			for (int i=secondTermWithPosition.getSecond(); i<termList.size()-2; i++) {
				if (!withWildcards || !termList.get(i).equals("%")) {
					List<DBPosition> result = getPositionsForTerm(
							session, 
							startTermWithPosition.getFirst(), // first non wildcard term
							termList.get(i), // i-th term (2..size-1)
							i, //i-th position (2..size-1)
							documentId,
							true, // results for first term
							withWildcards, 
							limit); 
					// ... and keep just those matchList entries that match the
					// current condition (i-th term at the i-th position)
					matchList.retainAll(result);
				}
			}
			
			// mapping start position -> position object of the matching first term
			HashMap<Integer, DBPosition> startPositions = 
					new HashMap<Integer, DBPosition>();
			
			for (DBPosition p : matchList) {
				startPositions.put(p.getTokenOffset(), p);
			}
			
			// 
			List<DBPosition> endPositions = getPositionsForTerm(
					session, 
					startTermWithPosition.getFirst(), // first non wildcard term
					termList.get(termList.size()-1), //last term
					termList.size()-1, // last position
					documentId,
					false,  // results for the last term
					withWildcards,
					limit);
			
			// list of matching phrases with their start and end terms as position objects
			List<Pair<DBPosition, DBPosition>> result = 
					new ArrayList<Pair<DBPosition,DBPosition>>();
			
			// fill the list by looping throught the end positions
			for (DBPosition p : endPositions) {
				if (startPositions.containsKey(
						p.getTokenOffset()-(termList.size()-1))) {
					
					result.add(
						new Pair<DBPosition,DBPosition>(
							startPositions.get(
									p.getTokenOffset()-(termList.size()-1)),
							p));
				}
			}
			
			// create a queryResult from matching phrases
			QueryResultRowArray queryResult = new QueryResultRowArray();
			for (Pair<DBPosition,DBPosition> match : result) {
				
				queryResult.add(
					new QueryResultRow(
						match.getFirst().getTerm().getDocumentId(), 
						new Range(
							match.getFirst().getCharacterStart(),
							match.getSecond().getCharacterEnd()), 
						phrase));
			}
			
			return queryResult;
		}		
	}
	
	private Pair<String, Integer> getNextNonMatchAllTerm(int startIdx,
			List<String> termList) {
		
		for (int i=startIdx; i<termList.size(); i++) {
			String term = termList.get(i);
			if (!term.equals("%")) {
				return new Pair<String,Integer>(term,i);
			}
		}
		
		return null;
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
				+ " :termArg"; //TODO: lower() for case insens.
		
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
	
	@SuppressWarnings("unchecked")
	private List<DBPosition> getPositionsForTerm(
			Session session, String term1, String term2, int tokenOffset, 
			String documentId, boolean resultsForFirstTerm, 
			boolean withWildcards, int limit) {
		
		String query = "select "
				+ (resultsForFirstTerm? " pos1 " : " pos2 ")
				+ " from "
				+ DBPosition.class.getSimpleName() 
				+ " pos1, "
				+ DBPosition.class.getSimpleName() 
				+ " pos2 where pos1.term.term " + (withWildcards?"like ": "= ") + ":curTerm1" 
				+ " and pos2.term.term " + (withWildcards?"like ": "= ") + ":curTerm2"
				+ " and pos2.tokenOffset = pos1.tokenOffset + " + tokenOffset 
				+ " and pos1.term.documentId = pos2.term.documentId";
		
		if (documentId != null) {
			query += " and pos1.term.documentId = '" + documentId + "'";
		}
		
		Query q = 
				session.createQuery(query);
		q.setString("curTerm1", term1);
		q.setString("curTerm2", term2);
		
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
