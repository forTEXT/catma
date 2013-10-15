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

import static de.catma.repository.db.jooqgen.catmaindex.Tables.TERM;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;

import de.catma.indexer.db.model.DBPosition;
import de.catma.indexer.db.model.DBTerm;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;

class FrequencySearcher {
	
	private SessionFactory sessionFactory;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private DataSource dataSource;
	
	public FrequencySearcher(SessionFactory sessionFactory) throws NamingException {
		super();
		this.sessionFactory = sessionFactory;
		Context  context = new InitialContext();
		this.dataSource = (DataSource) context.lookup("catmads");
	}

	
	public QueryResult search2(
			List<String> documentIdList, CompareOperator comp1, int freq1,
			CompareOperator comp2, int freq2) {
		
		DSLContext db = DSL.using(dataSource, SQLDialect.MYSQL);
		
		SelectConditionStep<Record> query = db
		.select()
		.from(TERM)
		.where(TERM.DOCUMENTID.in(documentIdList));
		
		if ((freq1 > 0) || (!comp1.equals(CompareOperator.GREATERTHAN))) {
			
			switch(comp1) {
			case EQUAL:
				query = query.and(TERM.FREQUENCY.eq(freq1));
				break;
			case GREATERTHAN:
				query = query.and(TERM.FREQUENCY.greaterThan(freq1));
				break;
			case LESSTHAN:
				query = query.and(TERM.FREQUENCY.lessThan(freq1));
				break;
			case GREATEROREQUALTHAN:
				query = query.and(TERM.FREQUENCY.greaterOrEqual(freq1));
				break;
			case LESSOREQUALTHAN:
				query = query.and(TERM.FREQUENCY.lessOrEqual(freq1));
				break;
			}
		}
		
		if (comp2 != null) {
			switch(comp2) {
			case EQUAL:
				query = query.and(TERM.FREQUENCY.eq(freq2));
				break;
			case GREATERTHAN:
				query = query.and(TERM.FREQUENCY.greaterThan(freq2));
				break;
			case LESSTHAN:
				query = query.and(TERM.FREQUENCY.lessThan(freq2));
				break;
			case GREATEROREQUALTHAN:
				query = query.and(TERM.FREQUENCY.greaterOrEqual(freq2));
				break;
			case LESSOREQUALTHAN:
				query = query.and(TERM.FREQUENCY.lessOrEqual(freq2));
				break;
			}
		}
		
		
		Result<Record> record = query.fetch();
		
		
		
		return null;
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
					new LazyDBPhraseQueryResult(t.getTerm()));
			}
			
			phraseResultMapping.get(t.getTerm()).addTerm(t);
		}
		
	}
	
	

}
