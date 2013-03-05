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

import java.util.Comparator;

import de.catma.queryengine.result.QueryResultRow;

public enum TagMatchMode {
	BOUNDARY(new Comparator<QueryResultRow>() {
		public int compare(QueryResultRow o1, QueryResultRow o2) {
			if (!o1.getSourceDocumentId().equals(o2.getSourceDocumentId())) {
				return -1;
			}
        	if(o1.getRange().isInBetween(o2.getRange())) {
        		return 0;
        	}
            else {
                return (int)(o1.getRange().getStartPoint()-o2.getRange().getStartPoint())+
                		(o1.getRange().getEndPoint()-o2.getRange().getEndPoint());
            }
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
			if (o1.getRange().hasOverlappingRange(o2.getRange())) {
				return 0;
			}
			else {
				return (int)(o1.getRange().getStartPoint()-o2.getRange().getStartPoint());
			}
		}
		public String toString() {
			return TagMatchMode.OVERLAP.name() + super.toString();
		};

	}),
	EXACT(new Comparator<QueryResultRow>() {
		public int compare(QueryResultRow o1, QueryResultRow o2) {
			if (!o1.getSourceDocumentId().equals(o2.getSourceDocumentId())) {
				return -1;
			}
			return o1.getRange().compareTo(o2.getRange());
		}
		
		public String toString() {
			return TagMatchMode.EXACT.name() + super.toString();
		};

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
