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
import java.util.List;

import de.catma.core.document.Range;

/**
 * A section of a {@link org.catma.queryengine.result.SelectionResultDocument}. A section represents
 * one type and its tokens of a {@link org.catma.queryengine.result.QueryResult}. That means each type
 * has its own document section.
 *
 * @author Marco Petris
 *
 */
public class DocumentSection {
    private QueryResultRow resultRow;
    private Range sectionRange;
    private Range headerRange;
    private List<SectionEntry> sectionEntryList;

    /**
     * Constructor.
     * @param resultRow the row containing the type and its tokens.
     * @param headerRange the header range of this section
     * in the {@link org.catma.queryengine.result.SelectionResultDocument}
     */
    public DocumentSection(QueryResultRow resultRow, Range headerRange) {
        this.resultRow = resultRow;
        this.headerRange = headerRange;
        this.sectionEntryList = new ArrayList<SectionEntry>();
    }

    /**
     * @param sectionRange the range of this section in the {@link org.catma.queryengine.result.SelectionResultDocument}
     */
    public void setSectionRange(Range sectionRange) {
        this.sectionRange = sectionRange;
    }

    /**
     * @param entry the entry to add
     */
    public void addSectionEntry(SectionEntry entry) {
        sectionEntryList.add(entry);
    }

    /**
     * @return the list of entries of this section
     */
    public List<SectionEntry> getSectionEntryList() {
        return sectionEntryList;
    }

    /**
     * @return the range of this section in the {@link org.catma.queryengine.result.SelectionResultDocument}
     */
    public Range getSectionRange() {
        return sectionRange;
    }

    /**
     * @return the row containing the type and its tokens of this section
     */
    public QueryResultRow getResultRow() {
        return resultRow;
    }

    /**
     * @return the header range of this section in the {@link org.catma.queryengine.result.SelectionResultDocument} 
     */
    public Range getHeaderRange() {
        return headerRange;
    }
}
