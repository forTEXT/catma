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

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.queryengine.result.CollocateComputationResult;
import de.catma.queryengine.result.DistributionComputationResult;
import de.catma.queryengine.result.QueryResult;

/**
 * A compount job the combines a {@link org.catma.queryengine.computation.DistributionComputationJob distribution analysis}
 * and a {@link org.catma.queryengine.computation.CollocateComputationJob}.
 *
 * @author Marco Petris
 *
 */
public class CompoundComputationJob extends DefaultProgressCallable<CompoundComputationResult> {

    private QueryResult computationInput;
    private DistributionComputationJob distributionComputationJob;
    private CollocateComputationJob collocateComputationJob;

    /**
     * Constructor.
     * @param computationInput the result of the query this analyis is based on
     * @param distributionComputationJob distribution analysis
     * @param collocateComputationJob collocation analysis
     */
    public CompoundComputationJob(
            QueryResult computationInput,
            DistributionComputationJob distributionComputationJob,
            CollocateComputationJob collocateComputationJob) {
        this.computationInput = computationInput;
        this.distributionComputationJob = distributionComputationJob;
        this.collocateComputationJob = collocateComputationJob;
    }

    public CompoundComputationResult call() throws Exception {
        distributionComputationJob.setProgressListener(getProgressListener());
        DistributionComputationResult distributionComputationResult =
                distributionComputationJob.call();
        collocateComputationJob.setProgressListener(getProgressListener());
        CollocateComputationResult collocateComputationResult =
                collocateComputationJob.call();
        return new CompoundComputationResult(
            distributionComputationResult, collocateComputationResult, computationInput);
    }
}
