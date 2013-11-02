/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.queryengine.result.computation;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResultRow;

public class DistributionComputation {
	
	private GroupedQueryResultSet groupedQueryResultSet;
	private Repository repository;
	private List<String> relevantSourceDocumentIDs;
	private int segmentSizeInPercent = 10;
	private double segmentSize;
	private int numberOfSegments;
	private List<XYValues<Integer, Integer>> xySeriesCollection;
	private Set<PlotBand> plotBands;
	private HashMap<String, Long> documentSegmentOffsets;
	private long totalSize = 0;
	
	public DistributionComputation(GroupedQueryResultSet groupedQueryResultSet,
			Repository repository, List<String> relevantSourceDocumentIDs) throws IOException {
		this.groupedQueryResultSet = groupedQueryResultSet;
		this.repository = repository;
		this.relevantSourceDocumentIDs = relevantSourceDocumentIDs;
		this.xySeriesCollection = new ArrayList<XYValues<Integer,Integer>>();
		plotBands = new HashSet<PlotBand>();
		documentSegmentOffsets = new HashMap<String, Long>();
		
		computeSegments();

	}

	private void computeSegments() throws IOException {
		Set<SourceDocument> toBeUnloaded = new HashSet<SourceDocument>();
		for (String sourceDocId : relevantSourceDocumentIDs) {
			SourceDocument sd = repository.getSourceDocument(sourceDocId);
			if (!sd.isLoaded()) {
				toBeUnloaded.add(sd);
			}
			documentSegmentOffsets.put(sourceDocId, totalSize);
			
			totalSize += sd.getLength();
		}
		
		for (String sourceDocId : relevantSourceDocumentIDs) {
			SourceDocument sd = repository.getSourceDocument(sourceDocId);		
			plotBands.add(
				new PlotBand(
					sd.getID(),
					sd.toString(),
					100.0/totalSize*documentSegmentOffsets.get(sourceDocId),
					100.0/totalSize*
						(sd.getLength()+documentSegmentOffsets.get(sourceDocId))));
		
		}
		for (SourceDocument sd : toBeUnloaded) {
			sd.unload();
		}
		
		segmentSize = segmentSizeInPercent / 100.0 * totalSize;
		numberOfSegments = 
			BigDecimal.valueOf(totalSize).divide(
				BigDecimal.valueOf(segmentSize), BigDecimal.ROUND_UP).intValue();
	}


	public void compute() {
		for (GroupedQueryResult groupedQueryResult : groupedQueryResultSet.asGroupedSet()) {
			compute(groupedQueryResult);
		}
	}


	private void compute(GroupedQueryResult groupedQueryResult) {
		Object group = groupedQueryResult.getGroup();
		XYValues<Integer, Integer> xySeries = 
				new XYValues<Integer, Integer>(group);
		
		for (int i=1; i<=numberOfSegments; i++) {
			xySeries.set((i*segmentSizeInPercent)-(segmentSizeInPercent/2), 0);
		}
		for (QueryResultRow row : groupedQueryResult) {
			int segmentNo =
				getSegment(
					row.getRange(), 
					documentSegmentOffsets.get(row.getSourceDocumentId()));
			xySeries.set(
				(segmentNo*segmentSizeInPercent)-(segmentSizeInPercent/2), 
				xySeries.get(
					(segmentNo*segmentSizeInPercent)-(segmentSizeInPercent/2))+1);
		}
		xySeriesCollection.add(xySeries);
	}

	private int getSegment(Range range, Long offset) {
		double curSize = segmentSize;
		int curSegment = 1;
		while (offset+range.getStartPoint() > curSize) {
			curSegment++;
			curSize += segmentSize;
		}
		return curSegment;
	}


	public List<XYValues<Integer, Integer>> getXYSeriesCollection() {
		return Collections.unmodifiableList(xySeriesCollection);
	}

	public int getPercentSegmentSize() {
		return segmentSizeInPercent;
	}

	
	public Set<PlotBand> getPlotBands() {
		return plotBands;
	}

}
