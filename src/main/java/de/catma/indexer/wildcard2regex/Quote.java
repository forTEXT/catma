package de.catma.indexer.wildcard2regex;

public class Quote implements OutputSequence {
	private StringBuilder builder = new StringBuilder();
	public void add(char c) {
		builder.append(c);
	}
	public void add(String s) {
		builder.append(s);
	}
	
	@Override
	public String toString() {
		return "\\Q"+builder.toString()+"\\E";
	}
}
