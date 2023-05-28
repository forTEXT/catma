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

/**
 * The base class for all queries.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public abstract class Query {
	
    private Refinement refinement;
    private QueryOptions queryOptions;

    /**
     * Executes a query and returns a {@link QueryResult} that has
     * not been refined yet by the {@link Refinement} of this query.
     * <p>
     * Normally this method does not need to be called directly. To execute a query
     * and to retrieve the final execution result you should call {@link #getResult()} instead.
     *
     * @return the <strong>unrefined</strong> result, never <code>null</code>
     * @throws Exception see instance for details
     */
    protected abstract QueryResult execute() throws Exception;

    /**
     * @return the result of the execution, optionally refined by a {@link Refinement}.
     * @throws Exception see instance for details 
     */
    public QueryResult getResult() throws Exception {

        QueryResult result = execute();
        
        if(refinement != null) {
        	refinement.setQueryOptions(queryOptions);
            return refinement.refine(result);
        }

        return result;
    }

    /**
     * @param refinement the new refinement for the execution result
     */
    public void setRefinement(Refinement refinement) {
        this.refinement = refinement;
    }
    
    public void setQueryOptions(QueryOptions queryOptions) {
		this.queryOptions = queryOptions;
	}
    
    public QueryOptions getQueryOptions() {
		return queryOptions;
	}
}
