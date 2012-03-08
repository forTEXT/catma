package de.catma.indexer;

import de.catma.core.document.Range;

public class TermInfo {

	private String term;
	private Range range;

	public TermInfo(String term, int start, int end) {
		this.term = term;
		this.range = new Range(start,end);
	}
	
	public Range getRange() {
		return range;
	}
	
	public String getTerm() {
		return term;
	}

}
