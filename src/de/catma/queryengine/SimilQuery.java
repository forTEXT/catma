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

import de.catma.core.document.source.SourceDocument;
import de.catma.indexer.Index;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.result.ResultList;

/**
 * A query that looks for phrases that are {@link org.catma.queryengine.Simil similar} to the given
 * phrase to a certain degree.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class SimilQuery extends Query {

    private String phrase;
    private int similPercent;

    /**
     * Constructor.
     * @param phraseQuery the phrase results should be similar to
     * @param percentValue the degree of similarity,
     * must be parseable to {@link Integer#parseInt(String) Integer}
     */
    public SimilQuery(Phrase phraseQuery, String percentValue) {
        this.phrase = phraseQuery.getPhrase();
        this.similPercent = Integer.parseInt(percentValue);
    }

    @Override
    protected ResultList execute() throws Exception {

        SourceDocument sourceDoc = FileManager.SINGLETON.getCurrentSourceDocument();
        Index index = sourceDoc.getIndex();


        //TODO: enable phrases rather than words for simil
//        int maxWindowSize =
//            (int)Math.round((
//                phrase.length()*2.0-((similPercent/100.0)*phrase.length()))/(similPercent/100.0));
//
//        int minWindowSize =
//            (int)Math.round(
//                (phrase.length()*(similPercent/100.0))/(2-(similPercent/100.0)));

        List<TermInfo> termInfoList = index.searchTermsBySimilarity(phrase, similPercent);

        return new ResultList(termInfoList);
    }

    

}
