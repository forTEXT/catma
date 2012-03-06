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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.catma.core.document.source.SourceDocument;
import de.catma.indexer.Index;
import de.catma.indexer.TermInfo;
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

        SourceDocument sourceDoc = FileManager.SINGLETON.getCurrentSourceDocument();

        Index index = sourceDoc.getIndex();

        // the phrase can contain more than one type
        List<TermInfo> phraseTerms = index.extractTermInfoFrom(phrase,0);

        if (phraseTerms.size() == 0) {
            return new ResultList(Collections.<TermInfo>emptyList());
        }
        // get the tokens for the first type of the phrase
        List<TermInfo> searchResults = index.search(phraseTerms.get(0).getTerm());

        if (phraseTerms.size() > 1) {
            List<TermInfo> phraseResults = new ArrayList<TermInfo>();

            // loop over the tokens and test the remaining tokens of the phrase
            Iterator<TermInfo> iter = searchResults.iterator();
            while(iter.hasNext()) {
                TermInfo currentToken = iter.next();

                // for the current token we retrieve the text fragment
                // with the same length as the phrase we are looking for
                String textFragmentToTest =
                        sourceDoc.getContent(
                            currentToken.getRange().getStartPoint(),
                            currentToken.getRange().getStartPoint()+phrase.length());

                // is the text fragment our phrase?
                if (textFragmentToTest.equals(phrase)) {
                    // yes, ok then this is a valid token for the query results
                    phraseResults.add(
                            new TermInfo(
                                textFragmentToTest,
                                (int)currentToken.getRange().getStartPoint(),
                                (int)currentToken.getRange().getStartPoint()+phrase.length()));
                }

            }
            return new ResultList(phraseResults);
        }

        return new ResultList(searchResults);
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
