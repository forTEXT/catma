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
import de.catma.indexer.TermInfo;

/**
 * A row of a {@link org.catma.queryengine.result.QueryResult}.
 *
 *
 * @author Marco Petris
 *
 */
public class QueryResultRow implements Comparable<QueryResultRow> {

    private static final int ELLIPSIS_MAX_LENGTH = 100;

    private String text;
    private List<TermInfo> termInfoList;
    private List<Range> rangeList;
    private boolean selected;
    private String singleLineWithEllipsis;

    /**
     * Constructor.
     *
     * @param text the 'type' of this row
     * @param termInfoList  the list of tokens for the type
     */
    public QueryResultRow(String text, List<TermInfo> termInfoList) {
        this.text = text;
        this.termInfoList = termInfoList;
        this.selected = false;
        if (text.length() > ELLIPSIS_MAX_LENGTH) {
            singleLineWithEllipsis =
                    text.substring(
                        0, ELLIPSIS_MAX_LENGTH/2).replaceAll(
                            "\\r\\n", " ").replaceAll(
                                "[^\\r]\\n", " ")
                    + " [...] "
                    + text.substring(
                        text.length()-ELLIPSIS_MAX_LENGTH/2).replaceAll(
                            "\\r\\n", " ").replaceAll(
                                "[^\\r]\\n", " ");
        }
        else {
            singleLineWithEllipsis = text;
        }
    }

    /**
     * @return the type
     */
    public String getText() {
        return text;
    }

    /**
     * @return the type as single line without linebreaks and with an ellipsis if the length of the type exceeds the
     * maximum length
     */
    public String getTextAsSingleLineWithEllipsis() {
        return singleLineWithEllipsis;
    }

    /**
     * @return the number of tokens
     */
    public int getTokenCount() {
        return getTermInfoList().size();
    }

    /**
     * @return the list of tokens
     */
    public List<TermInfo> getTermInfoList() {
        return termInfoList;
    }

    /**
     * @return the list of the Ranges of the tokens
     */
    public List<Range> getRangeList() {
        if (rangeList==null) {
            rangeList = new ArrayList<Range>();
        
            for (TermInfo ti : termInfoList) {
                rangeList.add(ti.getRange());
            }
        }
        return rangeList;
    }

    /**
     * @return true if this row has been marked selected
     * @see #setSelected(boolean)
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * @param selected the new selected state of this row
     * @see #isSelected()
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * @param rangeList the new list of tokens
     */
    protected void setTermInfoList(List<TermInfo> rangeList) {
        this.termInfoList = rangeList;
        this.rangeList = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryResultRow that = (QueryResultRow) o;

        if (termInfoList != null ? !termInfoList.equals(that.termInfoList) : that.termInfoList != null) return false;
        if (!text.equals(that.text)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    /**
     * Rows are compared by their {@link #getText() type}.
     * @param o the row to compare this row to
     * @return see {@link Comparable#compareTo(Object)}
     */
    public int compareTo(QueryResultRow o) {
        return this.text.compareTo(o.getText());
    }

    @Override
    public String toString() {
        return getTextAsSingleLineWithEllipsis();
    }
}
