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
 * A key for a Tag-based series.
 *
 * @author Marco Petris
 *
 */
public class DistributionXYSeriesTagKey
        implements DistributionXYSeriesKey, Comparable<DistributionXYSeriesTagKey> {
    
    private Tag tag;
    private List<QueryResultRow> rows;
    private List<Range> rangeList;

    /**
     * Constructor.
     * @param tag the Tag this key is based on
     * @param rows the list of rows that belong to the result of the Tag-based query
     * @param rangeList the list of ranges of the result
     */
    public DistributionXYSeriesTagKey(
            Tag tag, List<QueryResultRow> rows, List<Range> rangeList) {
        this.tag = tag;
        this.rows = rows;
        this.rangeList = rangeList;
    }

    public int compareTo(DistributionXYSeriesTagKey o) {
        return tag.compareTo(o.tag);
    }

    public List<Range> getRangeList() {
        return rangeList;
    }

    public QueryResultRow getRowForRange(Range range) {
        for( QueryResultRow row : rows) {
            if (row.getRangeList().contains(range)) {
                return row;
            }
        }
        return null;
    }

    public Tag getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return tag.toString();
    }
}
