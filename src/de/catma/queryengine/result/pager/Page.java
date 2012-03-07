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
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.ResultDocument;
import de.catma.queryengine.result.SelectionResultDocument;

/**
 * This is a ResultDocument that contains a portion of the complete query results.
 * 
 *
 * @author Marco Petris
 *
 */
public class Page implements ResultDocument {

    private List<QueryResultRow> resultRows;
    private int startIndex;
    private int endIndex;
    private int kwicSize;

    private int curApproxSize = 0;
    private SourceDocument sourceDocument;
    private SelectionResultDocument selectionResultDocument;


    /**
     * Constructor.
     * @param sourceDocument the source document on which is results are based
     * @param kwicSize the current keyword in context span size
     */
    public Page(SourceDocument sourceDocument, int kwicSize) {
        this.sourceDocument = sourceDocument;
        this.kwicSize = kwicSize;
        resultRows = new ArrayList<QueryResultRow>();
    }

    /**
     * @return <code>false</code> if this page is full, i. e. its maximum size has been reached or exceeded,
     * else <code>true</code>
     */
    public boolean hasSizeLeft() {
        return (Pager.MAX_PAGE_SIZE-curApproxSize) > 0;
    }

    /**
     * Adds a chunk to this page. The number range of the indices of the chunks added must be complete and contain no
     * gaps.
     *
     * @param row the data-set that contains the chunk to add
     * @param chunkSiz the approx. size of the chunk to add including the context for a KWIC-display.
     * @param entryIndex the index of the {@link org.catma.document.Range}-entry of the data-set that represents the
     * the chunk we want to add
     */
    public void addChunk(QueryResultRow row, int chunkSiz, int entryIndex) {
        curApproxSize+=chunkSiz;

        if (resultRows.size() == 0) {
            startIndex = entryIndex;
        }

        if (!resultRows.contains(row)) {
            resultRows.add(row);
        }

        endIndex = entryIndex;
    }

    /**
     * @return the KWIC-style result document that contains the data added to this page (lazy initialization)
     */
    private SelectionResultDocument getSelectionResultDocument() {
        if (selectionResultDocument == null) {
            selectionResultDocument =
                    new SelectionResultDocument(
                            resultRows, startIndex, endIndex, sourceDocument, kwicSize);
        }

        return selectionResultDocument;
    }

    public List<Range> getKeywordRangeList() {
        return getSelectionResultDocument().getKeywordRangeList();
    }

    /**
     * @param queryResultRow the reference system for the given range
     * @param range the range of the {@link org.catma.document.source.SourceDocument}
     * @return true if this page contains the entry specified by the given row/range pair
     */
    public boolean containsResultDocRangeFor(QueryResultRow queryResultRow, Range range) {
        for (int idx=0; idx<resultRows.size(); idx++) {
            QueryResultRow curRow = resultRows.get(idx);
            if (curRow.equals(queryResultRow)) {
                int start = 0;
                if (idx==0) {
                    start = startIndex;
                }
                int end = curRow.getRangeList().size();
                if (idx == resultRows.size()-1) {
                    end = endIndex+1;
                }

                return curRow.getRangeList().subList(start, end).contains(range);
            }
        }

        return false;
    }

    public Range getResultDocRangeFor(QueryResultRow queryResultRow, Range range) {
        return getSelectionResultDocument().getResultDocRangeFor(queryResultRow, range);
    }

    public String getContent() {
        return getSelectionResultDocument().getContent();
    }

    public Pair<QueryResultRow, Range> getQueryResultRowRange(Range range) {
        return getSelectionResultDocument().getQueryResultRowRange(range);
    }

    public List<Range> getHeaderRangeList() {
        return getSelectionResultDocument().getHeaderRangeList();
    }

    public Range convertFromSourceDocumentRangeRelativeToRange(Range sourceRange, Range range) {
        return getSelectionResultDocument().convertFromSourceDocumentRangeRelativeToRange(sourceRange,range);
    }

    public Range convertToSourceDocumentRange(Range range) {
        return getSelectionResultDocument().convertToSourceDocumentRange(range);
    }
}
