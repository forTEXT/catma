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


/**
 * A computation result from a {@link org.catma.queryengine.computation.CollocateComputationJob}.
 *
 * @author Malte Meister
 *
 */
public class CollocateComputationResult implements ComputationResult {
    private QueryResult computationInput;
    private List<CollocateComputationResultRow> resultRowList;

    /**
     * Constructor.
     * @param computationInput the query result
     * @param resultRowList the rows of this collocation computation result
     */
    public CollocateComputationResult(QueryResult computationInput, List<CollocateComputationResultRow> resultRowList) {
        this.computationInput = computationInput;
        this.resultRowList = resultRowList;
    }

    /**
     * @return the rows of this collocation computation result
     */
    public List<CollocateComputationResultRow> getResultRowList() {
        if (resultRowList == null) {
            return new ArrayList<CollocateComputationResultRow>();
        }

        return resultRowList;
    }

    public int getTypeCount() {
        return getResultRowList().size();
    }

    public QueryResult getComputationInput() {
        if (computationInput == null) {
            return new QueryResultRowList();
        }

        return computationInput;
    }
}
