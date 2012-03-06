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
 * A key for a row based series.
 *
 * @author Marco Petris
 *
 */
public class DistributionXYSeriesRowKey implements DistributionXYSeriesKey, Comparable<DistributionXYSeriesRowKey> {

    private QueryResultRow row;

    /**
     * Constructor.
     * @param row the row the series with this key is based on
     */
    public DistributionXYSeriesRowKey(QueryResultRow row) {
        this.row = row;
    }

    public List<Range> getRangeList() {
        return row.getRangeList();
    }

    public QueryResultRow getRowForRange(Range range) {
        return row;
    }

    public Tag getTag() {
        if (row.getTermInfoList().size() > 0) {
            return row.getTermInfoList().get(0).getTag();
        }
        return null;
    }

    public int compareTo(DistributionXYSeriesRowKey o) {
        return this.row.getTextAsSingleLineWithEllipsis().compareTo(
                o.row.getTextAsSingleLineWithEllipsis());
    }

    @Override
    public String toString() {
        return row.getTextAsSingleLineWithEllipsis();
    }
}
