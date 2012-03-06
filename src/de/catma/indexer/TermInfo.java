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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.catma.indexer;

import de.catma.core.document.Range;

/**
 * A term info represents a token, i. e. a type occurrrence.
 * <br><br>
 * <b>Note:<b/>A TermInfo with Tag information as constructed by this
 * {@link TermInfo#TermInfo(String, org.catma.document.Range, org.catma.tag.Tag) TermInfo}-
 * constructor has the consequence that two TermInfos with identical range and term
 * but different tags are NOT equal!
 *
 * @author Marco Petris
 */
public class TermInfo {
    private String term;
    private Range range;
    private Tag tag = null;

    /**
     * Constructor.
     * @param term the type of this token
     * @param offset the start offset
     */
    public TermInfo(char term, int offset) {
        this( String.valueOf(term), offset, offset+1);
    }

    /**
     * Constructor.
     * @param term the type of this token
     * @param startOffset the start offset of the token, will be parsed to integer
     * @param endOffset the endoffset of the token, will be parsed to integer
     */
    public TermInfo(String term, String startOffset, String endOffset) {
        this( term, 
                Integer.parseInt(startOffset),
                Integer.parseInt(endOffset));
    }

    /**
     * Constructor.
     * @param term the type of this token
     * @param startOffset the start offset of the token
     * @param endOffset the endoffset of the token
     */
    public TermInfo(String term, int startOffset, int endOffset) {
        this(term, new Range(startOffset, endOffset));
    }

    /**
     * Constructor.
     * @param term the type of this token
     * @param range the range of the token
     */
    public TermInfo(String term, Range range) {
        this(term, range, null);
    }

    /**
     * Constructor. <br><br> <b>Note:<b/> A TermInfo with Tag information as constructed by this
     * constructor has the consequence that two TermInfos with identical range and term
     * but different tags are NOT equal!
     *
     * @param term the type of this token
     * @param range the range of the token
     * @param tag a Tag that is present at this token (this is optional), can be <code>null</code>
     * @throws IllegalArgumentException if the term argument is <code>null</code>
     */
    public TermInfo(String term, Range range, Tag tag) {
        if (term == null) {
            throw new IllegalArgumentException("argument term cannot be null!");
        }

        this.term = term;
        this.range = range;
        this.tag = tag;
    }

    /**
     * @return the range of this token
     */
    public Range getRange() {
        return range;
    }

    /**
     * @return the type of this token
     */
    public String getTerm() {
        return term;
    }

    @Override
    public String toString() {
        return getTerm() + " " + getRange();
    }

    /**
     * @return a Tag that is present at this token, may be <code>null</code> if the Tag has not been set
     */
    public Tag getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TermInfo termInfo = (TermInfo) o;

        if (!range.equals(termInfo.range)) {
            return false;
        }

        // the inclusion of the tag property is important:
        // UnionQuery would otherwise combine terms that are marked with
        // the two tags of two TagQueries which can be combined by the UnionQuery
        // this would result in wrong tag distribution graphs!
        if (tag != null ? !tag.equals(termInfo.tag) : termInfo.tag != null) {
            return false;
        }

        if (!term.equals(termInfo.term)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = term.hashCode();
        result = 31 * result + range.hashCode();
        result = 31 * result + (tag != null ? tag.hashCode() : 0);
        return result;
    }
}
