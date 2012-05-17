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

import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRowArray;

/**
 * An operator that combines the results of two queries.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class UnionQuery extends Query {

    private Query query1;
    private Query query2;

    /**
     * Constructor.
     * @param query1 the first query
     * @param query2 the second query
     */
    public UnionQuery(Query query1, Query query2) {
        this.query1 = query1;
        this.query2 = query2;
    }

    @Override
    protected QueryResult execute() throws Exception {
    	query1.setQueryOptions(getQueryOptions());
    	query2.setQueryOptions(getQueryOptions());

    	QueryResultRowArray unifiedResult = 
    			query1.getResult().asQueryResultRowArray();
    	QueryResultRowArray r2 = query2.getResult().asQueryResultRowArray();
    	
    	r2.removeAll(unifiedResult);
    	unifiedResult.addAll(r2);
    	
    	return unifiedResult;
    }
    
}
