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

import java.util.List;

import de.catma.core.document.Range;
import de.catma.core.util.Pair;

/**
 * A ResultDocument holds one or more selection results and can provide context (KWIC) for
 * these selection results. 
 *
 * @author Marco Petris
 *
 * @see org.catma.queryengine.result.QueryResultRow
 * @see org.catma.queryengine.result.QueryResult
 */
public interface ResultDocument extends RangeConverter {
    /**
     * @return a list of the Ranges of all the keywords (hits) contained by the results of this document.
     */
    public List<Range> getKeywordRangeList();

    /**
     * Converts the given ({@link org.catma.document.result.SourceResultDocument-}range of the given
     * result row to the corresponding Range of this document.
     * @param queryResultRow the reference system of the given Range
     * @param range the range to convert
     * @return the corresponding/converted range
     * @see #getQueryResultRowRange(org.catma.document.Range)
     */
    public Range getResultDocRangeFor(QueryResultRow queryResultRow, Range range);

    /**
     * @return the string representation of the content of this document
     */
    public String getContent();

    /**
     * Converts the given (ResultDocument-)Range to the corresponding
     * ({@link org.catma.document.result.SourceResultDocument-}range.
     * @param range the range to convert
     * @return the corresponding/converted range and its reference system
     */
    public Pair<QueryResultRow, Range> getQueryResultRowRange(Range range);

    /**
     * @return if the implementation contains special header regions for each
     * {@link org.catma.queryengine.result.QueryResultRow} this method returns
     * a list of the Ranges of this header regions, else implementors should return
     * an empty list. This method never returns <code>null</code>!
     */
    public List<Range> getHeaderRangeList();
}
