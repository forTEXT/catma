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
import java.util.List;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.catma.document.Range;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

class PhraseSearcher {
	private static final int MAX_DIRECT_SEARCH_TERMS = 5;
	private DataSource dataSource;
//	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public PhraseSearcher() throws NamingException {
		super();
		Context  context = new InitialContext();
		this.dataSource = (DataSource) context.lookup("catmads");
	}

	public QueryResult search(List<String> documentIdList,
			String phrase, List<String> termList, int limit) throws IOException {
		
		if (documentIdList.isEmpty()) {
			throw new IllegalArgumentException("documentIdList cannot be empty");
		}
		
		QueryResultRowArray queryResult = new QueryResultRowArray();
		if (limit != 0) {
			for (String documentId : documentIdList) {
				limit -= ((QueryResultRowArray)queryResult).size();
				if  (limit >= 0) {
					queryResult.addAll(
						searchPhrase(
							documentId, phrase, termList, false, limit));
				}
			}
		}
		else {
			for (String documentId : documentIdList) {
				queryResult.addAll(
					searchPhrase(
						documentId, phrase, termList, false, limit));
			}
		}
		return queryResult;
	}

	private QueryResultRowArray searchPhrase(String documentId,
			String phrase, List<String> termList, boolean withWildcards, int limit) throws IOException {

		QueryResultRowArray result = new QueryResultRowArray();
		if (termList.size() == 0) {
			return result;
		}
		else {
			
			DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
			
			SpSearchPhrase spSearchPhrase = new SpSearchPhrase();
			spSearchPhrase.setLimitResult(limit);
			spSearchPhrase.setTermList(termList);
			spSearchPhrase.setDocumentId(documentId);
			spSearchPhrase.setWithWildcards(withWildcards);
			
			Result<Record> records = spSearchPhrase.execute(db);

			SpGetTerms spGetTerms = null;
			
			if (termList.size() > MAX_DIRECT_SEARCH_TERMS) {
				spGetTerms = new SpGetTerms();
				spGetTerms.setDocumentId(documentId);
			}

			
			for (Record r : records) {
				if (termList.size() > MAX_DIRECT_SEARCH_TERMS) {
						int tokenOffset = 
							(Integer)r.getValue(SpSearchPhrase.ResultColumn.tokenOffset.name());
						
						spGetTerms.setBasePos(tokenOffset+MAX_DIRECT_SEARCH_TERMS-1);
						
						spGetTerms.setTermCount(termList.size()-MAX_DIRECT_SEARCH_TERMS);
						
						Result<Record> termResultRecords = spGetTerms.execute(db);
						if (match(termResultRecords, termList, withWildcards)) {
							result.add(
								new QueryResultRow(
									documentId,
									new Range(
										tokenOffset,
										(Integer)termResultRecords.getValue(
												termResultRecords.size()-1, 
												SpGetTerms.ResultColumn.characterEnd.name())),
									phrase));
						}
				}
				else {
					result.add(
						new QueryResultRow(
							documentId, 
							new Range(
								(Integer)r.getValue(
										SpSearchPhrase.ResultColumn.characterStart.name()),
								(Integer)r.getValue(
										SpSearchPhrase.ResultColumn.characterEnd.name())),
								phrase));
				}
			}
			

		}
		
		return result;
	}
	
	

	private boolean match(
			Result<Record> termResultRecords, List<String> termList,
			boolean withWildcards) {
		
		int termListIdx = 0;
		for (Record r : termResultRecords) {
			String curTermResult = (String)r.getValue(SpGetTerms.ResultColumn.term.name());
			String curTerm = termList.get(MAX_DIRECT_SEARCH_TERMS+termListIdx);
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
			String term, String documentId) {
		
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);

		QueryResultRowArray result = new QueryResultRowArray();
		
		result.addAll(db
			.select()
			.from(POSITION)
			.join(TERM)
				.on(TERM.TERMID.eq(POSITION.TERMID))
				.and(TERM.DOCUMENTID.eq(documentId))
				.and(TERM.TERM_.eq(term))
			.fetch().map(new QueryResultRowMapper(documentId)));		
				
		return result;
	}
	
	public QueryResult searchWildcard(List<String> documentIdList,
			List<String> termList, int limit) throws IOException {
		
		QueryResultRowArray queryResult = null;
		
		if ((documentIdList==null) || documentIdList.isEmpty()) {
			queryResult = searchPhrase(null, null, termList, true, limit);
		}
		else {
			queryResult = new QueryResultRowArray();

			if (limit != 0) {
				for (String documentId : documentIdList) {
					limit -= ((QueryResultRowArray)queryResult).size();
					if  (limit >= 0) {
						queryResult.addAll(
							searchPhrase(
								documentId, null, 
								termList, true, limit));
					}
				}
			}
			else {
				for (String documentId : documentIdList) {
					queryResult.addAll(
						searchPhrase(
							documentId, null, 
							termList, true, limit));
				}
			}
		}
		
		return queryResult;
	}

}
