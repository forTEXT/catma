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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.catma.indexer.TermInfo;
import de.catma.queryengine.ResultList;

/**
 * A query result based on a {@link de.catma.queryengine.catma.queryengine.result.ResultList}.
 *
 * @author Marco Petris
 *
 */
public class CompleteQueryResult implements QueryResult {

    /**
     * An empty query result.
     */
    public final static CompleteQueryResult EMPTY_RESULT = new CompleteQueryResult();

    private HashMap<String, List<TermInfo>> termRangeMap;
    private ResultList resultList;

    /**
     * Empty result constructor.
     */
    private CompleteQueryResult() {
        this(new ResultList(Collections.<TermInfo>emptyList()));
    }

    /**
     * Constructor.
     *
     * @param resultList the result data from the query.
     * @see org.catma.queryengine.Query
     */
    public CompleteQueryResult(ResultList resultList) {
        this.resultList = resultList;
        // reformat the result to a type->tokenList mapping
        termRangeMap = new HashMap<String, List<TermInfo>>();
        for (TermInfo ti : resultList.getTermInfoList()) {
            if(!termRangeMap.containsKey(ti.getTerm())) {
                termRangeMap.put(ti.getTerm(), new ArrayList<TermInfo>());
            }
            termRangeMap.get(ti.getTerm()).add(ti);
        }
    }

    public int getTypeCount() {
        return termRangeMap.keySet().size();
    }

    public Iterator<QueryResultRow> iterator() {
        return new QueryResultIterator(termRangeMap.entrySet().iterator());
    }

    public List<TermInfo> getSortedTermInfoList() {

        List<TermInfo> termInfoList = new ArrayList<TermInfo>();

        for (QueryResultRow row : this) {
            termInfoList.addAll(row.getTermInfoList());
        }

        return termInfoList;
    }

    public List<TermInfo> getTermInfoList() {
        return resultList.getTermInfoList();
    }

    public List<QueryResultRow> asList() {
        List<QueryResultRow> result = new ArrayList<QueryResultRow>();
        for (Map.Entry<String, List<TermInfo>> entry : termRangeMap.entrySet()) {
            result.add(new QueryResultRow(entry.getKey(), entry.getValue()));
        }
        return Collections.unmodifiableList(result);
    }
}
