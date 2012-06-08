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

package de.catma.queryengine.computation;

import de.catma.queryengine.result.CollocateComputationResult;
import de.catma.queryengine.result.DistributionComputationResult;
import de.catma.queryengine.result.QueryResult;

/**
 * A container for three results:
 * <ul>
 * <li>a {@link org.catma.queryengine.result.QueryResult}</li>
 * <li>a {@link org.catma.queryengine.result.DistributionComputationResult}</li>
 * <li>a {@link org.catma.queryengine.result.CollocateComputationResult}</li>
 * </ul>
 * 
 *
 * @author Marco Petris
 *
 */
public class CompoundComputationResult {
    private DistributionComputationResult distributionComputationResult;
    private CollocateComputationResult collocateComputationResult;
    private QueryResult queryResult;

    /**
     * Constructor
     * @param distributionComputationResult the result of the distribution analysis
     * @param collocateComputationResult the result of the collocation analysis
     * @param queryResult the result of the query
     */
    public CompoundComputationResult(
            DistributionComputationResult distributionComputationResult,
            CollocateComputationResult collocateComputationResult,
            QueryResult queryResult) {
        this.distributionComputationResult = distributionComputationResult;
        this.collocateComputationResult = collocateComputationResult;
        this.queryResult = queryResult;
    }

    /**
     * @return the result of the distribution analysis
     */
    public DistributionComputationResult getDistributionComputationResult() {
        return distributionComputationResult;
    }

    /**
     * @return the result of the collocation analysis
     */
    public CollocateComputationResult getCollocateComputationResult() {
        return collocateComputationResult;
    }

    /**
     * @return the result of the query
     */
    public QueryResult getQueryResult() {
        return queryResult;
    }
}
