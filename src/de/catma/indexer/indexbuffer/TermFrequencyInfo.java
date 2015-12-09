package de.catma.indexer.indexbuffer;

public class TermFrequencyInfo {
	private String term;
	private int frequency;
	public TermFrequencyInfo(String term, int frequency) {
		super();
		this.term = term;
		this.frequency = frequency;
	}
	
	public String getTerm() {
		return term;
	}
	
	public int getFrequency() {
		return frequency;
	}
}
