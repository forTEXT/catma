package de.catma.indexer.wildcard2regex;

import java.util.ArrayList;
import java.util.List;

class Context {
	private List<OutputSequence> output;
	private Quote currentQuote;
	
	public Context() {
		super();
		this.output = new ArrayList<>();
	}
	List<OutputSequence> getOutput() {
		return output;
	}
	Quote getCurrentQuote() {
		return currentQuote;
	}
	
	void setCurrentQuote(Quote currentQuote) {
		this.currentQuote = currentQuote;
		output.add(currentQuote);
	}
	
	boolean add(OutputSequence e) {
		return output.add(e);
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();
		
		for (OutputSequence seq: output) {
			builder.append(seq.toString());
		}
	
		return builder.toString();
	
	}
	public boolean hasWildcard() {

		for (OutputSequence os : output) {
			if (os instanceof Wildcard) {
				return true;
			}
		}
		return false;
	}
	
}
