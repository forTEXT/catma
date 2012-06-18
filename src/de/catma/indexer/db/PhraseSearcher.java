package de.catma.indexer.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import de.catma.document.Range;
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
			String phrase, List<String> termList) throws IOException {
		
		
		final Session session = sessionFactory.openSession();
		QueryResultRowArray queryResult = null;
		
		try {
			
			if ((documentIdList==null) || documentIdList.isEmpty()) {
				queryResult = searchPhrase(session, null, phrase, termList, false);
			}
			else {
				queryResult = new QueryResultRowArray();
	
				for (String documentId : documentIdList) {
					queryResult.addAll(
						searchPhrase(
								session, documentId, phrase, termList, false));
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
	
	private QueryResultRowArray searchPhrase(Session session, String documentId,
			String phrase, List<String> termList, boolean withWildcards) throws IOException {
		if (termList.size() == 0) {
			return new QueryResultRowArray();
		}
		else if (termList.size() == 1) {
			
			List<DBPosition> positions = 
					getPositionsForTerm(session, termList.get(0), documentId, withWildcards);
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
			// get the matching positions for the first term that has the second
			// term at the adjacent following position
			List<DBPosition> matchList =  
					getPositionsForTerm(
							session, 
							termList.get(0), // first term
							termList.get(1), // second term
							1, // adjacent following position 
							documentId,
							true, // results for first term
							withWildcards); 
			
			// get matching positions for the first term that has the i-th
			// term at the i-th position ...
			for (int i=2; i<termList.size()-2; i++) {
				List<DBPosition> result = getPositionsForTerm(
						session, 
						termList.get(0), // first term
						termList.get(i), // i-th term (2..size-1)
						i, //i-th position (2..size-1)
						documentId,
						true, // results for first term
						withWildcards); 
				// ... and keep just those matchList entries that match the
				// current condition (i-th term at the i-th position)
				matchList.retainAll(result);
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
					termList.get(0), // first term
					termList.get(termList.size()-1), //last term
					termList.size()-1, // last position
					documentId,
					false,  // results for the last term
					withWildcards);
			
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
			
			// create a queryResult from machting phrases
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
	
	public QueryResultRowArray getPositionsForTerm(
			String term, String documentId) {
		
		final Session session = sessionFactory.openSession();
		
		QueryResultRowArray result = new QueryResultRowArray();
		try {
			List<DBPosition> positions = 
					getPositionsForTerm(session, term, documentId, false);
			
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
			Session session, String term, String documentId, boolean withWildcard) {
		
		String query =
				" from "
				+ DBEntityName.DBPosition 
				+ " pos1 where pos1.term.term "
				+ (withWildcard?" like ": "=") 
				+ " :termArg"; //TODO: lower() for case insens.
		
		if (documentId != null) {
			query += " and pos1.term.documentId = '" + documentId + "'";
		}
		
		Query q = 
				session.createQuery(query);
		
		q.setString("termArg", term);
		
		return q.list();
	}
	
	@SuppressWarnings("unchecked")
	private List<DBPosition> getPositionsForTerm(
			Session session, String term1, String term2, int tokenOffset, 
			String documentId, boolean resultsForFirstTerm, 
			boolean withWildcards) {
		
		String query = "select "
				+ (resultsForFirstTerm? " pos1 " : " pos2 ")
				+ " from "
				+ DBEntityName.DBPosition 
				+ " pos1, "
				+ DBEntityName.DBPosition 
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
			List<String> termList) throws IOException {
		
		final Session session = sessionFactory.openSession();
		QueryResultRowArray queryResult = null;
		
		try {
			
			if ((documentIdList==null) || documentIdList.isEmpty()) {
				queryResult = searchPhrase(session, null, null, termList, true);
			}
			else {
				queryResult = new QueryResultRowArray();
	
				for (String documentId : documentIdList) {
					queryResult.addAll(
						searchPhrase(session, documentId, null, termList, true));
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
