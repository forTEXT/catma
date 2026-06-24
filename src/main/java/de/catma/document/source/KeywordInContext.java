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

package de.catma.document.source;

import de.catma.document.Range;

/**
 * A keyword and its context.
 *
 * @author marco.petris@web.de
 *
 */
public class KeywordInContext {
    private final String keyword;
    private final Range keywordRange;
    private final String kwic;
    private final Range kwicSourceRange;
    private final int relativeKeywordStartPos;
    private final boolean rtl;

    /**
     * Constructor.
     *
     * @param keyword the keyword
     * @param kwic the keyword in context
     * @param kwicSourceRange the corresponding Range within the {@link de.catma.document.source.SourceDocument}
     * @param relativeKeywordStartPos the start position of the keyword within the context string
     */
    public KeywordInContext(
            String keyword, String kwic,
            Range kwicSourceRange, int relativeKeywordStartPos,
            boolean rtl) {
        this.keyword = keyword;
        this.kwic = kwic;
        this.kwicSourceRange = kwicSourceRange;
        this.relativeKeywordStartPos = relativeKeywordStartPos;
        this.keywordRange =
                new Range(
                        kwicSourceRange.getStartPoint() + relativeKeywordStartPos,
                        kwicSourceRange.getStartPoint() + relativeKeywordStartPos + keyword.length());
        this.rtl = rtl;
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
    public int getRelativeKeywordStartPos() {
        return relativeKeywordStartPos;
    }

    /**
     * Returns the KWIC string to be exported.
     * Based on the direction of the text, the left and right context is swapped.
     *
     * @return the KWIC string to be exported
     */
    public String getStringForExport() {
        if (rtl) {
            return getRightContext()
                    + "***" + keyword + "***"
                    + getLeftContext();
        }
        return getLeftContext()
                + "***" + keyword + "***"
                + getRightContext();
    }

    /**
     * Returns the left context, based on the direction of the text.
     * This string is shown in the KWIC view in the **left** column, which
     * corresponds to the word before the keyword for left-to-right text
     * and to the word after the keyword for right-to-left text.
     *
     * @return the left context of the KWIC keyword
     */
    public String getLeftContext() {
        return rtl ?
                getKwic().substring(getRelativeKeywordStartPos() + keyword.length()) :
                getKwic().substring(0, getRelativeKeywordStartPos());
    }

    /**
     * Returns the right context, based on the direction of the text.
     * This string is shown in the KWIC view in the **right** column, which
     * corresponds to the word after the keyword for left-to-right text
     * and to the word before the keyword for right-to-left text.
     *
     * @return the right context of the KWIC keyword
     */
    public String getRightContext() {
        return rtl ?
                getKwic().substring(0, getRelativeKeywordStartPos()) :
                getKwic().substring(getRelativeKeywordStartPos() + keyword.length());
    }

    public boolean isRightToLeft() {
        return rtl;
    }

    public Range getKeywordRange() {
        return keywordRange;
    }
}
