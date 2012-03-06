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

import org.jfree.data.xy.XYSeriesCollection;


/**
 * A computation result from a {@link org.catma.queryengine.computation.DistributionComputationJob}.
 *
 * @author Malte Meister
 *
 */
public class DistributionComputationResult implements ComputationResult {
    private XYSeriesCollection resultDataCollection;
    private QueryResult computationInput;

    /**
     * Constructor.
     * @param resultDataCollection the data collection for the distribution graph
     * @param computationInput the query result
     */
    public DistributionComputationResult(XYSeriesCollection resultDataCollection, QueryResult computationInput) {
        this.resultDataCollection = resultDataCollection;
        this.computationInput = computationInput;
    }

    /**
     * @return the data collection for the distribution graph
     */
    public XYSeriesCollection getXySeriesCollection() {
        if (resultDataCollection == null) {
            return new XYSeriesCollection();
        }

        return resultDataCollection;
    }

    public int getTypeCount() {
        return getXySeriesCollection().getSeriesCount();
    }

    public QueryResult getComputationInput() {
        if (computationInput == null) {
            return new QueryResultRowList();
        }

        return computationInput;
    }
}
