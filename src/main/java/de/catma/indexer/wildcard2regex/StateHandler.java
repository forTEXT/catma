package de.catma.indexer.wildcard2regex;

public interface StateHandler {
	public State handleCharacter(char c, Context context);
}
