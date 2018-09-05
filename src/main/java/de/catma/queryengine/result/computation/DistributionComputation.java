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
import de.catma.queryengine.result.QueryResultRow;

public class DistributionComputation {
	
	private GroupedQueryResultSet groupedQueryResultSet;
	private Repository repository;
	private List<String> relevantSourceDocumentIDs;
	private double segmentSizeInPercent = 10.0;
	private HashMap<String, Distribution> documentDistributions;
	private int maxOccurrences = 0;
	
	public DistributionComputation(GroupedQueryResultSet groupedQueryResultSet,
			Repository repository, List<String> relevantSourceDocumentIDs) throws IOException {
		this.groupedQueryResultSet = groupedQueryResultSet;
		this.repository = repository;
		this.relevantSourceDocumentIDs = relevantSourceDocumentIDs;
		this.documentDistributions = new HashMap<String, Distribution>();
	}

	private void prepareDistributions() throws IOException {
		Set<SourceDocument> toBeUnloaded = new HashSet<SourceDocument>();
		try {
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
		catch (Exception e) {
			try {
				for (SourceDocument sd : toBeUnloaded) {
					sd.unload();
				}
			}
			catch (Exception e2) {e2.printStackTrace();}
			throw new IOException(e);
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
			
			String xyValuesLabel = group + " " + groupedQueryResult.getFrequency(sourceDocId);
			
			if (xyValuesLabel.length() > 39) {
				xyValuesLabel = "..." + xyValuesLabel.substring(xyValuesLabel.length()-39);
			}
			
			XYValues<Integer, Integer, QueryResultRow> xyValues = 
					new XYValues<Integer, Integer, QueryResultRow>(xyValuesLabel);
			Distribution distribution = documentDistributions.get(sourceDocId);
			for (QueryResultRow row : groupedQueryResult.getSubResult(sourceDocId)) {
				int segmentNo =
						getSegment(
								row.getRange(), 
								distribution.getSegmentSize());
				int xValue = Double.valueOf((segmentNo*segmentSizeInPercent)-(segmentSizeInPercent/2.0)).intValue();
				int yValue =(xyValues.get(xValue)==null)?1:xyValues.get(xValue)+1; 
				
				xyValues.set(xValue, yValue, row);
			}
			double segementCount = 100.0/segmentSizeInPercent;
			
			// set segments without occurrences to zero
			for (int segmentNo=1; segmentNo<=segementCount; segmentNo++) {
				int xValue = Double.valueOf((segmentNo*segmentSizeInPercent)-(segmentSizeInPercent/2.0)).intValue();
				if (xyValues.get(xValue) == null) {
					xyValues.set(xValue, 0);
				}
			}
			
			distribution.add(xyValues);
			if (maxOccurrences < xyValues.getMaxYValue()) {
				maxOccurrences = xyValues.getMaxYValue();
			}
		}
	}

	private int getSegment(Range range, double segmentSize) {
		double curSize = segmentSize;
		int curSegment = 1;
		while (range.getStartPoint() > curSize) {
			curSegment++;
			curSize += segmentSize;
		}
		return curSegment;
	}

	public List<Distribution> getDistributions() {
		return new ArrayList<Distribution>(documentDistributions.values());
	}

	public int getMaxOccurrences() {
		return maxOccurrences;
	}
}
