package de.catma.indexer.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import de.catma.core.document.Range;
import de.catma.core.util.CloseSafe;
import de.catma.core.util.Pair;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

class PhraseSearcher {
	
	private SessionFactory sessionFactory;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public PhraseSearcher(SessionFactory sessionFactory) {
		super();
		this.sessionFactory = sessionFactory;
	}

	public QueryResult search(List<String> documentIdList,
			String phrase, List<String> termList) throws Exception {
		
		
		final Session session = sessionFactory.openSession();
		QueryResultRowArray queryResult = null;
		
		try {
			
			if ((documentIdList==null) || documentIdList.isEmpty()) {
				queryResult = searchPhrase(session, null, phrase, termList);
			}
			else {
				queryResult = new QueryResultRowArray();
	
				for (String documentId : documentIdList) {
					queryResult.addAll(
							searchPhrase(session, documentId, phrase, termList));
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
			String phrase, List<String> termList) throws Exception {
		if (termList.size() == 0) {
			return new QueryResultRowArray();
		}
		else if (termList.size() == 1) {
			
			List<DBPosition> positions = 
					getPositionsForTerm(session, termList.get(0), documentId);
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
			
			List<DBPosition> matchList =  
					getPositionsForTerm(
							session, 
							termList.get(0), 
							termList.get(1), 1,
							documentId,
							true);
			
			for (int i=2; i<termList.size()-1; i++) {
				List<DBPosition> result = getPositionsForTerm(
						session, termList.get(0), 
						termList.get(i+1), i+1, documentId, true);
				matchList.retainAll(result);
			}
			
			HashMap<Integer, DBPosition> startPositions = 
					new HashMap<Integer, DBPosition>();
			
			for (DBPosition p : matchList) {
				startPositions.put(p.getTokenOffset(), p);
			}
			
			List<DBPosition> endPositions = getPositionsForTerm(
					session, 
					termList.get(0), termList.get(termList.size()-1), 
					termList.size()-1, 
					documentId,
					false);
			
			List<Pair<DBPosition, DBPosition>> result = 
					new ArrayList<Pair<DBPosition,DBPosition>>();
			
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
			
			QueryResultRowArray queryResult = new QueryResultRowArray();
			for (Pair<DBPosition,DBPosition> match : result) {
				System.out.println(match);
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
					getPositionsForTerm(session, term, documentId);
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
			Session session, String term, String documentId) {
		
		String query =
				" from "
				+ DBEntityName.DBPosition 
				+ " pos1 where pos1.term.term = '" //TODO: lower() for case insens.
				+ term
				+ "'";
		
		if (documentId != null) {
			query += " and pos1.term.documentId = '" + documentId + "'";
		}
		
		logger.info("query: " + query);
		Query q = 
				session.createQuery(query);
		
		return q.list();
	}
	
	@SuppressWarnings("unchecked")
	private List<DBPosition> getPositionsForTerm(
			Session session, String term1, String term2, int tokenOffset, 
			String documentId, boolean resultsForFirstTerm) {
		
		String query = "select "
				+ (resultsForFirstTerm? " pos1 " : " pos2 ")
				+ " from "
				+ DBEntityName.DBPosition 
				+ " pos1, "
				+ DBEntityName.DBPosition 
				+ " pos2 where pos1.term.term = '" 
				+ term1
				+ "'"
				+ " and pos2.term.term = '"
				+ term2
				+ "' and pos2.tokenOffset = pos1.tokenOffset + " + tokenOffset 
				+ " and pos1.term.documentId = pos2.term.documentId";
		
		if (documentId != null) {
			query += " and pos1.term.documentId = '" + documentId + "'";
		}
		
		logger.info("query: " + query);
		Query q = 
				session.createQuery(query);
		
		return q.list();
	}

}
