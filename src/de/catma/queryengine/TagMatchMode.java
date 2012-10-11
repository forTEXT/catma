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
	}),
	EXACT(new Comparator<QueryResultRow>() {
		public int compare(QueryResultRow o1, QueryResultRow o2) {
			if (!o1.getSourceDocumentId().equals(o2.getSourceDocumentId())) {
				return -1;
			}
			return o1.getRange().compareTo(o2.getRange());
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
