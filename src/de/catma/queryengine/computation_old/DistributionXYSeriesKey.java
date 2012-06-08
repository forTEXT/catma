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

package de.catma.queryengine.computation;

import java.util.List;

import de.catma.queryengine.result.QueryResultRow;
import de.catma.core.document.Range;

/**
 * A key for a {@link org.jfree.data.xy.XYSeries}.
 *
 * @author Marco Petris
 *
 */
public interface DistributionXYSeriesKey {

    /**
     * @return the list of Ranges the series is based on
     */
    List<Range> getRangeList();

    /**
     * Retrieves the result row for the given Range
     * @param range the range we want the row for
     * @return the corresponding row or <code>null</code> if there is no such row
     */
    QueryResultRow getRowForRange(Range range);

    /**
     * @return the Tag the query was based on or <code>null</code> if query was not Tag-based 
     */
    Tag getTag();

}
