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
import java.util.List;

import de.catma.core.ExceptionHandler;
import de.catma.core.document.Range;
import de.catma.core.document.source.KeywordInContext;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.util.Pair;


/**
 * Query results presented in a kwic style, sorted by type.
 *
 * @author Marco Petris
 *
 */
public class SelectionResultDocument implements ResultDocument {

    private List<DocumentSection> documentSectionList;
    private String content;
    private List<QueryResultRow> resultRows;
    private int startIndex;
    private int endIndex;
    private SourceDocument sourceDocument;
    private int kwicSize;

    /**
     * Cosntructor.
     *
     * @param resultRows the query result that contains the rows presented by this document
     * @param startIndex the start index of the rows presented by this document
     * @param endIndex the last index of the rows presented by this document
     * @param sourceDocument the source document the results are based on
     * @param kwicSize the current keyword in context span size
     */
    public SelectionResultDocument(
            List<QueryResultRow> resultRows,
            int startIndex,
            int endIndex,
            SourceDocument sourceDocument, int kwicSize) {
        this.resultRows = resultRows;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.sourceDocument = sourceDocument;
        setKwicSize(kwicSize);
    }

    /**
     * Constructor.
     *
     * @param queryResultRows the query result presented by this document
     * @param sourceDocument the source document the results are based on
     * @param kwicSize the current keyword in context span size
     */
    public SelectionResultDocument(
            List<QueryResultRow> queryResultRows, SourceDocument sourceDocument, int kwicSize) {
        this(
            queryResultRows,
            0, queryResultRows.get(queryResultRows.size()-1).getRangeList().size()-1,
            sourceDocument, kwicSize);
    }

    /**
     * @param kwicSize the new keyword in context span size
     */
    public void setKwicSize(int kwicSize) {
        if (this.kwicSize != kwicSize) {
            this.kwicSize = kwicSize;
            try {
                computeDocument();
            }
            catch(IOException io) {
                ExceptionHandler.log(io);
            }
        }
    }

    /**
     * @return the content of this document (all types with their tokens in kwic style)
     */
    public String getContent() {
        return content;
    }

    /**
     * Computes this document (all the sections with their entries).
     *
     * @throws IOException problems accessing the {@link org.catma.indexer.Index} of
     * the {@link org.catma.document.source.SourceDocument}
     */
    private void computeDocument() throws IOException {

        documentSectionList = new ArrayList<DocumentSection>();
        StringBuilder contentBuilder = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator");

        for (int idx=0; idx<resultRows.size(); idx++) {
            QueryResultRow row = resultRows.get(idx);
            handleRow(row, contentBuilder, lineSeparator,
                    (idx==0)?startIndex : 0,
                    (idx==resultRows.size()-1)?endIndex : row.getRangeList().size()-1);
        }

        content = contentBuilder.toString();
    }

    /**
     * Creates one {@link org.catma.queryengine.result.DocumentSection} for the given row.
     * @param resultRow the row we are creating the document section for
     * @param contentBuilder the target where to store the created section
     * @param lineSeparator the current line separator
     * @param start the start index of the entries to take into account for the creation
     * @param end the end index of the entries to take into account for the creation
     * @throws IOException problems accessing the {@link org.catma.indexer.Index} of
     * the {@link org.catma.document.source.SourceDocument}
     */
    public void handleRow(
            QueryResultRow resultRow, StringBuilder contentBuilder,
            String lineSeparator, int start, int end)
            throws IOException {
        
        int sectionStart = contentBuilder.length();

        // leave space to the previous section
        if (documentSectionList.size() != 0) {
            contentBuilder.append(lineSeparator);
            contentBuilder.append(lineSeparator);
            contentBuilder.append(lineSeparator);
        }

        // add section header
        int headerStart = contentBuilder.length();
        contentBuilder.append(resultRow.getTextAsSingleLineWithEllipsis());
        contentBuilder.append("(");
        contentBuilder.append(resultRow.getTermInfoList().size());
        contentBuilder.append(")");

        // create section

        DocumentSection section =
            new DocumentSection(
                resultRow,
                new Range(headerStart, contentBuilder.length()));


        // add section entries
        for ( int idx=start; idx<=end; idx++ ) {

            Range range = resultRow.getRangeList().get(idx);

            KeywordInContext kwic = sourceDocument.getKWIC(range, kwicSize);
            int entryStart = contentBuilder.length();

            // add entry info
            contentBuilder.append(lineSeparator);
            contentBuilder.append(lineSeparator);
            contentBuilder.append(range.getStartPoint());
            contentBuilder.append(" - ");
            contentBuilder.append(range.getEndPoint());
            contentBuilder.append(":");
            contentBuilder.append(lineSeparator);

            // add entry
            SectionEntry entry =
                new SectionEntry(
                    new Range(entryStart, contentBuilder.length()+kwic.toString().length()),
                    range,
                    kwic,
                    new Range(
                        contentBuilder.length(),
                        contentBuilder.length()+kwic.toString().length()));
            contentBuilder.append(kwic);
            section.addSectionEntry(entry);
        }

        section.setSectionRange(
                new Range(sectionStart, contentBuilder.length()));
        documentSectionList.add(section);

    }

    public Range convertFromSourceDocumentRangeRelativeToRange(
            Range sourceRange, Range resultDocRange) {

        // loop over the sections
        for (DocumentSection ds : this.documentSectionList) {
            // try to find the given range within the section
            if(resultDocRange.hasOverlappingRange(ds.getSectionRange())) {

                // try to locate the specific entry that has the overlap with the given range
                for (SectionEntry se : ds.getSectionEntryList()) {
                    long convertedStartPoint =
                        se.getKwicResultDocRange().getStartPoint()
                            + (sourceRange.getStartPoint()-
                                se.getKwicSourceRange().getStartPoint());
                    long convertedEndPoint =
                            convertedStartPoint + sourceRange.getSize();

                    Range overlappingRange =
                            se.getKwicResultDocRange().getOverlappingRange(
                                    new Range(convertedStartPoint, convertedEndPoint));

                    if (overlappingRange != null) {
                        return overlappingRange;
                    }
                }
            }
        }

        return null;
    }

    public Range convertToSourceDocumentRange(Range resultDocRange) {
        // loop over the sections
        for (DocumentSection ds : this.documentSectionList) {
            // try to find the given range within the section
            if(resultDocRange.hasOverlappingRange(ds.getSectionRange())) {
                // try to find the overlapping entry
                for (SectionEntry se : ds.getSectionEntryList()) {
                    Range overlappingRange =
                            se.getKwicResultDocRange().getOverlappingRange(resultDocRange);

                    // convert the range with the entry we found
                    if (overlappingRange != null) {
                        long convertedStartPoint =
                                se.getKwicSourceRange().getStartPoint()
                                        + (overlappingRange.getStartPoint()-
                                                se.getKwicResultDocRange().getStartPoint());
                        long convertedEndPoint =
                                convertedStartPoint + overlappingRange.getSize();

                        return new Range(convertedStartPoint, convertedEndPoint);
                    }
                }
            }
        }

        return null;
    }

    public List<Range> getKeywordRangeList() {
        List<Range> result = new ArrayList<Range>();
        
        for (DocumentSection ds : this.documentSectionList) {
            for (SectionEntry se : ds.getSectionEntryList()) {
                result.add(se.getKeywordResultDocRange());
            }
        }

        return result;
    }

    /**
     * @return a list of all the entries of all sections
     */
    public List<SectionEntry> getSectionEntryList() {
        List<SectionEntry> result = new ArrayList<SectionEntry>();

        for (DocumentSection ds : this.documentSectionList) {
            for (SectionEntry se : ds.getSectionEntryList()) {
                result.add(se);
            }
        }

        return result;
    }

    public List<Range> getHeaderRangeList() {
        List<Range> result = new ArrayList<Range>();

        for (DocumentSection ds : this.documentSectionList) {
            result.add(ds.getHeaderRange());
        }
        
        return result;
    }

    public Pair<QueryResultRow,Range> getQueryResultRowRange(Range resultDocumentRange) {
        for (DocumentSection ds : this.documentSectionList) {
            if(resultDocumentRange.isInBetween(ds.getSectionRange())) {
                for (SectionEntry se : ds.getSectionEntryList()) {
                    if(resultDocumentRange.isInBetween(se.getSectionEntryRange())) {
                        return new Pair<QueryResultRow,Range>(
                                ds.getResultRow(), se.getKeywordSourceRange());
                    }
                }
                return new Pair<QueryResultRow,Range>(
                               ds.getResultRow(), ds.getSectionRange());
            }
        }

        return null;
    }

    public Range getResultDocRangeFor(QueryResultRow queryResultRow, Range range) {
        for (DocumentSection ds : this.documentSectionList) {
            if (queryResultRow.equals(ds.getResultRow())) {
                for (SectionEntry se : ds.getSectionEntryList()) {
                    if (se.getKeywordSourceRange().equals(range)) {
                        return se.getKeywordResultDocRange();
                    }
                }
                return ds.getSectionRange();
            }
        }

        return null;
    }
}
