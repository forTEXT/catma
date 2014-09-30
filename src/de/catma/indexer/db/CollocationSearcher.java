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

import static de.catma.repository.db.jooqgen.catmaindex.Tables.POSITION;
import static de.catma.repository.db.jooqgen.catmaindex.Tables.TERM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Select;
import org.jooq.impl.DSL;

import de.catma.document.Range;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TermInfo;
import de.catma.indexer.db.model.Position;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.repository.db.CatmaDataSourceName;

class CollocationSearcher {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private DataSource dataSource;

	public CollocationSearcher() throws NamingException {
		Context  context = new InitialContext();
		this.dataSource = (DataSource) context.lookup(CatmaDataSourceName.CATMADS.name());
	}
	
	public SpanContext getSpanContextFor(
			String sourceDocumentId, Range range, int spanContextSize,
			SpanDirection direction) throws IOException {
		
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		
		SpanContext spanContext = getSpanContextFor(
			db, sourceDocumentId, range, spanContextSize, direction);
		return spanContext;
	}

	private SpanContext getSpanContextFor(
			DSLContext db, String sourceDocumentId, Range keywordRange, 
			int spanContextSize, SpanDirection direction) throws IOException {
	
		SpanContext spanContext = new SpanContext(sourceDocumentId);

		
		List<Position> tokensOfKeywordRange = 
				getTokensForRange(db, sourceDocumentId, keywordRange);
		
		if (tokensOfKeywordRange.size() > 0) {
			if (direction.equals(SpanDirection.BOTH)
					|| direction.equals(SpanDirection.FORWARD)) {
				//forward
				
				int lastTokenOffset = 
						tokensOfKeywordRange.get(
								tokensOfKeywordRange.size()-1).getTokenOffset();
				
				List<Position> forwardContextTokens = 
					getTokensForContext(
						db, true, sourceDocumentId, 
						lastTokenOffset, spanContextSize);
				for (Position pos : forwardContextTokens) {
					spanContext.addForwardToken(
						new TermInfo(pos.getTerm().getTerm(), 
								pos.getCharacterStart(), pos.getCharacterEnd(),
								pos.getTokenOffset()));
				}
			}
			
			if (direction.equals(SpanDirection.BOTH)
					|| direction.equals(SpanDirection.BACKWARD)) {
				int firstTokenOffset = 
						tokensOfKeywordRange.get(0).getTokenOffset();

				List<Position> backwardContextTokens = 
					getTokensForContext(
						db, false, sourceDocumentId, 
						firstTokenOffset, spanContextSize);
				
				for (Position pos : backwardContextTokens) {
					spanContext.addBackwardToken(
						new TermInfo(pos.getTerm().getTerm(), 
								pos.getCharacterStart(), pos.getCharacterEnd(), 
								pos.getTokenOffset()));
				}
				
			}
		}
		return spanContext;
	}
	
	
	private List<Position> getTokensForRange(
			DSLContext db, String sourceDocumentId, Range keywordRange) {
		
		return db
		.select()
		.from(POSITION)
		.join(TERM)
			.on(TERM.TERMID.eq(POSITION.TERMID))
			.and(TERM.DOCUMENTID.eq(sourceDocumentId))
		.where(POSITION.CHARACTERSTART.lessThan(keywordRange.getEndPoint()))
		.and(POSITION.CHARACTEREND.greaterThan(keywordRange.getStartPoint()))
		.orderBy(POSITION.TOKENOFFSET.asc())
		.fetch()
		.map(new PositionMapper());
	}

	private List<Position> getTokensForContext(
			DSLContext db, boolean forward, String sourceDocumentId, 
			int tokenOffset, int tokenCount) {

		Select<Record> query = db
		.select()
		.from(POSITION)
		.join(TERM)
			.on(TERM.TERMID.eq(POSITION.TERMID))
			.and(TERM.DOCUMENTID.eq(sourceDocumentId))
		.where(POSITION.TOKENOFFSET.eq(forward?(tokenOffset+1):(tokenOffset-1)));
		
		for (int i=2; i<=tokenCount; i++) {
			query = query.union(
				db
				.select()
				.from(POSITION)
				.join(TERM)
					.on(TERM.TERMID.eq(POSITION.TERMID))
					.and(TERM.DOCUMENTID.eq(sourceDocumentId))
				.where(POSITION.TOKENOFFSET
						.eq(forward?(tokenOffset+i):(tokenOffset-i))));
		}
		
		return query.fetch().map(new PositionMapper());
	}

	//TODO: this is way too slow, huge union query solutions are a bit faster but 
	// vulnerable if the size of the union query grows. 
	// Maybe parallelization of the baserow-for-loop could help, needs more investigation
	public QueryResult search(QueryResult baseResult,
				QueryResult collocationConditionResult, int spanContextSize,
				SpanDirection direction) throws IOException {
		
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);

		QueryResultRowArray result = new QueryResultRowArray();
		
		Map<QueryResultRow, List<TermInfo>> rowToTermInfoListMapping = 
				new HashMap<QueryResultRow, List<TermInfo>>();
		
		for (QueryResultRow baseRow : baseResult) {
			SpanContext spanContext = getSpanContextFor(
					db, baseRow.getSourceDocumentId(), 
					baseRow.getRange(), spanContextSize, direction);
			if (spanContextMeetsCollocCondition(
					db, spanContext, 
					collocationConditionResult, rowToTermInfoListMapping)) {
				result.add(baseRow);
			}
		}

		return result;
	}
	
	private boolean spanContextMeetsCollocCondition(
			DSLContext db, SpanContext spanContext, 
			QueryResult collocationConditionResult, 
			Map<QueryResultRow, List<TermInfo>> rowToTermInfoListMapping) {
		
		
		for (QueryResultRow row : collocationConditionResult) {
			if (spanContext.getSourceDocumentId().equals(row.getSourceDocumentId())) {
				if (!rowToTermInfoListMapping.containsKey(row)) {
					List<Position> collocationConditionTokens = 
							getTokensForRange(
								db, row.getSourceDocumentId(), 
								row.getRange());
					
					List<TermInfo> collocCondOrderedTermInfos = 
							new ArrayList<TermInfo>();
					
					for (Position pos : collocationConditionTokens) {
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
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		List<Position> tokens = 
				getTokensForRange(
					db, sourceDocumentId, 
					range);
		
		List<TermInfo> termInfos = 
				new ArrayList<TermInfo>();
		
		for (Position pos : tokens) {
			termInfos.add(
				new TermInfo(
					pos.getTerm().getTerm(), 
					pos.getCharacterStart(), pos.getCharacterEnd(),
					pos.getTokenOffset()));
		}
		return termInfos;
	}
}
