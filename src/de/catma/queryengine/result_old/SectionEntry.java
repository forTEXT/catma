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

package de.catma.queryengine.result;

import de.catma.core.document.Range;
import de.catma.core.document.source.KeywordInContext;

/**
 * An entry of a {@link org.catma.queryengine.result.DocumentSection}. An entry represents a token and its context (KWIC).
 *
 * @author Marco Petris
 *
 */
public class SectionEntry {
    private Range sectionEntryRange;
    private Range keywordSourceRange;
    private Range kwicResultDocRange;
    private Range keywordResultDocRange;
    private KeywordInContext keywordInContext;

    /**
     * Constructor.
     *
     * @param sectionEntryRange the range of this entry in the {@link org.catma.queryengine.result.SelectionResultDocument}
     * @param sourceRange the range of the token in the {@link org.catma.document.source.SourceDocument}
     * @param keywordInContext the token and its context
     * @param kwicResultDocRange the range of the complete kwic in
     * the {@link org.catma.queryengine.result.SelectionResultDocument} 
     */
    SectionEntry(
            Range sectionEntryRange,
            Range sourceRange,
            KeywordInContext keywordInContext,
            Range kwicResultDocRange) {
        
        this.sectionEntryRange = sectionEntryRange;
        this.keywordSourceRange = sourceRange;
        this.kwicResultDocRange = kwicResultDocRange;
        this.keywordInContext = keywordInContext;
        this.keywordResultDocRange =
           new Range(
               kwicResultDocRange.getStartPoint()+keywordInContext.getRelativeKeywordStartPos(),
               kwicResultDocRange.getStartPoint()
                       +keywordInContext.getRelativeKeywordStartPos()
                       +sourceRange.getSize());
    }

    /**
     * @return the range of the complete kwic in the {@link org.catma.queryengine.result.SelectionResultDocument}
     */
    public Range getKwicResultDocRange() {
        return kwicResultDocRange;
    }

    /**
     * @return the range of the complete kwic in the {@link org.catma.document.source.SourceDocument}
     */
    public Range getKwicSourceRange() {
        return keywordInContext.getKwicSourceRange();
    }

    /**
     * @return the range of the token in the {@link org.catma.queryengine.result.SelectionResultDocument}
     */
    public Range getKeywordResultDocRange() {
        return keywordResultDocRange;
    }

    /**
     * @return the type
     */
    public String getKeyword() {
        return keywordInContext.getKeyword();
    }

    /**
     * @return the range of the token in the {@link org.catma.document.source.SourceDocument}
     */
    public Range getKeywordSourceRange() {
        return keywordSourceRange;
    }

    /**
     * @return the range of this entry in the {@link org.catma.queryengine.result.SelectionResultDocument}
     */
    public Range getSectionEntryRange() {
        return sectionEntryRange;
    }

    /**
     * @return the keyword in context of this entry
     */
    public KeywordInContext getKeywordInContext() {
        return keywordInContext;
    }
}
