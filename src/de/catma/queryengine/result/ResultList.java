/*
 *    CATMA Computer Aided Text Markup and Analysis
 * 
 *    Copyright (C) 2009  University Of Hamburg
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
import java.util.Set;

import de.catma.indexer.TermInfo;

/**
 * A list of tokens provided by a {@link org.catma.queryengine.Query}.
 *
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class ResultList {

    private List<TermInfo> termInfoList;

    /**
     * Constructor of an empty list.
     */
    public ResultList() {
        this.termInfoList = new ArrayList<TermInfo>();
    }

    /**
     * Constructor.
     * @param termInfoSet a set of tokens
     */
    public ResultList(Set<TermInfo> termInfoSet) {
        this();
        this.termInfoList.addAll(termInfoSet);
    }

    /**
     * Constructor
     * @param termInfoList a list of tokens
     */
    public ResultList(List<TermInfo> termInfoList) {
        this.termInfoList = termInfoList;
    }

    /**
     * Copy Constructor. A shallow copy is created, only the list is copied not the token elements itself.
     * @param result the result to copy
     */
    public ResultList(ResultList result) {
        this();
        termInfoList.addAll(result.getTermInfoList());
    }

    /**
     * @return the list of tokens of this result
     */
    public List<TermInfo> getTermInfoList() {
        return termInfoList;
    }

}
