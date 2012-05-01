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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.catma.core.document.Range;
import de.catma.core.util.Pair;
import de.catma.queryengine.result.pager.Pager;

/**
 * A ResultDocument implementation that is based on a {@link org.catma.document.source.SourceDocument}. The
 * context of the results is therefore the whole document and results (tokens) are not sorted by 'type'.
 *
 * @author Marco Petris
 *
 */
public class SourceResultDocument implements ResultDocument {
    private Pager pager;

    /**
     * Constructor.
     * @param pager the pager that contains the {@link org.catma.document.source.SourceDocument} and
     * the {@link org.catma.queryengine.result.QueryResult} that form the basis of this document. 
     */
    public SourceResultDocument(Pager pager) {
        this.pager = pager;
    }

    /**
     * No conversion needed in this implementation since ResultDocument and SourceDocument have
     * the same reference system.
     *  
     * @param sourceRange the range to convert
     * @param range the reference system
     * @return the sourceRange
     */
    public Range convertFromSourceDocumentRangeRelativeToRange(Range sourceRange, Range range) {
        return sourceRange;
    }

    /**
     * No conversion needed in this implementation since ResultDocument and SourceDocument have
     * the same reference system.
     * @param range the range to convert
     * @return the given range is simply returned
     */
    public Range convertToSourceDocumentRange(Range range) {
        return range;
    }

    public List<Range> getKeywordRangeList() {
        List<Range> result = new ArrayList<Range>();

        for( QueryResultRow row : pager.getQueryResult()) {
            result.addAll(row.getRangeList());
        }

        return result;
    }

    /**
     * No conversion needed in this implementation since ResultDocument and SourceDocument have
     * the same reference system.
     * @param queryResultRow the reference system of the given Range
     * @param range the range to convert
     * @return the given range is simply returned
     */
    public Range getResultDocRangeFor(QueryResultRow queryResultRow, Range range) {
        return range;
    }

    public String getContent() {
        try {
			return pager.getSourceDocument().getContent();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    public Pair<QueryResultRow, Range> getQueryResultRowRange(Range range) {
        for( QueryResultRow row : pager.getQueryResult()) {
            for(Range r : row.getRangeList()) {
                if( r.hasOverlappingRange(range)) {
                    return new Pair<QueryResultRow, Range>(row, r);
                }
            }
        }

        return null;
    }

    /**
     * @return an empty list, this implementation does not use header sections
     */
    public List<Range> getHeaderRangeList() {
        return Collections.emptyList();
    }
}
