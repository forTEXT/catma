package de.catma.indexer;

import java.util.regex.Pattern;

public class SQLWildcardMatcher implements TermMatcher {

	public boolean match(String wildcardTerm, String term) {
		
		// first we break up in chunks seperated by %
		String[] percentParts = wildcardTerm.split("((?<=[^\\\\])%)|(^%)");
		StringBuilder pattern = new StringBuilder();
		String percentConc = "";
		
		// connect the %-parts and the _-parts with the corresponding regex
		for (String percentPart : percentParts) {
			pattern.append(percentConc);
			// then we break up in chunks seperated by _
			String[] underscoreParts = percentPart.split("((?<=[^\\\\])_)|(^_)");
			String underScoreConc = "";
			for (String underScorePart : underscoreParts) {
				pattern.append(underScoreConc);
				if (!underScorePart.isEmpty()) {
					pattern.append(Pattern.quote(underScorePart));
				}
				underScoreConc = ".{1}?";
			}
			
			if ((percentPart.length()>1) && (percentPart.endsWith("_"))) {
				pattern.append(underScoreConc);
			}
			
			percentConc = ".*?";
		}
		
		if ((wildcardTerm.length()>1) && (wildcardTerm.endsWith("%"))) {
			pattern.append(percentConc);
		}
		
		// validate
		return term.matches(pattern.toString());
	}
}
