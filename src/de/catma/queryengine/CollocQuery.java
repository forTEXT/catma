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

import de.catma.indexer.Indexer;
import de.catma.indexer.SpanDirection;
import de.catma.queryengine.result.QueryResult;

/**
 * A collocation query looks for terms that form a collocation with other terms within a given
 * span context. 
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class CollocQuery extends Query {

    public static final int DEFAULT_SPANCONTEXT_SIZE = 5;
    
    private Query query1;
    private Query query2;
    private int spanContextSize;
    private SpanDirection direction;

    /**
     * Constructor.
     * @param query1 the definiton of the search term
     * @param query2 the defintion of the collocation term
     * @param spanContext the size of the context
     * (needs to be parseable to {@link Integer#parseInt(String) Integer}) 
     * or <code>null</code> for the default size.
     * @param direction the direction of the span context
     */
    CollocQuery(Query query1, Query query2, String spanContext, SpanDirection direction) {
        this.query1 = query1;
        this.query2 = query2;
        
        if (spanContext == null) {
            spanContextSize = DEFAULT_SPANCONTEXT_SIZE;
        }
        else {
            spanContextSize = Integer.parseInt(spanContext);
        }
        this.direction = direction;
    }

    /**
     * Constructor. ({@link de.catma.indexer.catma.queryengine.SpanDirection#BOTH both} directions are used as the span context)
     * @param query1 the definiton of the search term
     * @param query2 the defintion of the collocation term
     * @param spanContext the size of the context
     * (needs to be parseable to {@link Integer#parseInt(String) Integer})
     * or <code>null</code> for the default size.
     */
    public CollocQuery(Query query1, Query query2, String spanContext) {
        this(query1, query2, spanContext, SpanDirection.BOTH);
    }

    @Override
    protected QueryResult execute() throws Exception {
    	QueryResult baseResult = query1.execute();
    	QueryResult collocCondition = query2.execute();
    	
    	Indexer indexer = getQueryOptions().getIndexer();
    	return indexer.searchCollocation(baseResult, collocCondition, spanContextSize, direction);
    }

    @Override
    public void setQueryOptions(QueryOptions queryOptions) {
    	super.setQueryOptions(queryOptions);
        this.query1.setQueryOptions(getQueryOptions());
        this.query2.setQueryOptions(getQueryOptions());
    }
}
