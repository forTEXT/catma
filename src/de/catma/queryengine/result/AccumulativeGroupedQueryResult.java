package de.catma.queryengine.result;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AccumulativeGroupedQueryResult implements GroupedQueryResult {
	
	private Set<QueryResultRow> rows;
	private Map<String, Integer> frequencyBySourceDocument;
	private int totalFrequency;
	private String group;
	
	public AccumulativeGroupedQueryResult(Set<GroupedQueryResult> results) {
		String conc = "";
		StringBuilder groupBuilder = new StringBuilder();
		
		rows = new HashSet<QueryResultRow>();
		frequencyBySourceDocument = new HashMap<String, Integer>();
		
		for (GroupedQueryResult result : results) {
			totalFrequency += result.getTotalFrequency();
			for (String sourceDocumentID : result.getSourceDocumentIDs()) {
				if (!frequencyBySourceDocument.containsKey(sourceDocumentID)) {
					frequencyBySourceDocument.put(sourceDocumentID, 0);
				}
				frequencyBySourceDocument.put(
					sourceDocumentID, 
					frequencyBySourceDocument.get(sourceDocumentID)
						+result.getFrequency(sourceDocumentID));
			}
			for (QueryResultRow row : result) {
				rows.add(row);
			}
			groupBuilder.append(conc);
			groupBuilder.append(result.getGroup().toString());
			conc = ",";
		}
		
		group = groupBuilder.toString();
	}

	public Iterator<QueryResultRow> iterator() {
		return rows.iterator();
	}

	public Object getGroup() {
		return group;
	}

	public int getTotalFrequency() {
		return totalFrequency;
	}

	public int getFrequency(String sourceDocumentID) {
		if (frequencyBySourceDocument.containsKey(sourceDocumentID)) {
			return frequencyBySourceDocument.get(sourceDocumentID);
		}
 		return 0;
	}

	public Set<String> getSourceDocumentIDs() {
		return Collections.unmodifiableSet(frequencyBySourceDocument.keySet());
	}

}
