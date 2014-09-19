package de.catma.indexer;

public class EqualsMatcher implements TermMatcher {

	@Override
	public boolean match(String term1, String term2) {
		return term1.equals(term2);
	}

}
