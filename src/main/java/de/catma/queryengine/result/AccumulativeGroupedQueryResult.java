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
package de.catma.queryengine.result;

import java.util.Arrays;
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
	
	public AccumulativeGroupedQueryResult() {
		rows = new HashSet<QueryResultRow>();
		frequencyBySourceDocument = new HashMap<String, Integer>();
	}

	public AccumulativeGroupedQueryResult(Set<GroupedQueryResult> results) {
		this();
		
		for (GroupedQueryResult result : results) {
			addGroupedQueryResult(result);
		}
		
	}

	public void addGroupedQueryResult(GroupedQueryResult result) {
		String conc = (group == null) ? "" : ",";
		StringBuilder groupBuilder = new StringBuilder((group==null) ? "" : group);
		
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

	public GroupedQueryResult getSubResult(String... sourceDocumentID) {
		Set<String> filterSourceDocumentIds = new HashSet<String>(); 
		filterSourceDocumentIds.addAll(Arrays.asList(sourceDocumentID));
		
		PhraseResult subResult = new PhraseResult(group);
		for (QueryResultRow row : this) {
			if (filterSourceDocumentIds.contains(row.getSourceDocumentId())) {
				subResult.addQueryResultRow(row);
			}
		}
		return subResult;
	}

}
