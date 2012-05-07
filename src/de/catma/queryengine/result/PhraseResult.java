package de.catma.queryengine.result;

import java.util.ArrayList;
import java.util.HashMap;
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
						&& !currentResultRowIterator.hasNext()) {
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

	private String phrase;
	private Map<String, List<QueryResultRow>> sourceDocumentResults;
	
	public PhraseResult(String phrase) {
		this.phrase = phrase;
		sourceDocumentResults = new HashMap<String, List<QueryResultRow>>();
	}
	
	public void addQueryResultRow(QueryResultRow row) {
		if (!sourceDocumentResults.containsKey(row.getSourceDocumentId())) {
			sourceDocumentResults.put(
				row.getSourceDocumentId(), new ArrayList<QueryResultRow>());
		}
		
		sourceDocumentResults.get(row.getSourceDocumentId()).add(row);
	}
	
	public String getPhrase() {
		return getGroup();
	}
	
	public String getGroup() {
		return phrase;
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
}
