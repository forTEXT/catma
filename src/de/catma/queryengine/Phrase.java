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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import de.catma.core.document.Range;
import de.catma.indexer.WhitespaceAndPunctuationAnalyzer;
import de.catma.queryengine.result.ResultList;

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
    protected ResultList execute() throws Exception {
    	
        WhitespaceAndPunctuationAnalyzer analyzer =
                new WhitespaceAndPunctuationAnalyzer(
                        this.getUnseparableCharacterSequences(),
                        this.getUserDefinedSeparatingCharacters(),
                        this.getLocale());

        TokenStream ts =
                analyzer.tokenStream(
                        null, // our analyzer does not use the fieldname 
                        new StringReader(phrase));
        List<String> termList = new ArrayList<String>();
        while(ts.incrementToken()) {
            CharTermAttribute termAttr =
                    (CharTermAttribute)ts.getAttribute(CharTermAttribute.class);
            termList.add(termAttr.toString());
        }
        Map<String,List<Range>> result = 
        		getIndexer().searchTerm(getDocumentIds(), termList);
        
        return new ResultList();
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
