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
package de.catma.indexer.db;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import de.catma.indexer.db.model.Term;
import de.catma.queryengine.result.GroupedQueryResult;
import de.catma.queryengine.result.PhraseResult;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.QueryResultRowArray;

public class LazyDBPhraseQueryResult implements GroupedQueryResult {
	
	private Map<String, Term> termsByDocument;
	private String term;
	private QueryResultRowArray queryResultRowArray;
	private Map<String,Integer> freqByDocument;

	public LazyDBPhraseQueryResult(String term) {
		this.term = term;
		termsByDocument = new HashMap<String, Term>();
	}

	public Iterator<QueryResultRow> iterator() {
		if (queryResultRowArray == null) {
			try {
				loadQueryResultRows();
			}
			catch (NamingException ne) {
				throw new RuntimeException(ne);
			}
		}
		return queryResultRowArray.iterator();
	}

	private void loadQueryResultRows() throws NamingException {
		queryResultRowArray = new QueryResultRowArray();
		freqByDocument = new HashMap<String, Integer>();
		PhraseSearcher phraseSearcher = new PhraseSearcher();
		for (String sourceDocumentID : getSourceDocumentIDs()) {
			QueryResultRowArray positions =
					phraseSearcher.getPositionsForTerm(
							term, 
							sourceDocumentID);
			queryResultRowArray.addAll(positions);
		}
	}

	public Object getGroup() {
		return term;
	}

	public int getTotalFrequency() {
		
		if (queryResultRowArray != null) {
			return queryResultRowArray.size();
		}
		
		int sum = 0;
		for (Term t : termsByDocument.values()) {
			sum += t.getFrequency();
		}
		return sum;
	}

	public int getFrequency(String sourceDocumentID) {
		if (termsByDocument.containsKey(sourceDocumentID)) {
			if (queryResultRowArray != null) {
				if (!freqByDocument.containsKey(sourceDocumentID)) {
					freqByDocument.put(sourceDocumentID, computeFrequency(sourceDocumentID));
				}
				return freqByDocument.get(sourceDocumentID);
			}
			return termsByDocument.get(sourceDocumentID).getFrequency();
		}
		else {
			return 0;
		}
	}

	private int computeFrequency(String sourceDocumentID) {
		int freq = 0;
		for (QueryResultRow row : queryResultRowArray) {
			if (row.getSourceDocumentId().equals(sourceDocumentID)) {
				freq++;
			}
		}
		return freq;
	}

	public Set<String> getSourceDocumentIDs() {
		return termsByDocument.keySet();
	}

	void addTerm(Term t) {
		termsByDocument.put(t.getDocumentId(), t);
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
