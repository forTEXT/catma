package de.catma.indexer;

import java.util.Comparator;

import de.catma.document.Range;

public class TermInfo {
	
	static class TokenOffsetComparator implements Comparator<TermInfo> {
		public int compare(TermInfo o1, TermInfo o2) {
			return o1.tokenOffset-o2.tokenOffset;
		}
	}
	
	public static final TokenOffsetComparator TOKENOFFSETCOMPARATOR = 
			new TokenOffsetComparator();

	private String term;
	private Range range;
	private int tokenOffset;
	
	public TermInfo(String term, int start, int end) {
		this(term, start, end, 0);
	}

	public TermInfo(String term, int start, int end, int tokenOffset) {
		this.term = term;
		this.range = new Range(start,end);
		this.tokenOffset = tokenOffset;
	}
	
	public Range getRange() {
		return range;
	}
	
	public String getTerm() {
		return term;
	}

	public int getTokenOffset() {
		return tokenOffset;
	}

	@Override
	public String toString() {
		return term + "@" + tokenOffset + "@" + range ;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((range == null) ? 0 : range.hashCode());
		result = prime * result + ((term == null) ? 0 : term.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TermInfo other = (TermInfo) obj;
		if (range == null) {
			if (other.range != null)
				return false;
		} else if (!range.equals(other.range))
			return false;
		if (term == null) {
			if (other.term != null)
				return false;
		} else if (!term.equals(other.term))
			return false;
		return true;
	}
	
	
}
