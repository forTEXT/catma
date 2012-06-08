package de.catma.queryengine.result.computation;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	
	public DistributionComputation(GroupedQueryResultSet groupedQueryResultSet,
			Repository repository, List<String> relevantSourceDocumentIDs) throws IOException {
		this.groupedQueryResultSet = groupedQueryResultSet;
		this.repository = repository;
		this.relevantSourceDocumentIDs = relevantSourceDocumentIDs;
		computeSegmentSize();
	}
	
	
	private void computeSegmentSize() throws IOException {
		Set<SourceDocument> toBeUnloaded = new HashSet<SourceDocument>();
		long totalSize = 0;
		for (String sourceDocId : relevantSourceDocumentIDs) {
			SourceDocument sd = repository.getSourceDocument(sourceDocId);
			if (!sd.isLoaded()) {
				toBeUnloaded.add(sd);
			}
			totalSize += sd.getLength();
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
		XYSeries<Integer, Integer> xySeries = 
				new XYSeries<Integer, Integer>(group);
		
		//hier gehts weiter: 
		/*
		 * count occurrences per segment by comparing ranges
		 * add xy (curSegmentAofDocB, count)
		 * make sure every segmentAofDocB has an entry (0 for no occurrences)
		 * 
		 * note: NOT every document has the same number of segments!!!
		 */
		
		
		for (QueryResultRow row : groupedQueryResult) {
			for (int i=0; i<numberOfSegments; i++) {
				
			}
		}
	}
	

}
