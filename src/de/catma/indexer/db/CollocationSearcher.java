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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import de.catma.document.Range;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TermInfo;
import de.catma.indexer.db.model.DBPosition;
import de.catma.indexer.db.model.DBTerm;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.util.CloseSafe;

public class CollocationSearcher {

	private SessionFactory sessionFactory;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public CollocationSearcher(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public SpanContext getSpanContextFor(
			String sourceDocumentId, Range range, int spanContextSize,
			SpanDirection direction) throws IOException {
		
		final Session session = sessionFactory.openSession();
		try {
			SpanContext spanContext = getSpanContextFor(
				session, sourceDocumentId, range, spanContextSize, direction);
			return spanContext;
		}
		finally {
			CloseSafe.close(new Closeable() {
				public void close() throws IOException {
					session.close();
				}
			});
		}
	}
	
	private SpanContext getSpanContextFor(
			Session session, String sourceDocumentId, Range keywordRange, 
			int spanContextSize, SpanDirection direction) throws IOException {
		
		SpanContext spanContext = new SpanContext(sourceDocumentId);
		
		List<DBPosition> tokensOfKeywordRange = 
			getTokensForRange(session, sourceDocumentId, keywordRange);
		
		for (DBPosition pos : tokensOfKeywordRange) {
			logger.info("Found token: " + pos);
		}
		
		if (tokensOfKeywordRange.size() > 0) {
			if (direction.equals(SpanDirection.Both)
					|| direction.equals(SpanDirection.Right)) {
				//forward
				
				int lastTokenOffset = 
						tokensOfKeywordRange.get(
								tokensOfKeywordRange.size()-1).getTokenOffset();
				
				List<DBPosition> forwardContextTokens = 
					getTokensForContext(
						session, true, sourceDocumentId, 
						lastTokenOffset, spanContextSize);
				for (DBPosition pos : forwardContextTokens) {
					spanContext.addForwardToken(
						new TermInfo(pos.getTerm().getTerm(), 
								pos.getCharacterStart(), pos.getCharacterEnd(),
								pos.getTokenOffset()));
				}
			}
			if (direction.equals(SpanDirection.Both)
					|| direction.equals(SpanDirection.Left)) {
				int firstTokenOffset = 
						tokensOfKeywordRange.get(0).getTokenOffset();

				List<DBPosition> backwardContextTokens = 
					getTokensForContext(
						session, false, sourceDocumentId, 
						firstTokenOffset, spanContextSize);
				
				for (DBPosition pos : backwardContextTokens) {
					spanContext.addBackwardToken(
						new TermInfo(pos.getTerm().getTerm(), 
								pos.getCharacterStart(), pos.getCharacterEnd(), 
								pos.getTokenOffset()));
				}
				
			}
		}
		else {
			logger.info("no tokens at " + keywordRange + "@" +sourceDocumentId);
		}
		
		return spanContext;
	}
	
	@SuppressWarnings("unchecked")
	private List<DBPosition> getTokensForContext(
		Session session, boolean forward, String sourceDocumentId, 
		int tokenOffset, int tokenCount) {
		
		StringBuilder queryBuilder = new StringBuilder();
		String conc = "";
		for (
				int i=1; i<=tokenCount; i++) {
			
			queryBuilder.append(conc);
			queryBuilder.append(" SELECT * FROM " ); 
			queryBuilder.append(DBPosition.TABLENAME);
			queryBuilder.append(" p ");
			queryBuilder.append(" JOIN ");
			queryBuilder.append(DBTerm.TABLENAME); 
			queryBuilder.append(" t ON t.termID = p.termID and t.documentID = '");
			queryBuilder.append(sourceDocumentId); 
			queryBuilder.append("' WHERE p.tokenOffset = ");
			queryBuilder.append(forward?(tokenOffset+i):(tokenOffset-i));
			conc = " UNION ";
		}
		String query = queryBuilder.toString();
		logger.info("Query: " + query);
		
		SQLQuery sqlQuery = session.createSQLQuery(query);
		sqlQuery.addEntity("position", DBPosition.class);
		
		return sqlQuery.list();
	}
	
	@SuppressWarnings("unchecked")
	private List<DBPosition> getTokensForRange(
			Session session, String sourceDocumentId, Range range) {
		String query = 
				" from " + DBPosition.class.getSimpleName() + " as pos " +
				" where pos.term.documentId = '" + sourceDocumentId + "'" +
				" and pos.characterStart < " + range.getEndPoint() + 
				" and pos.characterEnd > " + range.getStartPoint() +
				" order by pos.tokenOffset asc";

		logger.info("Query: " + query);
		Query hqlQuery = 
				session.createQuery(query);
		
		return hqlQuery.list();
	}

	@SuppressWarnings("unchecked")
	private List<DBPosition> getTokensForRange(
			Query query, String sourceDocumentId, Range range) {

		query.setString("curDocumentId", sourceDocumentId);
		query.setInteger("curCharStart", range.getEndPoint());
		query.setInteger("curCharEnd", range.getStartPoint());

		logger.info("Query: " + query.getQueryString());
		
		return query.list();
	}
	
	
	private Query createTokensForRangeQuery(Session session) {
		String query = 
		" from " + DBPosition.class.getSimpleName() + " as pos " +
		" where pos.term.documentId = :curDocumentId" +
		" and pos.characterStart < :curCharStart" +
		" and pos.characterEnd > :curCharEnd" +
		" order by pos.tokenOffset asc";

		
		return session.createQuery(query);
	}
	//TODO: this is way too slow, huge union query solutions are a bit faster but 
	// vulnerable if the size of the union query grows. 
	// Maybe parallelization of the baserow-for-loop could help, needs more investigation
	public QueryResult search(QueryResult baseResult,
				QueryResult collocationConditionResult, int spanContextSize,
				SpanDirection direction) throws IOException {
		
		QueryResultRowArray result = new QueryResultRowArray();
		final Session session = sessionFactory.openSession();
		try {
			Map<QueryResultRow, List<TermInfo>> rowToTermInfoListMapping = 
					new HashMap<QueryResultRow, List<TermInfo>>();
			Query getTokensQuery = createTokensForRangeQuery(session);
			for (QueryResultRow baseRow : baseResult) {
				SpanContext spanContext = getSpanContextFor(
						session, baseRow.getSourceDocumentId(), 
						baseRow.getRange(), spanContextSize, direction);
				if (spanContextMeetsCollocCondition(
						getTokensQuery, spanContext, 
						collocationConditionResult, rowToTermInfoListMapping)) {
					result.add(baseRow);
				}
			}

			return result;
		}
		finally {
			CloseSafe.close(new Closeable() {
				public void close() throws IOException {
					session.close();
				}
			});
		}
	}
	
	private boolean spanContextMeetsCollocCondition(
			Query getTokensQuery, SpanContext spanContext, 
			QueryResult collocationConditionResult, 
			Map<QueryResultRow, List<TermInfo>> rowToTermInfoListMapping) {
		
		
		for (QueryResultRow row : collocationConditionResult) {
			if (spanContext.getSourceDocumentId().equals(row.getSourceDocumentId())) {
				if (!rowToTermInfoListMapping.containsKey(row)) {
					List<DBPosition> collocationConditionTokens = 
							getTokensForRange(
								getTokensQuery, row.getSourceDocumentId(), 
								row.getRange());
					
					List<TermInfo> collocCondOrderedTermInfos = 
							new ArrayList<TermInfo>();
					
					for (DBPosition pos : collocationConditionTokens) {
						collocCondOrderedTermInfos.add(
							new TermInfo(
								pos.getTerm().getTerm(), 
								pos.getCharacterStart(), pos.getCharacterEnd(),
								pos.getTokenOffset()));
					}
					rowToTermInfoListMapping.put(
							row, collocCondOrderedTermInfos);
				}
				
				if (spanContext.contains(rowToTermInfoListMapping.get(row))) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public List<TermInfo> getTermInfosFor(String sourceDocumentId, Range range) {
		final Session session = sessionFactory.openSession();
		try {
			List<DBPosition> tokens = 
					getTokensForRange(
						session, sourceDocumentId, 
						range);
			
			List<TermInfo> termInfos = 
					new ArrayList<TermInfo>();
			
			for (DBPosition pos : tokens) {
				termInfos.add(
					new TermInfo(
						pos.getTerm().getTerm(), 
						pos.getCharacterStart(), pos.getCharacterEnd(),
						pos.getTokenOffset()));
			}
			return termInfos;
		}
		finally {
			CloseSafe.close(new Closeable() {
				public void close() throws IOException {
					session.close();
				}
			});
		}
	}
}
