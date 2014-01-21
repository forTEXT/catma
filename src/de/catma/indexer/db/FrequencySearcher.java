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

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.SelectConditionStep;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;
import de.catma.repository.db.CatmaDataSourceName;

class FrequencySearcher {
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private DataSource dataSource;
	
	public FrequencySearcher() throws NamingException {
		super();
		Context  context = new InitialContext();
		this.dataSource = (DataSource) context.lookup(CatmaDataSourceName.CATMADS.name());
	}

	public QueryResult search(
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
		
		
		Result<Record> records = query.fetch();
		
		HashMap<String, LazyDBPhraseQueryResult> phraseResultMapping = 
				new HashMap<String, LazyDBPhraseQueryResult>();
		TermMapper termMapper = new TermMapper();
		
		for (Record r : records) {
			LazyDBPhraseQueryResult qr = null;
			String term = r.getValue(TERM.TERM_);
			if (!phraseResultMapping.containsKey(term)) {
				qr = new LazyDBPhraseQueryResult(term);
				phraseResultMapping.put(term, qr);
			}
			else {
				qr = phraseResultMapping.get(term);
			}
			
			qr.addTerm(termMapper.map(r));
		}
		
		GroupedQueryResultSet groupedQueryResultSet = new GroupedQueryResultSet();
		groupedQueryResultSet.addAll(phraseResultMapping.values());
		
		return groupedQueryResultSet;
	}
}
