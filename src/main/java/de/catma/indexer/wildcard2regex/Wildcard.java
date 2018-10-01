package de.catma.indexer.wildcard2regex;

public class Wildcard implements OutputSequence {
	public String wildcardText;

	public Wildcard(String wildcardText) {
		super();
		this.wildcardText = wildcardText;
	}
	
	@Override
	public String toString() {
		return wildcardText;
	}
	
}
