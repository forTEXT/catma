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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class QueryResultRowArray extends ArrayList<QueryResultRow> implements QueryResult {

	public QueryResultRowArray() {
	}
	
	public QueryResultRowArray(Collection<QueryResultRow> rows) {
		addAll(rows);
	}

	@Override
	public String toString() {
		return Arrays.toString(this.toArray());
	}
	
	public int getFrequency(String sourceDocumentID) {
		int sum = 0;
		for (QueryResultRow row : this) {
			if (row.getSourceDocumentId().equals(sourceDocumentID)) {
				sum++;
			}
		}
		return sum;
	}
	
	public Set<String> getSourceDocumentIDs() {
		Set<String> sourceDocumentIDs = new HashSet<String>();
		for (QueryResultRow row : this) {
			sourceDocumentIDs.add(
					row.getSourceDocumentId());
		}
		return sourceDocumentIDs;
	}
	
	public int getTotalFrequency() {
		return size();
	}
	
	public Set<GroupedQueryResult> asGroupedSet() {
		HashMap<String, PhraseResult> phraseResultMapping = 
				new HashMap<String, PhraseResult>();
		
		for (QueryResultRow row : this) {
			if (row.getPhrase() == null) {
				throw new UnsupportedOperationException(
					"The rows in this Iterable are not phrase based, " +
					"don't know how to group!");
			}
			if (!phraseResultMapping.containsKey(row.getPhrase())) {
				phraseResultMapping.put(
					row.getPhrase(), new PhraseResult(row.getPhrase()));
			}
			
			phraseResultMapping.get(row.getPhrase()).addQueryResultRow(row);
		}
		
		Set<GroupedQueryResult> groupedQueryResults = 
				new HashSet<GroupedQueryResult>();
		
		groupedQueryResults.addAll(phraseResultMapping.values());
		
		return groupedQueryResults;	
	}

	public QueryResultRowArray asQueryResultRowArray() {
		return this;
	}
}
