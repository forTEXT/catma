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
package de.catma.queryengine;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import de.catma.document.Range;
import de.catma.queryengine.result.QueryResultRow;
import de.catma.queryengine.result.TagQueryResultRow;

public enum TagMatchMode {
	BOUNDARY(new Comparator<QueryResultRow>() {
		public int compare(QueryResultRow o1, QueryResultRow o2) {
			if (!o1.getSourceDocumentId().equals(o2.getSourceDocumentId())) {
				return -1;
			}
//        	if(o1.getRange().isInBetween(o2.getRange())) {
			if (isInBetween(o1, o2)) {
        		return 0;
        	}
            else {
                return (int)(o1.getRange().getStartPoint()-o2.getRange().getStartPoint())+
                		(o1.getRange().getEndPoint()-o2.getRange().getEndPoint());
            }
		}
		
		private boolean isInBetween(QueryResultRow o1, QueryResultRow o2) {
			Collection<Range> ranges1 = null;
			if (o1 instanceof TagQueryResultRow) {
				ranges1 = ((TagQueryResultRow)o1).getRanges();
			}
			else {
				ranges1 = Collections.singletonList(o1.getRange());
			}
			Collection<Range> ranges2 = null;
			if (o2 instanceof TagQueryResultRow) {
				ranges2 = ((TagQueryResultRow)o2).getRanges();
			}
			else {
				ranges2 = Collections.singletonList(o2.getRange());
			}
			
			for (Range r1 : ranges1) {
				boolean found = false;
				for (Range r2 : ranges2) {
					if (r1.isInBetween(r2)) {
						found = true;
						break;
					}
				}
				
				if (!found) {
					return false;
				}
			}
			
			return true;
		}
		
		public String toString() {
			return TagMatchMode.BOUNDARY.name() + super.toString();
		};
	}),
	OVERLAP(new Comparator<QueryResultRow>() {
		public int compare(QueryResultRow o1, QueryResultRow o2) {
			if (!o1.getSourceDocumentId().equals(o2.getSourceDocumentId())) {
				return -1;
			}
			if (hasOverlappingRange(o1, o2)) {
				return 0;
			}
			else {
				return (int)(o1.getRange().getStartPoint()-o2.getRange().getStartPoint());
			}
		}
		public String toString() {
			return TagMatchMode.OVERLAP.name() + super.toString();
		};
		
		private boolean hasOverlappingRange(QueryResultRow o1, QueryResultRow o2) {
			Collection<Range> ranges1 = null;
			if (o1 instanceof TagQueryResultRow) {
				ranges1 = ((TagQueryResultRow)o1).getRanges();
			}
			else {
				ranges1 = Collections.singletonList(o1.getRange());
			}
			Collection<Range> ranges2 = null;
			if (o2 instanceof TagQueryResultRow) {
				ranges2 = ((TagQueryResultRow)o2).getRanges();
			}
			else {
				ranges2 = Collections.singletonList(o2.getRange());
			}
			
			for (Range r1 : ranges1) {
				boolean found = false;
				for (Range r2 : ranges2) {
					if (r1.hasOverlappingRange(r2)) {
						found = true;
						break;
					}
				}
				
				if (!found) {
					return false;
				}
			}
			
			return true;
		}


	}),
	EXACT(new Comparator<QueryResultRow>() {
		public int compare(QueryResultRow o1, QueryResultRow o2) {
			if (!o1.getSourceDocumentId().equals(o2.getSourceDocumentId())) {
				return -1;
			}
			if (isExactMatch(o1, o2)) {
				return 0;
			}
			else {
				return 1;
			}
		}
		
		public String toString() {
			return TagMatchMode.EXACT.name() + super.toString();
		};
		
		private boolean isExactMatch(QueryResultRow o1, QueryResultRow o2) {
			Collection<Range> ranges1 = null;
			if (o1 instanceof TagQueryResultRow) {
				ranges1 = ((TagQueryResultRow)o1).getRanges();
			}
			else {
				ranges1 = Collections.singletonList(o1.getRange());
			}
			Collection<Range> ranges2 = null;
			if (o2 instanceof TagQueryResultRow) {
				ranges2 = ((TagQueryResultRow)o2).getRanges();
			}
			else {
				ranges2 = Collections.singletonList(o2.getRange());
			}
			
			for (Range r1 : ranges1) {
				boolean found = false;
				for (Range r2 : ranges2) {
					if (r1.compareTo(r2)==0) {
						found = true;
						break;
					}
				}
				
				if (!found) {
					return false;
				}
			}
			
			return true;
		}
		

	}),
	;
	
	private Comparator<QueryResultRow> comparator;

	private TagMatchMode(Comparator<QueryResultRow> comparator) {
		this.comparator = comparator;
	}
	
	public Comparator<QueryResultRow> getComparator() {
		return comparator;
	}
}
