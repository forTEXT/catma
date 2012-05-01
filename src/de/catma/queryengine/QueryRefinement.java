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

package de.catma.queryengine;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import de.catma.indexer.TermInfo;
import de.catma.queryengine.result.QueryResult;

/**
 * A refinement that is specified via a {@link org.catma.queryengine.Query}.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class QueryRefinement implements Refinement {

    private Query query;

    /**
     * Constructor.
     *
     * @param query the definition of this refinement
     */
    public QueryRefinement(Query query) {
        this.query = query;
    }

    public QueryResult refine(QueryResult result) throws Exception {

//        List<TermInfo> termInfoList = query.getResult().getTermInfoList();
//
//        Comparator<TermInfo> comparator = query.getComparator();
//        
//        // do we have a special comparator or do we compare by equal?
//        if (comparator == null) {
//            result.getTermInfoList().retainAll(termInfoList);
//        }
//        else {
//            Iterator<TermInfo> baseResultIterator = result.getTermInfoList().iterator();
//            while (baseResultIterator.hasNext()) {
//                TermInfo curInfo = baseResultIterator.next();
//                if(!hasMatch(curInfo,termInfoList,comparator)) {
//                    baseResultIterator.remove();
//                }
//            }
//        }
//
//        return result;
    	return null;
    }

    /**
     * Tests whether the given token has an equal token in the list.
     * @param curInfo the token to test
     * @param termInfoList the list to check against
     * @param comparator the comparator to use for the test
     * @return <code>true</code> if there is an equal token in the list 
     */
    private boolean hasMatch(
            TermInfo curInfo, List<TermInfo> termInfoList, Comparator<TermInfo> comparator) {

        for (TermInfo comp : termInfoList) {
            if((comparator.compare(curInfo,comp)==0)||(comparator.compare(comp,curInfo)==0)) {
                return true;
            }
        }

        return false;
    }
}
