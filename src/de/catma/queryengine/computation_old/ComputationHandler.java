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

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.queryengine.SpanDirection;
import de.catma.queryengine.result.CollocateComputationResult;
import de.catma.queryengine.result.DistributionComputationResult;
import de.catma.queryengine.result.QueryResult;

/**
 * Delegats analysis computation to the {@link org.catma.backgroundservice.BackgroundService}.
 *
 *
 * @author Malte Meister
 *
 */
public class ComputationHandler {
    /**
     * Constructor.
     */
    public ComputationHandler() {
    }

    /**
     * Executes a distribution analysis in the background.
     * @param computationInput The selection for which to compute distribution info
     * @param textSegmentSize The % size of chunks to split the text into for the distribution chart
     *                        (must multiply to equal 100%, max. value is 50%)
     * @param jobName displayname of the job (may be used by progress listener)
     * @param execListener this listener will be notified once the computation has completed
     */
    public void executeDistributionComputation(
            QueryResult computationInput, int textSegmentSize, String jobName,
            ExecutionListener<DistributionComputationResult> execListener) {
        BackgroundService.SINGLETON.submit(
                new DistributionComputationJob(computationInput, textSegmentSize, jobName), execListener);
    }

    /**
     * Executes a collocation analysis in the background.
     * @param computationInput the result of the query this collocation analyis is based on
     * @param spanContextSize the current span context size for the collocation
     * @param jobName the name of the job (to be displayed to the user during job execution)
     * @param execListener this listener will be notified once the computation has completed
     */
    public void executeCollocateComputation(
            QueryResult computationInput, int spanContextSize, String jobName,
            ExecutionListener<CollocateComputationResult> execListener) {
        BackgroundService.SINGLETON.submit(
                new CollocateComputationJob(
                        computationInput, spanContextSize, SpanDirection.Both, jobName), execListener);
    }

    /**
     * Executes a collocation analysis and a distribution analysis, both in the background
     * @param computationInput the result of the query this computation is based on
     * @param textSegmentSize The % size of chunks to split the text into for the distribution chart
     *                        (must multiply to equal 100%, max. value is 50%)
     * @param jobName1 displayname of the distribution computation job (may be used by progress listener)
     * @param spanContextSize the current span context size for the collocation
     * @param jobName2 displayname of the collocation computation job (may be used by progress listener)
     * @param execListener this listener will be notified once the computation has completed
     */
    public void executeCompoundComputation(
            QueryResult computationInput, int textSegmentSize, String jobName1,
            int spanContextSize, String jobName2,
            ExecutionListener<CompoundComputationResult> execListener) {
        BackgroundService.SINGLETON.submit(
                new CompoundComputationJob(
                        computationInput,
                        new DistributionComputationJob(computationInput, textSegmentSize, jobName1),
                        new CollocateComputationJob(
                        computationInput, spanContextSize, SpanDirection.Both, jobName2)
                        ), execListener);
    }

}
