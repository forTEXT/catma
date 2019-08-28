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

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.catma.document.Range;
import de.catma.queryengine.QueryId;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.repository.db.CatmaDataSourceName;

/**
 * Searches for phrases with optional wildcards % (0 or more characters) and _ (exactly one character).
 * 
 * @author marco.petris@web.de
 *
 */
class PhraseSearcher {
	private static final int MAX_DIRECT_SEARCH_TERMS = 5;
	private DataSource dataSource;
//	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public PhraseSearcher() {
		super();
		this.dataSource = CatmaDataSourceName.CATMADS.getDataSource();
	}

	/**
	 * Plain phrase search without wildcards.
	 * 
	 * @param documentIdList list of source document IDs (localUri)
	 * @param phrase the whole phrase to search for
	 * @param termList the tokenized phrase
	 * @param limit a limit for the result set
	 * @return a query result
	 * @throws IOException failure
	 */
	public QueryResult search(QueryId queryId, List<String> documentIdList,
			String phrase, List<String> termList, int limit) throws IOException {
		
		if (documentIdList.isEmpty()) {
			throw new IllegalArgumentException("documentIdList cannot be empty");
		}
		
		QueryResultRowArray queryResult = new QueryResultRowArray();
		// do we have a limit for the result set?
		if (limit != 0) {
			// respect the limit even when having more than one call to D
			for (String documentId : documentIdList) {
				limit -= ((QueryResultRowArray)queryResult).size();
				if  (limit >= 0) {
					queryResult.addAll(
						searchPhrase(
							queryId,
							documentId, phrase, termList, false, limit));
				}
			}
		}
		else { // no limits
			for (String documentId : documentIdList) {
				queryResult.addAll(
					searchPhrase(
						queryId,
						documentId, phrase, termList, false, limit));
			}
		}
		return queryResult;
	}

	/**
	 * Phrase search with optional wildcards.
	 * 
	 * @param documentIdList list of source document IDs (localUri)
	 * @param phrase the whole phrase to search for
	 * @param termList the tokenized phrase
	 * @param withWildcards <code>true</code> search with wildcards
	 * @param limit a limit for the result set
	 * @return  a query result 
	 * @throws IOException failure
	 */
	private QueryResultRowArray searchPhrase(QueryId queryId, String documentId,
			String phrase, List<String> termList, boolean withWildcards, int limit) throws IOException {

		QueryResultRowArray result = new QueryResultRowArray();
		if (termList.size() == 0) { // we do not support search for empty character sequence
			return result;
		}
		else {
			
			DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
			
			SpSearchPhrase spSearchPhrase = new SpSearchPhrase();
			spSearchPhrase.setLimitResult(limit);
			spSearchPhrase.setTermList(termList);
			spSearchPhrase.setDocumentId(documentId);
			spSearchPhrase.setWithWildcards(withWildcards);
			
			// search for the phrase or at least for the first part of
			// the phrase when the number of tokens exceeds MAX_DIRECT_SEARCH_TERMS
			Result<Record> records = spSearchPhrase.execute(db);

			SpGetTerms spGetTerms = null;
			
			// prepare for refining the search result with the remaining tokens
			if (termList.size() > MAX_DIRECT_SEARCH_TERMS) {
				spGetTerms = new SpGetTerms();
				spGetTerms.setDocumentId(documentId);
			}

			
			for (Record r : records) {
				// refine result
				if (termList.size() > MAX_DIRECT_SEARCH_TERMS) {
					
					int tokenOffset = 
						(Integer)r.getValue(SpSearchPhrase.ResultColumn.tokenOffset.name());
					
					// ok, the base token offset has to be counted from the tokenoffset of 
					// the last token in the current record
					spGetTerms.setBasePos(tokenOffset+MAX_DIRECT_SEARCH_TERMS-1);
					
					// number of remaining tokens in the termList 
					spGetTerms.setTermCount(termList.size()-MAX_DIRECT_SEARCH_TERMS);
					
					Result<Record> termResultRecords = spGetTerms.execute(db);
					// do we still have a valid row?
					if (match(termResultRecords, termList, withWildcards)) {
						// yes, ok then add to query result
						result.add(
							new QueryResultRow(
								queryId,
								documentId,
								new Range(
									(Integer)r.getValue(
											SpSearchPhrase.ResultColumn.characterStart.name()),
									(Integer)termResultRecords.getValue(
											termResultRecords.size()-1, 
											SpGetTerms.ResultColumn.characterEnd.name())),
								phrase));
					}
				}
				// no refinement necessary, just create query result
				else {
					result.add(
						new QueryResultRow(
							queryId,
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
	
	

	/**
	 * Matches a list of tokens from the DB (termResultRecords) with the tokens 
	 * of the phrase (termList).
	 * @param termResultRecords tokens from the DB
	 * @param termList tokens of the phrase to search
	 * @param withWildcards <code>true</code>->consider wildcards
	 * @return true if all tokens from the DB have a match
	 */
	private boolean match(
			Result<Record> termResultRecords, List<String> termList,
			boolean withWildcards) {
		
		int termListIdx = 0;
		// validate each term from the DB
		for (Record r : termResultRecords) {
			String curTermResult = (String)r.getValue(SpGetTerms.ResultColumn.term.name());
			String curTerm = termList.get(MAX_DIRECT_SEARCH_TERMS+termListIdx);
			
			if (!withWildcards) { // no wildcards so we use plain old equals
				if (!curTermResult.equals(curTerm)) {
					return false;
				}
			}
			else { // with wildcards
				
				// first we break up in chunks seperated by %
				String[] percentParts = curTerm.split("((?<=[^\\\\])%)|(^%)");
				StringBuilder pattern = new StringBuilder();
				String percentConc = "";
				
				// connect the %-parts and the _-parts with the corresponding regex
				for (String percentPart : percentParts) {
					pattern.append(percentConc);
					// then we break up in chunks seperated by _
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
				
				// validate
				if (!curTermResult.matches(pattern.toString())) {
					return false; 
				}
			}
			termListIdx++;
		}
		return true;
	}
	
	/**
	 * @param term the term we want positions for
	 * @param documentId the document we want positions for
	 * @return a list of positions for the given term in the given document
	 */
	public QueryResultRowArray getPositionsForTerm(
			QueryId queryId, 
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
			.fetch().map(new QueryResultRowMapper(queryId, documentId)));		
				
		return result;
	}
	
	/**
	 * Searches a phrase that may contain wildcards
	 * @param documentIdList list of source document IDs (localUri)
	 * @param termList the tokenized phrase
	 * @param limit  limit of the query result
	 * @return a query result
	 * @throws IOException failure
	 */
	public QueryResult searchWildcard(QueryId queryId, List<String> documentIdList,
			List<String> termList, int limit) throws IOException {
		
		QueryResultRowArray queryResult = null;
		
		if ((documentIdList==null) || documentIdList.isEmpty()) {
			queryResult = searchPhrase(queryId, null, null, termList, true, limit);
		}
		else {
			queryResult = new QueryResultRowArray();
			// respect limit even when doing multiple queries to the DB
			if (limit != 0) {
				for (String documentId : documentIdList) {
					limit -= ((QueryResultRowArray)queryResult).size();
					if  (limit >= 0) {
						queryResult.addAll(
							searchPhrase(
								queryId,
								documentId, null, 
								termList, true, limit));
					}
				}
			}
			else {
				for (String documentId : documentIdList) {
					queryResult.addAll(
						searchPhrase(
							queryId,
							documentId, null, 
							termList, true, limit));
				}
			}
		}
		
		return queryResult;
	}

}
