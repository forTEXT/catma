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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.source.SourceDocument;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.GroupedQueryResultSet;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;
import de.catma.queryengine.result.TagQueryResultRow;
import de.catma.tag.TagDefinition;

public class DistributionOverTagComputation extends DistributionComputation {
	
	private GroupedQueryResultSet groupedQueryResultSet;
	private Repository repository;
	private List<String> relevantSourceDocumentIDs;
	private double segmentSizeInPercent = 10.0;
	private HashMap<String, Distribution> documentDistributions;
	private int maxOccurrences = 0;
	private QueryResult tagInstances;
	
	public DistributionOverTagComputation(GroupedQueryResultSet groupedQueryResultSet,
			Repository repository, List<String> relevantSourceDocumentIDs, QueryResult tagInstances) throws IOException {
		super(groupedQueryResultSet, repository, relevantSourceDocumentIDs);
		this.groupedQueryResultSet = groupedQueryResultSet;
		this.repository = repository;
		this.relevantSourceDocumentIDs = relevantSourceDocumentIDs;
		this.tagInstances = tagInstances;
		this.documentDistributions = new HashMap<String, Distribution>();
	}

	private void prepareDistributions() throws IOException {
		Set<SourceDocument> toBeUnloaded = new HashSet<SourceDocument>();
		for (String sourceDocId : relevantSourceDocumentIDs) {
			SourceDocument sd = repository.getSourceDocument(sourceDocId);
			if (!sd.isLoaded()) {
				toBeUnloaded.add(sd);
			}
			documentDistributions.put(sourceDocId, 
				new Distribution(
					sd.getID(),
					sd.toString(),
					Double.valueOf(sd.getLength())/100.0*segmentSizeInPercent,
					segmentSizeInPercent));		
		}
		for (SourceDocument sd : toBeUnloaded) {
			sd.unload();
		}
	}


	public void compute() throws IOException {
		prepareDistributions();
		for (GroupedQueryResult groupedQueryResult : groupedQueryResultSet.asGroupedSet()) {
			compute(groupedQueryResult);
		}
	}


	private void compute(GroupedQueryResult groupedQueryResult) {
		Object group = groupedQueryResult.getGroup();
		
		for (String sourceDocId : groupedQueryResult.getSourceDocumentIDs()) {
			XYValues<Integer, Integer, QueryResultRow> xyValues = 
					new XYValues<Integer, Integer, QueryResultRow>(group);
			Distribution distribution = documentDistributions.get(sourceDocId);
			double documentSize = distribution.getSegmentSize()*segmentSizeInPercent;
			QueryResult sourceDocTagInstances = QueryResultRowArray.createSubResult(tagInstances, sourceDocId);
			for (QueryResultRow tagInstanceRow : sourceDocTagInstances) {
				distribution.addPlotBand(createPlotBand((TagQueryResultRow)tagInstanceRow, documentSize));
			}
			
			for (QueryResultRow row : groupedQueryResult.getSubResult(sourceDocId)) {
				Integer xValue = computeXValue(row, sourceDocTagInstances, documentSize );
				
				if (xValue != null) {
					int yValue =(xyValues.get(xValue)==null)?1:xyValues.get(xValue)+1; 
					
					xyValues.set(xValue, yValue, row);
				}
				//else skip the value
			}
				
			distribution.add(xyValues);
			if (maxOccurrences < xyValues.getMaxYValue()) {
				maxOccurrences = xyValues.getMaxYValue();
			}
		}
	}

	private PlotBand<Integer> createPlotBand(TagQueryResultRow tagInstanceRow, double documentSize) {
		
		Range range = tagInstanceRow.getRange();
		int plotBandStart = Double.valueOf(100.0/documentSize*range.getStartPoint()).intValue();
		int plotBandEnd = Double.valueOf(100.0/documentSize*range.getEndPoint()).intValue();
		String label = TagDefinition.getLastPathComponent(tagInstanceRow.getTagDefinitionPath());
		
		return new PlotBand<Integer>(plotBandStart, plotBandEnd, label);
	}

	private Integer computeXValue(QueryResultRow row,
			QueryResult sourceDocTagInstances, double totalSize) {
		for (QueryResultRow tagInstanceRow : sourceDocTagInstances) {
			Range xRange = tagInstanceRow.getRange(); 
			if (row.getRange().isInBetween(xRange)) {
				double xRangeCenter = (xRange.getSize()/2.0)+xRange.getStartPoint();
				
				return Double.valueOf((100.0/totalSize)*xRangeCenter).intValue();
			}
		}
		
		return null;
	}

	public List<Distribution> getDistributions() {
		return new ArrayList<Distribution>(documentDistributions.values());
	}

	public int getMaxOccurrences() {
		return maxOccurrences;
	}
}
