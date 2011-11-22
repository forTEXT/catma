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

package de.catma.core.document.source;

import de.catma.core.document.Range;

/**
 * A keyword and its context.
 *
 * @author Marco Petris
 *
 */
public class KeywordInContext {
    private String keyword;
    private String kwic;
    private Range kwicSourceRange;
    private long relativeKeywordStartPos;

    /**
     * Constructor.
     * @param keyword the keyword
     * @param kwic the keyword in context
     * @param kwicSourceRange the corresponding Range within the {@link de.catma.document.source.SourceDocument}
     * @param relativeKeywordStartPos the start position of the keyword within the context string
     */
    public KeywordInContext(
            String keyword, String kwic,
            Range kwicSourceRange, long relativeKeywordStartPos) {
        this.keyword = keyword;
        this.kwic = kwic;
        this.kwicSourceRange = kwicSourceRange;
        this.relativeKeywordStartPos = relativeKeywordStartPos;
    }

    /**
     * @return the keyword in context as a string
     */
    public String getKwic() {
        return kwic;
    }

    /**
     * @return the corresponding Range within the {@link de.catma.document.source.SourceDocument}
     */
    public Range getKwicSourceRange() {
        return kwicSourceRange;
    }

    /**
     * @return the keyword
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * @return the start position of the keyword within the context string
     */
    public long getRelativeKeywordStartPos() {
        return relativeKeywordStartPos;
    }

    @Override
    public String toString() {
        return getKwic();
    }
}
