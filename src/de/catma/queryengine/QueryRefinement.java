/*
 *    CATMA Computer Aided Text Markup and Analysis
 * 
 *    Copyright (C) 2009  University Of Hamburg
 * 
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 * 
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.catma.queryengine;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

/**
 * A refinement that is specified via a {@link org.catma.queryengine.Query}.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class QueryRefinement implements Refinement {

    private Query query;

    /**
     * Constructor.
     *
     * @param query the definition of this refinement
     */
    public QueryRefinement(Query query) {
        this.query = query;
    }
    
    private QueryResult refineWithNonFreqQuery(QueryResult result) throws Exception {

    	QueryResult refinementResult = query.getResult();
    	
    	Comparator<QueryResultRow> comparator = query.getComparator();
    	
    	if (comparator == null) {
    		QueryResultRowArray refinedResult = result.asQueryResultRowArray();
    		refinedResult.retainAll(refinementResult.asQueryResultRowArray());
    		
    		return refinedResult;
    	}
    	else {
    		Iterator<QueryResultRow> resultIterator = result.iterator();
    		while (resultIterator.hasNext()) {
    			QueryResultRow curRow = resultIterator.next();
    			if (!curRow.existsIn(refinementResult, comparator)) {
    				resultIterator.remove();
    			}
    		}
    	}

    	return result;
    }
    
    private QueryResult refineWithFreqQuery(QueryResult result) throws Exception {
    	FreqQuery freqQuery = (FreqQuery)query;
    	Set<GroupedQueryResult> groupedSet = result.asGroupedSet();
    	Iterator<GroupedQueryResult> resultIterator = groupedSet.iterator();
		while (resultIterator.hasNext()) {
			GroupedQueryResult curGroupedResult = resultIterator.next();
			
			if (!freqQuery.matches(curGroupedResult.getTotalFrequency())) {
				resultIterator.remove();
			}
		}
		QueryResultRowArray refinedResult = new QueryResultRowArray();
		for (GroupedQueryResult groupedResult : groupedSet) {
			for (QueryResultRow row : groupedResult) {
				refinedResult.add(row);
			}
		}
		return refinedResult;
    }
    
    public QueryResult refine(QueryResult result) throws Exception {
    	if (query instanceof FreqQuery) {
    		return refineWithFreqQuery(result);
    	}
    	return refineWithNonFreqQuery(result);
    }
    
    public void setQueryOptions(QueryOptions queryOptions) {
    	query.setQueryOptions(queryOptions);
    }
}
