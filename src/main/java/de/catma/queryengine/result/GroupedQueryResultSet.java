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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

public class GroupedQueryResultSet implements QueryResult {
	private static class GroupedQueryResultListIterator 
		implements Iterator<QueryResultRow>{
		
		private Iterator<GroupedQueryResult> groupedQueryResultIterator;
		private Iterator<QueryResultRow> currentResultRowIterator;
		private int currentRows = 0;
		
		public GroupedQueryResultListIterator(
				GroupedQueryResultSet groupedQueryResultSet) {
			this.groupedQueryResultIterator = 
					groupedQueryResultSet.groupedQueryResults.iterator();
		}
		public boolean hasNext() {
			if ((this.currentResultRowIterator == null) 
					|| (!this.currentResultRowIterator.hasNext())) {
				
				while (this.groupedQueryResultIterator.hasNext()
						&& ((this.currentResultRowIterator == null) 
								|| !currentResultRowIterator.hasNext())) {
					GroupedQueryResult curGroupedQueryResult = 
							groupedQueryResultIterator.next();
					currentRows = curGroupedQueryResult.getTotalFrequency();
					
					this.currentResultRowIterator = 
							curGroupedQueryResult.iterator();
					if (!currentResultRowIterator.hasNext()) {
						groupedQueryResultIterator.remove();
					}
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
			if (currentResultRowIterator != null) {
				currentRows--;
				currentResultRowIterator.remove();
				if (currentRows == 0) {
					groupedQueryResultIterator.remove();
				}
			}
		}
	}
	
	private Set<GroupedQueryResult> groupedQueryResults;
	
	public GroupedQueryResultSet() {
		groupedQueryResults = new HashSet<GroupedQueryResult>();
	}
	
	public Iterator<QueryResultRow> iterator() {
		return new GroupedQueryResultListIterator(this);
	}
	
	public Set<GroupedQueryResult> asGroupedSet() {
		return groupedQueryResults;
	}
	
	@Override
	public Set<GroupedQueryResult> asGroupedSet(Function<QueryResultRow, Object> groupingKeyProvider) {
		return asQueryResultRowArray().asGroupedSet(groupingKeyProvider);
	}
	
	public void add(GroupedQueryResult groupedQueryResult) {
		groupedQueryResults.add(groupedQueryResult);
	}

	public boolean addAll(Collection<? extends GroupedQueryResult> c) {
		return groupedQueryResults.addAll(c);
	}

	public QueryResultRowArray asQueryResultRowArray() {
		QueryResultRowArray result = new QueryResultRowArray();
		for (QueryResultRow row : this) {
			result.add(row);
		}
		return result;
	}
}
