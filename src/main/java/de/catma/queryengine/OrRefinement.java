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

import java.util.HashSet;
import java.util.Set;

import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

/**
 * This refinement combines to refinement conditions by a logical OR.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class OrRefinement implements Refinement {
    private Refinement refinement1;
    private Refinement refinement2;

     /**
     * Constructor
     * @param refinement1 first refinement condition
     * @param refinement2 second refinement condition
     */
    public OrRefinement( Refinement refinement1, Refinement refinement2) {
        this.refinement1 = refinement1;
        this.refinement2 = refinement2;
    }

    public QueryResult refine(QueryResult result) throws Exception {
    	QueryResultRowArray refinedResult1 = 
    			refinement1.refine(result).asQueryResultRowArray();
    	QueryResultRowArray refinedResult2 = 
    			refinement2.refine(result).asQueryResultRowArray();

        Set<QueryResultRow> withoutDuplicates = new HashSet<QueryResultRow>();
        withoutDuplicates.addAll(refinedResult1);
        withoutDuplicates.addAll(refinedResult2);

        QueryResultRowArray combinedResult = new QueryResultRowArray();
        combinedResult.addAll(withoutDuplicates);
    	return combinedResult;
    }

    public void setQueryOptions(QueryOptions queryOptions) {
    	this.refinement1.setQueryOptions(queryOptions);
    	this.refinement2.setQueryOptions(queryOptions);
    }
}
