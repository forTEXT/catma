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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class PhraseResult implements GroupedQueryResult {
	
	private static class PhraseResultRowIterator implements Iterator<QueryResultRow> {
		
		private Iterator<List<QueryResultRow>> phraseResultListIterator;
		private Iterator<QueryResultRow> currentResultRowIterator;
		
		public PhraseResultRowIterator(PhraseResult phraseResult) {
			this.phraseResultListIterator = 
					phraseResult.sourceDocumentResults.values().iterator(); 
		}
		
		public boolean hasNext() {
			if ((this.currentResultRowIterator == null) 
					|| (!this.currentResultRowIterator.hasNext())) {
				
				while (this.phraseResultListIterator.hasNext()
						&& ((this.currentResultRowIterator == null) 
								|| !currentResultRowIterator.hasNext())) {
					this.currentResultRowIterator = 
							phraseResultListIterator.next().iterator();
				}
				
				if (currentResultRowIterator != null) {
					return currentResultRowIterator.hasNext();
				}
				
				return false;
			}
			return true;
		}
		
		public QueryResultRow next() {
			if (hasNext()) {
				return currentResultRowIterator.next();
			}
			return null;
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private Object group;
	private Map<String, List<QueryResultRow>> sourceDocumentResults;
	
	public PhraseResult(Object group) {
		this.group = group;
		sourceDocumentResults = new HashMap<String, List<QueryResultRow>>();
	}
	
	public void add(QueryResultRow row) {
		if (!sourceDocumentResults.containsKey(row.getSourceDocumentId())) {
			sourceDocumentResults.put(
				row.getSourceDocumentId(), new ArrayList<QueryResultRow>());
		}
		
		sourceDocumentResults.get(row.getSourceDocumentId()).add(row);
	}
	
	public String getPhrase() {
		return getGroup().toString();
	}
	
	public Object getGroup() {
		return group;
	}
	
	public int getTotalFrequency() {
		int sum = 0;
		for (List<QueryResultRow> rows : sourceDocumentResults.values()) {
			sum += rows.size();
		}
		return sum;
	}
	
	public int getFrequency(String sourceDocumentID) {
		if (sourceDocumentResults.containsKey(sourceDocumentID)) {
			return sourceDocumentResults.get(sourceDocumentID).size();
		}
		else {
			return 0;
		}
	}
	
	public Set<String> getSourceDocumentIDs() {
		return sourceDocumentResults.keySet();
	}
	
	public Iterator<QueryResultRow> iterator() {
		return new PhraseResultRowIterator(this);
	}

	@Override
	public String toString() {
		return "PhraseResult [phrase=" + getPhrase() + ", sourceDocumentResults="
				+ Arrays.toString(sourceDocumentResults.keySet().toArray()) + "]";
	}
	

	public GroupedQueryResult getSubResult(String... sourceDocumentID) {
		Set<String> filterSourceDocumentIds = new HashSet<String>(); 
		filterSourceDocumentIds.addAll(Arrays.asList(sourceDocumentID));
		
		PhraseResult subResult = new PhraseResult(group);
		for (QueryResultRow row : this) {
			if (filterSourceDocumentIds.contains(row.getSourceDocumentId())) {
				subResult.add(row);
			}
		}
		return subResult;
	}
	
	@Override
	public boolean contains(QueryResultRow row) {
		if (sourceDocumentResults.containsKey(row.getSourceDocumentId())) {
			return sourceDocumentResults.get(row.getSourceDocumentId()).contains(row);
		}
		return false;
	}
	
	@Override
	public boolean remove(QueryResultRow row) {
		if (sourceDocumentResults.containsKey(row.getSourceDocumentId())) {
			return sourceDocumentResults.get(row.getSourceDocumentId()).remove(row);
		}

		return false;
	}
}
