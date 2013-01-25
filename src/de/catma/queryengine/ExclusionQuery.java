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

import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

/**
 * An exclusion query takes the results of the first query and substracts the results of the second
 * query.<br>
 * <br>
 * To decide if a token of the second result should be substracted from the first
 * {@link java.util.Comparator Comparator&le;TermInfo&ge;}s are used.
 * If the {@link org.catma.queryengine.Query queries} bring their own 
 * {@link org.catma.queryengine.Query#getComparator() comparators} those custom comparators will be used
 * to test for equality, otherwise the standard equality of {@link org.catma.indexer.TermInfo} will be used.
 * If both queries provide a custom comparator only one of them needs to signal equality for a TermInfo
 * to be excluded from the final result, i. e. the one that signals equality is the one that wins.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class ExclusionQuery extends Query {

    private Query query1;
    private Query query2;

    public ExclusionQuery(Query query1, Query query2) {
        this.query1 = query1;
        this.query2 = query2;
    }

    @Override
    protected QueryResult execute() throws Exception {
    	
    	query1.setQueryOptions(getQueryOptions());
    	query2.setQueryOptions(getQueryOptions());
    	
    	QueryResultRowArray result1 = query1.getResult().asQueryResultRowArray();
    	QueryResultRowArray result2 = query2.getResult().asQueryResultRowArray();
    	
    	Comparator<QueryResultRow> comparator1 = query1.getComparator();
    	Comparator<QueryResultRow> comparator2 = query2.getComparator();
    	if ( (comparator1 == null) && (comparator2 == null) ) {
    		result1.removeAll(result2);
    	}
    	else {
    		if (comparator1 != null) {
                removeWithComparator(result1, result2, comparator1);
            }

            if ((comparator1 != comparator2) && (comparator2 != null)) {
                removeWithComparator(result1, result2, comparator2);
            }
        }
    	return result1;
    }

    /**
     * Remove each item from the first list that is considered equal to the one of the item in
     * the second list testing with the given comparator.
     * @param results1 the result list items shall be removed from
     * @param results2 the result list to check against
     * @param comparator the comparator to use for the tests
     */
    private void removeWithComparator(
            QueryResultRowArray result1, 
            QueryResultRowArray results2, 
            Comparator<QueryResultRow> comparator) {
    	
        Iterator<QueryResultRow> baseResultIterator = result1.iterator();
        while (baseResultIterator.hasNext()) {
            QueryResultRow row  = baseResultIterator.next();
            if(row.existsIn(results2,comparator)) {
                baseResultIterator.remove();
            }
        }
    }

}
