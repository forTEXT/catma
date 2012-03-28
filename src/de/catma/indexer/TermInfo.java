package de.catma.indexer;

import de.catma.core.document.Range;

public class TermInfo {

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
		return term + range + " " + tokenOffset;
	}
}
