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

package de.catma.queryengine.result.pager;

import java.util.ArrayList;
import java.util.List;

import de.catma.core.document.Range;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.util.Pair;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;

/**
 * Splits a {@link org.catma.queryengine.result.QueryResult} into a list of lazy initialized
 * {@link org.catma.queryengine.result.pager.Page pages}.
 *
 * @author Marco Petris
 *
 */
public class Pager {

    static int MAX_PAGE_SIZE = 750;

    private QueryResult queryResult;
    private SourceDocument sourceDocument;
    private List<Page> pageList;

    /**
     * Constructor.
     * @param queryResult the result this pager should manage
     * @param sourceDocument the source document the result is based on
     * @param kwicSize the initial size of the keyword in context span
     */
    public Pager(QueryResult queryResult, SourceDocument sourceDocument, int kwicSize) {
        this.queryResult = queryResult;
        this.sourceDocument = sourceDocument;
        pageList = new ArrayList<Page>();
        computePages(kwicSize);
    }

    /**
     * Computes the list of pages.
     * @param kwicSize the initial size of the keyword in context span
     */
    private void computePages(int kwicSize) {
        Page curPage = createPage(kwicSize);

        for (QueryResultRow row : queryResult) {

            // approx. size of an entry of the row
            int entrySize = row.getText().length() + 2*kwicSize;

            for (int entryIndex=0; entryIndex<row.getRangeList().size(); entryIndex++) {
                if (!curPage.hasSizeLeft()) {
                    curPage = createPage(kwicSize);
                }

                curPage.addChunk(row, entrySize, entryIndex);
            }
        }
    }

    /**
     * @param kwicSize the initial size of the keyword in context span
     * @return a newly created page
     */
    private Page createPage(int kwicSize) {
        Page page = new Page(sourceDocument, kwicSize);
        pageList.add(page);
        return page;
    }

    /**
     * @return the number of pages of this pager
     */
    public int getPageCount() {
        return pageList.size();
    }

    /**
     * @return the query result managed by this pager
     */
    public QueryResult getQueryResult() {
        return queryResult;
    }


    /**
     * @return the source document the results of this pager are based on
     */
    public SourceDocument getSourceDocument() {
        return sourceDocument;
    }

    /**
     * @param page the page index
     * @return the page with the given index
     */
    public Page getPage(int page) {
        return pageList.get(page);
    }

    /**
     * @param kwicSize the new size of the keyword in context span
     */
    public void setKwicSize(int kwicSize) {
        pageList.clear();
        computePages(kwicSize);
    }

    /**
     * Gets the page index which contains the given row/range pair.
     * @param queryResultRowRange the row/range pair we want the page for
     * @return the page index or the first page if no other page could be found
     */
    public int getPageFor(Pair<QueryResultRow, Range> queryResultRowRange) {
        if (queryResultRowRange != null) {
            for (int idx=0; idx<pageList.size(); idx++) {
                if (pageList.get(idx).containsResultDocRangeFor(
                    queryResultRowRange.getFirst(), queryResultRowRange.getSecond())) {
                    return idx;
                }
            }
        }

        return 0;
    }
}
