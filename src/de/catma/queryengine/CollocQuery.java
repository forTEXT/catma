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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.KwicProvider;
import de.catma.indexer.SpanContext;
import de.catma.indexer.SpanDirection;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

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
    public CollocQuery(Query query1, Query query2, String spanContext, SpanDirection direction) {
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
     * Constructor. ({@link de.catma.indexer.catma.queryengine.SpanDirection#Both both} directions are used as the span context)
     * @param query1 the definiton of the search term
     * @param query2 the defintion of the collocation term
     * @param spanContext the size of the context
     * (needs to be parseable to {@link Integer#parseInt(String) Integer})
     * or <code>null</code> for the default size.
     */
    public CollocQuery(Query query1, Query query2, String spanContext) {
        this(query1, query2, spanContext, SpanDirection.Both);
    }

    @Override
    protected QueryResult execute() throws Exception {
    	QueryResult baseResult = query1.execute();
    	QueryResult collocCondition = query2.execute();
    	
    	Map<String,KwicProvider> kwicProviders = new HashMap<String, KwicProvider>(); 
    	Set<SourceDocument> toBeUnloaded = new HashSet<SourceDocument>();
    	Repository repository = getQueryOptions().getRepository();
    	
    	Map<QueryResultRow, List<TermInfo>> termInfos = 
    			new HashMap<QueryResultRow, List<TermInfo>>();
    	
    	QueryResultRowArray result = new QueryResultRowArray();
    	
    	for (QueryResultRow row : baseResult) {
    		SourceDocument sd = 
    				repository.getSourceDocument(row.getSourceDocumentId());
    		if (!sd.isLoaded()) {
    			//TODO: unload SourceDocuments to free space if tobeUnloaded.size() > 10
    			toBeUnloaded.add(sd);
    		}
    		
    		if (!kwicProviders.containsKey(sd.getID())) {
    			kwicProviders.put(sd.getID(), new KwicProvider(sd));
    		}
    		KwicProvider kwicProvider = kwicProviders.get(sd.getID());
    		SpanContext spanContext =
				kwicProvider.getSpanContextFor(	
					row.getRange(), 
					spanContextSize, direction);
    		
    		if (spanContextMeetsCollocCondition
    				(spanContext, collocCondition, termInfos)) {
    			result.add(row);
    		}
    	}
    	for (SourceDocument sd : toBeUnloaded) {
    		sd.unload();
    	}
   
    	return result;
    }

	private boolean spanContextMeetsCollocCondition(
			SpanContext spanContext, QueryResult collocationConditionResult, 
			Map<QueryResultRow, List<TermInfo>> rowToTermInfoListMapping) throws IOException {
		
		
		for (QueryResultRow collocConditionRow : collocationConditionResult) {
			if (spanContext.getSourceDocumentId().equals(
					collocConditionRow.getSourceDocumentId())) {
				if (!rowToTermInfoListMapping.containsKey(collocConditionRow)) {
					rowToTermInfoListMapping.put(
							collocConditionRow, 
							getQueryOptions().getIndexer().getTermInfosFor( //TODO: better use direct doc access via kwicprovider instead of DB
									collocConditionRow.getSourceDocumentId(), 
									collocConditionRow.getRange()));
				}
				
				if (spanContext.contains(
						rowToTermInfoListMapping.get(collocConditionRow))) {
					return true;
				}
			}
		}
		
		return false;
	}

    @Override
    public void setQueryOptions(QueryOptions queryOptions) {
    	super.setQueryOptions(queryOptions);
        this.query1.setQueryOptions(getQueryOptions());
        this.query2.setQueryOptions(getQueryOptions());
    }
}
