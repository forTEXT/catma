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

import de.catma.indexer.TermInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An iterator over a {@link org.catma.queryengine.result.QueryResult}.
 *
 * @author Marco Petris
 *
 */
public class QueryResultIterator implements Iterator<QueryResultRow> {

    private Iterator<Map.Entry<String, List<TermInfo>>> termRangeMapIterator;

    /**
     * Constructor.
     * 
     * @param termRangeMapIterator the data to iterate
     */
    QueryResultIterator(Iterator<Map.Entry<String,List<TermInfo>>> termRangeMapIterator) {
        this.termRangeMapIterator = termRangeMapIterator;
    }

    public boolean hasNext() {
        return termRangeMapIterator.hasNext();
    }

    public QueryResultRow next() {
        Map.Entry<String,List<TermInfo>> nextEntry = termRangeMapIterator.next();
        return new QueryResultRow(nextEntry.getKey(), nextEntry.getValue());
    }

    public void remove() {
        termRangeMapIterator.remove();
    }
}
