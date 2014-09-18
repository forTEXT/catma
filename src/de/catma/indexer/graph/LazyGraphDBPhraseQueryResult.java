/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2014  University Of Hamburg
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
package de.catma.indexer.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.PhraseResult;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;

class LazyGraphDBPhraseQueryResult implements GroupedQueryResult {
	
	private String term;
	private QueryResult queryResult;
	private Map<String,Integer> freqByDocument;
	private Integer sum;

	public LazyGraphDBPhraseQueryResult(String term) {
		this.term = term;
		freqByDocument = new HashMap<String, Integer>();
	}

	public Iterator<QueryResultRow> iterator() {
		if (queryResult == null) {
			try {
				loadQueryResultRows();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return queryResult.iterator();
	}

	private void loadQueryResultRows() throws NamingException, IOException {
		PhraseSearcher phraseSearcher = new PhraseSearcher();
		queryResult = phraseSearcher.search(
			new ArrayList<>(getSourceDocumentIDs()), 
			term, 
			Collections.singletonList(term), 
			0);
	}

	public Object getGroup() {
		return term;
	}

	public int getTotalFrequency() {
		if (sum == null) {
			sum = 0;
			for (int freq : freqByDocument.values()) {
				sum += freq;
			}
		}
		
		return sum;
	}

	public int getFrequency(String sourceDocumentID) {
		return freqByDocument.get(sourceDocumentID);
	}

	public Set<String> getSourceDocumentIDs() {
		return freqByDocument.keySet();
	}

	void addFrequency(String documentId, int frequency) {
		freqByDocument.put(documentId, frequency);
	}

	public GroupedQueryResult getSubResult(String... sourceDocumentID) {
		Set<String> filterSourceDocumentIds = new HashSet<String>(); 
		filterSourceDocumentIds.addAll(Arrays.asList(sourceDocumentID));
		
		PhraseResult subResult = new PhraseResult(term);
		for (QueryResultRow row : this) {
			if (filterSourceDocumentIds.contains(row.getSourceDocumentId())) {
				subResult.addQueryResultRow(row);
			}
		}
		return subResult;
	}
}
