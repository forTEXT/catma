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

import java.util.List;
import java.util.Map;

import de.catma.core.document.Range;
import de.catma.indexer.elasticsearch.TermExtractor;

/**
 * A query for tokens that match an exact phrase or the phrase of a more advanced query like the
 * {@link org.catma.queryengine.TagQuery}.
 *
 *
 * @author Marco Petris <marco.petris@web.de>
 * @see org.catma.queryengine.TagQuery
 * @see org.catma.queryengine.SimilQuery
 * @see org.catma.queryengine.RegQuery
 * @see org.catma.queryengine.PropertyQuery
 */
public class Phrase extends Query {

    private String phrase;

    /**
     * Constructor.
     * @param phrase the phrase of this query
     */
    public Phrase(String phrase) {
        this.phrase = phrase.substring(1,phrase.length()-1).replace("\\\"","\"");
    }

    @Override
    protected QueryResult execute() throws Exception {
    	QueryOptions options = getQueryOptions();
        TermExtractor termExtractor =
                new TermExtractor(
                		phrase,
                        options.getUnseparableCharacterSequences(),
                        options.getUserDefinedSeparatingCharacters(),
                        options.getLocale());

        List<String> termList = termExtractor.getTermsInOrder();
        
        Map<String,List<Range>> hits = 
        		getIndexer().searchTerm(options.getDocumentIds(), termList);
        QueryResultRowArray queryResult = new QueryResultRowArray();
        
        for (Map.Entry<String,List<Range>> entry : hits.entrySet()) {
        	String documentId = entry.getKey();
        	for (Range r : entry.getValue()) {
        		queryResult.add(new QueryResultRow(documentId, r));
        	}
        }
        return queryResult;
    }

    /**
     * Complex queries can use the phrase of this query for their execution. 
     * @return the phrase of this query
     * 
     * @see org.catma.queryengine.TagQuery
     * @see org.catma.queryengine.SimilQuery
     * @see org.catma.queryengine.RegQuery
     * @see org.catma.queryengine.PropertyQuery
     */
    public String getPhrase() {
        return phrase;
    }
}
