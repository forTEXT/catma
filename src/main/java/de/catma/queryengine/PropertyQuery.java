/*
 * CATMA Computer Aided Text Markup and Analysis
 *
 *    Copyright (C) 2008-2010  University Of Hamburg
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

import java.util.ArrayList;
import java.util.List;

import de.catma.document.Range;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocument;
import de.catma.indexer.Indexer;
import de.catma.project.Project;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;


/**
 * This query looks for tokens that are tagged with a {@link TagDefinition Tag} that has the desired
 * {@link Property} and value (optional)
 *
 * @author Marco Petris
 *
 */
public class PropertyQuery extends Query {

    private String propertyName;
    private String propertyValue;
    private String tagPhrase;
    
    /**
     * Constructor
     * @param property the name of the {@link Property}
     * @param value the value of the {@link Property} this is optional and can be
     * <code>null</code>
     */
    public PropertyQuery(Phrase tag, Phrase property, Phrase value) {
        propertyName = property.getPhrase();
        if (value != null) {
            propertyValue = value.getPhrase();
        }
        else {
            propertyValue = null;
        }
        if (tag != null){
        	this.tagPhrase = tag.getPhrase();
        }
        else {
        	this.tagPhrase = null;
        }
    }

    @Override
    protected QueryResult execute() throws Exception {
    	
    	QueryOptions queryOptions = getQueryOptions();
    	Project repository = queryOptions.getRepository();
    	
        Indexer indexer = queryOptions.getIndexer();
        List<String> relevantUserMarkupCollIDs = 
        		queryOptions.getRelevantUserMarkupCollIDs();
        
        if (relevantUserMarkupCollIDs.isEmpty() 
        		&& !queryOptions.getRelevantSourceDocumentIDs().isEmpty()) {
        	relevantUserMarkupCollIDs = new ArrayList<String>();
        	for (String sourceDocumentId 
        			: queryOptions.getRelevantSourceDocumentIDs()) {
        		for (AnnotationCollectionReference umcRef : 
        			repository.getSourceDocumentReference(sourceDocumentId).getUserMarkupCollectionRefs()) {
        			relevantUserMarkupCollIDs.add(umcRef.getId());
        		}
        	}
        	
        	if (relevantUserMarkupCollIDs.isEmpty()) {
        		return new QueryResultRowArray();
        	}
        }
        
        QueryResult result = 
				indexer.searchProperty(
						queryOptions.getQueryId(),
						relevantUserMarkupCollIDs,
						propertyName, propertyValue, tagPhrase);

        for (QueryResultRow row  : result) {
        	SourceDocument sd = 
        			repository.getSourceDocument(row.getSourceDocumentId());

        	TagQueryResultRow tRow = (TagQueryResultRow)row;
        	
        	if (tRow.getRanges().size() > 1) {
	        	StringBuilder builder = new StringBuilder();
	        	String conc = "";
	        	for (Range range : tRow.getRanges()) {
	        		builder.append(conc);
	        		builder.append(sd.getContent(range));
	        		conc = "[...]";
	        	}
	        	row.setPhrase(builder.toString());
        	}
        	else {
        		row.setPhrase(sd.getContent(row.getRange()));
        	}
        }
        
        return result;
    }
}
