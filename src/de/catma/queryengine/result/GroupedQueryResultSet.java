package de.catma.queryengine.result;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
