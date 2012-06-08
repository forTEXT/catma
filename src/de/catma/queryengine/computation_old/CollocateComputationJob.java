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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.core.document.Range;
import de.catma.core.document.source.SourceDocument;
import de.catma.indexer.Index;
import de.catma.indexer.SpanContext;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.SpanDirection;
import de.catma.queryengine.result.CollocateComputationResult;
import de.catma.queryengine.result.CollocateComputationResultRow;
import de.catma.queryengine.result.QueryResult;

/**
 * A job that computes a collocation analysis.
 *
 * @author Malte Meister
 *
 */
public class CollocateComputationJob extends DefaultProgressCallable<CollocateComputationResult> {
    private QueryResult computationInput;
    private int spanContextSize;
    private SpanDirection direction;
    private String jobName;

    /**
     * Constructor.
     *
     * @param computationInput the result of the query this collocation analyis is based on
     * @param spanContextSize the current span context size for the collocation
     * @param direction the direction of the span
     * @param jobName the name of the job (to be displayed to the user during job execution)
     */
    public CollocateComputationJob(QueryResult computationInput, int spanContextSize, SpanDirection direction,
                                   String jobName) {
        this.computationInput = computationInput;
        this.spanContextSize = spanContextSize;
        this.direction = direction;
        this.jobName = jobName;
    }

    public CollocateComputationResult call() throws Exception {
        getProgressListener().setIndeterminate(true, jobName);

        final SourceDocument sourceDoc = FileManager.SINGLETON.getCurrentSourceDocument();
        final Index index = sourceDoc.getIndex();
        final List<TermInfo> selectedTermsList = computationInput.getSortedTermInfoList();
        final int totalTokenCount = index.getTotalTokenCount();
        int miniTextTokenCount = 0;

        // build a mapping: type->collocating token range

        HashMap<String, List<Range>> typeToCollocTokenRangeMap = new HashMap<String, List<Range>>();

        for (TermInfo ti : selectedTermsList) {
            SpanContext spanContext = index.getSpanContextFor(
                    sourceDoc.getContent(), ti.getRange(),
                    spanContextSize, direction);

            List<TermInfo> collocList = new ArrayList<TermInfo>();
            collocList.addAll(spanContext.getBackwardTokens());
            collocList.addAll(spanContext.getForwardTokens());

            miniTextTokenCount += collocList.size();

            for (TermInfo colloc : collocList) {
                if (!typeToCollocTokenRangeMap.containsKey(colloc.getTerm())) {
                    typeToCollocTokenRangeMap.put(colloc.getTerm(), new ArrayList<Range>());
                }

                typeToCollocTokenRangeMap.get(colloc.getTerm()).add(colloc.getRange());
            }
        }

        List<CollocateComputationResultRow> resultRowList = new ArrayList<CollocateComputationResultRow>();

        // compute z-score for each type
        for (Map.Entry<String, List<Range>> entry : typeToCollocTokenRangeMap.entrySet()) {
            int termCollocCount = entry.getValue().size();

            if (termCollocCount > 0) {
                int termTotalTokenCount = index.search(entry.getKey()).size();
                double probability = (double)termTotalTokenCount / (double)totalTokenCount;
                double expectedFreq = probability * (double)miniTextTokenCount;
                double stdDeviation = Math.sqrt(expectedFreq * (1.0 - probability));
                double zScore = ((double)termCollocCount - expectedFreq) / stdDeviation;

                resultRowList.add(new CollocateComputationResultRow(
                        entry.getKey(), entry.getValue(), termTotalTokenCount, zScore
                ));
            }
        }
        
        getProgressListener().setIndeterminate(false, jobName);

        return new CollocateComputationResult(computationInput, resultRowList);
    }
}
