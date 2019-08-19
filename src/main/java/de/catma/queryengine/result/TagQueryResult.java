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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

/**
 * @author marco.petris@web.de
 *
 */
public class TagQueryResult implements GroupedQueryResult, QueryResult {
	
	private String group;
	private QueryResultRowArray rows;
	
	public TagQueryResult(String group) {
		this.group = group;
		this.rows = new QueryResultRowArray();
	}
	
	public int getFrequency(String sourceDocumentID) {
		return this.rows.getFrequency(sourceDocumentID);
	}
	
	public Object getGroup() {
		return group;
	}
	
	public Set<String> getSourceDocumentIDs() {
		return rows.getSourceDocumentIDs();
	}

	public int getTotalFrequency() {
		return rows.getTotalFrequency();
	}
	
	public Iterator<QueryResultRow> iterator() {
		return rows.iterator();
	}
	
	public Set<GroupedQueryResult> asGroupedSet() {
		return rows.asGroupedSet();
	}
	
	@Override
	public Set<GroupedQueryResult> asGroupedSet(Function<QueryResultRow, String> groupingKeyProvider) {
		return rows.asGroupedSet(groupingKeyProvider);
	}
	
	public QueryResultRowArray asQueryResultRowArray() {
		return rows;
	}
	
	public void add(TagQueryResultRow tagQueryResultRow) {
		rows.add(tagQueryResultRow);
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
