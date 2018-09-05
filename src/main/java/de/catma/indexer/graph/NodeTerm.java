package de.catma.indexer.graph;

public class NodeTerm {

	private String term;
	private int frequency;
	public NodeTerm(String term, int frequency) {
		super();
		this.term = term;
		this.frequency = frequency;
	}
	
	public int getFrequency() {
		return frequency;
	}
	
	public String getTerm() {
		return term;
	}
}
