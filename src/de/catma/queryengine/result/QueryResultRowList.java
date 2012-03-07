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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.catma.indexer.TermInfo;

/**
 * A query result based on a list of {@link org.catma.queryengine.result.QueryResultRow}s.
 *
 * @author Marco Petris
 *
 */
public class QueryResultRowList implements QueryResult {

    private List<QueryResultRow> resultRowList;


    /**
     * Constructor.
     *
     * @param resultRowList the data of this query result
     */
    public QueryResultRowList(List<QueryResultRow> resultRowList) {
        this.resultRowList = resultRowList;
    }

    /**
     * An empty query result.
     */
    public QueryResultRowList() {
        this(new ArrayList<QueryResultRow>());
    }

    /**
     * @param row the row to add to this result
     */
    public void add(QueryResultRow row) {
        resultRowList.add(row);
    }

    public Iterator<QueryResultRow> iterator() {
        return resultRowList.iterator();
    }

    public int getTypeCount() {
        return resultRowList.size();
    }

    public List<TermInfo> getSortedTermInfoList() {
        List<TermInfo> termInfoList = new ArrayList<TermInfo>();

        for (QueryResultRow row : this) {
            termInfoList.addAll(row.getTermInfoList());
        }

        return termInfoList;
    }

    public List<TermInfo> getTermInfoList() {
        return getSortedTermInfoList();
    }

    public List<QueryResultRow> asList() {
        return Collections.unmodifiableList(resultRowList);
    }
}
