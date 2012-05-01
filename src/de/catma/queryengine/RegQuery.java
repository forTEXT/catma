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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.catma.core.document.source.SourceDocument;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.result.QueryResult;

/**
 * A regular expression query.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class RegQuery extends Query {
    /**
     * flag to signal a case insensitive search.
     */
    public static final String CI = "CI";

    private Phrase phrase;
    private boolean caseInsensitive;

    /**
     * Constructor.
     * @param phrase the regular expression
     * @param caseInsensitiveMarker <code>null</code> for case sensitivity
     * or {@link #CI} for case insensitivity (other input is treated as <code>null</code>)
     */
    public RegQuery(Phrase phrase, String caseInsensitiveMarker) {
        this.phrase = phrase;
        caseInsensitive = ((caseInsensitiveMarker!=null)&&(caseInsensitiveMarker.equals(CI)));
    }

    @Override
    protected QueryResult execute() throws Exception {
//
//        SourceDocument sourceDoc = FileManager.SINGLETON.getCurrentSourceDocument();
//
//        int flags = Pattern.DOTALL;
//        if (caseInsensitive) {
//            flags |= Pattern.CASE_INSENSITIVE;
//        }
//        Pattern pattern = Pattern.compile(phrase.getPhrase(), flags);
//
//        Matcher matcher = pattern.matcher(sourceDoc.getContent());
//
//        List<TermInfo> searchResults = new ArrayList<TermInfo>();
//
//        while(matcher.find()) {
//            searchResults.add(
//                new TermInfo(matcher.group(), matcher.start(), matcher.end()));
//        }
//
//        return new ResultList(searchResults);
    	return null;
    }
}
