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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.result.DistributionComputationResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.core.document.Range;


/**
 * A job that computes a distribution analysis.
 *
 *
 * @author Malte Meister, (Marco Petris: some comments and optimization for Tag-based queries)
 *
 */
public class DistributionComputationJob extends DefaultProgressCallable<DistributionComputationResult> {

    private static final int MAX_PERSEGMENTSIZE = 50;

    private QueryResult computationInput;
    private int perSegmentSize;
    private String jobName;

    /**
	 * Constructor.
	 * @param computationInput The selection for which to compute distribution info
     * @param perSegmentSize The % size of chunks to split the text into for the distribution chart
     *                        (must multiply to equal 100%, max. value is 50%)
     * @param jobName displayname of the job (may be used by progress listener)
	 */
    public DistributionComputationJob(
            QueryResult computationInput, int perSegmentSize, String jobName) {
        this.computationInput = computationInput;
        this.jobName = jobName;

        if ((100 % perSegmentSize != 0) || (perSegmentSize > MAX_PERSEGMENTSIZE)) {
            throw new IllegalArgumentException("perSegmentSize");
        }
        else {
            this.perSegmentSize = perSegmentSize;
        }
    }

    public DistributionComputationResult call() throws Exception {

        if (getProgressListener() != null) {
            getProgressListener().setIndeterminate(true, jobName);
        }

        try {
            final long textSize = FileManager.SINGLETON.getCurrentSourceDocument().getSize();
            final double segmentSize = perSegmentSize / 100.0 * textSize;
            final double noOfSegments = textSize / segmentSize;


            return computeResult(computationInput, segmentSize, noOfSegments);

        }
        finally {
            if (getProgressListener() != null) {
                getProgressListener().setIndeterminate(false, jobName);
            }
        }
    }

    /**
     * Does the actual compution.
     *
     * @param computationInput The selection for which to compute distribution info
     * @param segmentSize the segment size
     * @param noOfSegments number of segments
     * @return the computation result
     */
    private DistributionComputationResult computeResult(
            QueryResult computationInput, double segmentSize, double noOfSegments) {

        XYSeriesCollection dataset = new XYSeriesCollection();

        // compute token count per segment for non Tag-based results
        // and create a dataset for a distribution graph
        // the series are based on result rows here
        for (QueryResultRow row : computationInput) {
            XYSeries series = new XYSeries(new DistributionXYSeriesRowKey(row));

            for (int i = 1; i <= noOfSegments; i++) {
                double segmentStart = (i - 1) * segmentSize;
                double segmentEnd = i * segmentSize;
                List<Range> rangeList = new ArrayList<Range>();
                for (TermInfo ti : row.getTermInfoList()) {
                    // is this result based on a Tag query?
                    if (ti.getTag() == null) {
                        // no, we process it here
                        rangeList.add(ti.getRange());
                    }
                }
                if (rangeList.size() > 0) {
                    int segmentTokenCount = getSegmentTokenCount(
                            rangeList, segmentStart, segmentEnd);

                    series.add(i * perSegmentSize, segmentTokenCount);
                }
            }

            if (series.getItemCount() > 0) {
                dataset.addSeries(series);
            }
        }


        // create per-Tag-mappings

        HashMap<Tag,List<Range>> tagRanges = new HashMap<Tag, List<Range>>();
        HashMap<Tag,List<QueryResultRow>> tagRows = new HashMap<Tag, List<QueryResultRow>>();

        for (QueryResultRow row : computationInput) {
            for (TermInfo ti : row.getTermInfoList()) {
                if (ti.getTag() != null) {
                    if (!tagRanges.containsKey(ti.getTag())) {
                        tagRanges.put(ti.getTag(), new ArrayList<Range>());
                    }
                    tagRanges.get(ti.getTag()).add(ti.getRange());

                    if (!tagRows.containsKey(ti.getTag())) {
                        tagRows.put(ti.getTag(), new ArrayList<QueryResultRow>());
                    }

                    if (!tagRows.get(ti.getTag()).contains(row)) {
                        tagRows.get(ti.getTag()).add(row);
                    }
                }
            }
       }

        // loop over the tags and build the data for a distribution graph for each Tag
        // the series are based on results for Tags not rows!
        for (Map.Entry<Tag,List<Range>> entry : tagRanges.entrySet()) {
            XYSeries series = new XYSeries(
                    new DistributionXYSeriesTagKey(
                            entry.getKey(),
                            tagRows.get(entry.getKey()),
                            entry.getValue()));

            for (int i = 1; i <= noOfSegments; i++) {
                double segmentStart = (i - 1) * segmentSize;
                double segmentEnd = i * segmentSize;

                int segmentTokenCount = getSegmentTokenCount(
                        entry.getValue(), segmentStart, segmentEnd);

                series.add(i * perSegmentSize, segmentTokenCount);
            }

            dataset.addSeries(series);
        }


        return new DistributionComputationResult(dataset, computationInput);
    }


    /**
     * Determines how many times a token appears within a text segment. Note that only the token's start point
     * is taken into consideration.
     * @param rangeList The list of ranges for the token to check.
     * @param segmentStart The start point of the text segment.
     * @param segmentEnd The end point of the text segment.
     * @return The number of times the token appears within the segment.
     */
    private int getSegmentTokenCount(List<Range> rangeList, double segmentStart, double segmentEnd) {
        int count = 0;
        Collections.sort(rangeList);

        //TODO: this does not work well with long tagged passages 
        for (Range r : rangeList) {
            // Essentially the same as 'if (r.getStartPoint() >= segmentStart && r.getStartPoint() <= segmentEnd)'
            // In this way, because the range list is sorted, we don't have any unnecessary iterations/conditionals
            if (r.getStartPoint() < segmentStart) {
                continue;
            }
            if (r.getStartPoint() > segmentEnd) {
                break;
            }

            count++;
        }

        return count;
    }
}
