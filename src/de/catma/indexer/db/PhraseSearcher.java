package de.catma.indexer.db;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.google.gwt.dev.util.collect.HashMap;

import de.catma.core.document.Range;
import de.catma.core.util.Pair;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class PhraseSearcher {
	
	private SessionFactory sessionFactory;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public PhraseSearcher(SessionFactory sessionFactory) {
		super();
		this.sessionFactory = sessionFactory;
	}

	public QueryResult searchPhrase(List<String> documentIdList,
			String phrase, List<String> termList) throws Exception {
		
		
		Session session = sessionFactory.openSession();
		
		QueryResultRowArray queryResult = null;
		
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
		
		session.close();
		
		return queryResult;
	}
	
	private QueryResultRowArray searchPhrase(Session session, String documentId,
			String phrase, List<String> termList) throws Exception {
		if (termList.size() == 0) {
			return new QueryResultRowArray();
		}
		else if (termList.size() == 1) {
			
			List<Position> positions = 
					getPositionsForTerm(session, termList.get(0), documentId);
			QueryResultRowArray result = new QueryResultRowArray();
			for (Position p : positions) {
				result.add(
					new QueryResultRow(
						p.getTerm().getDocumentId(), 
						new Range(p.getCharacterStart(), p.getCharacterEnd()), 
						phrase));
			}
			return result;
		}
		else {
			
			List<Position> matchList =  
					getPositionsForTerm(
							session, 
							termList.get(0), 
							termList.get(1), 1,
							documentId,
							true);
			
			for (int i=2; i<termList.size()-1; i++) {
				List<Position> result = getPositionsForTerm(
						session, termList.get(0), 
						termList.get(i+1), i+1, documentId, true);
				matchList.retainAll(result);
			}
			
			HashMap<Integer, Position> startPositions = 
					new HashMap<Integer, Position>();
			
			for (Position p : matchList) {
				startPositions.put(p.getTokenOffset(), p);
			}
			
			List<Position> endPositions = getPositionsForTerm(
					session, 
					termList.get(0), termList.get(termList.size()-1), 
					termList.size()-1, 
					documentId,
					false);
			
			List<Pair<Position, Position>> result = 
					new ArrayList<Pair<Position,Position>>();
			
			for (Position p : endPositions) {
				if (startPositions.containsKey(
						p.getTokenOffset()-(termList.size()-1))) {
					
					result.add(
						new Pair<Position,Position>(
							startPositions.get(
									p.getTokenOffset()-(termList.size()-1)),
							p));
				}
			}
			
			QueryResultRowArray queryResult = new QueryResultRowArray();
			for (Pair<Position,Position> match : result) {
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
	
	@SuppressWarnings("unchecked")
	private List<Position> getPositionsForTerm(
			Session session, String term, String documentId) {
		
		String query =
				" from Position pos1 where pos1.term.term = '" 
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
	private List<Position> getPositionsForTerm(
			Session session, String term1, String term2, int tokenOffset, 
			String documentId, boolean resultsForFirstTerm) {
		
		String query = "select "
				+ (resultsForFirstTerm? " pos1 " : " pos2 ")
				+ " from Position pos1, Position pos2 where pos1.term.term = '" 
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
