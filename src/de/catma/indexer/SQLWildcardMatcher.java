/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2014  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.indexer;

import java.util.regex.Pattern;

/**
 * Matches a first term that can contain the SQL-wildcards % (zero or more) and _ (excactly one) 
 * against a second term.  
 * 
 * @author marco.petris@web.de
 *
 */
public class SQLWildcardMatcher implements TermMatcher {
	
	private static final String ANY_CHAR_WILDCARD = "%";
	private static final String SINGLE_CHAR_WILDCARD = "_";
	private static final String UNESCAPED_ANY_CHAR_WILDCARD = "((?<=[^\\\\])%)|(^%)";
	private static final String UNESCAPED_SINGLE_CHAR_WILDCARD = "((?<=[^\\\\])_)|(^_)";

	private static final String SINGLE_CHAR_WILDCARD_REPLACEMENT_PATTERN = ".{1}?";
	private static final String ANY_CHAR_WILDCARD_REPLACEMENT_PATTERN = ".*?";

	/* (non-Javadoc)
	 * @see de.catma.indexer.TermMatcher#match(java.lang.String, java.lang.String)
	 */
	public boolean match(String wildcardTerm, String term) {
		
		// match all?
		if (wildcardTerm.matches("[" + ANY_CHAR_WILDCARD + "]+")) {
			return true;
		}
		
		// first we break up in chunks seperated by unescaped ANY_CHAR_WILDCARD
		String[] anyCharWildcardParts = wildcardTerm.split(UNESCAPED_ANY_CHAR_WILDCARD);
		StringBuilder pattern = new StringBuilder();
		String anyCharWildcardConc = "";
		
		// connect the ANY_CHAR_WILDCARD-parts and the SINGLE_CHAR_WILDARD-parts with the corresponding regex
		for (String anyCharWildcardPart : anyCharWildcardParts) {
			pattern.append(anyCharWildcardConc);
			// then we break up in chunks seperated by unescaped _
			String[] singleCharWildcardParts = anyCharWildcardPart.split(UNESCAPED_SINGLE_CHAR_WILDCARD);
			String singleCharWildcardConc = "";
			// do we actually have parts or is it all underscores?
			if (singleCharWildcardParts.length > 0 ) {
				for (String singleCharWildcardPart : singleCharWildcardParts) {
					pattern.append(singleCharWildcardConc);
					if (!singleCharWildcardPart.isEmpty()) {
						pattern.append(
							Pattern.quote(
								singleCharWildcardPart
									.replaceAll("\\\\"+ANY_CHAR_WILDCARD, ANY_CHAR_WILDCARD)
									.replaceAll("\\\\"+SINGLE_CHAR_WILDCARD, SINGLE_CHAR_WILDCARD)));
					}
					singleCharWildcardConc = SINGLE_CHAR_WILDCARD_REPLACEMENT_PATTERN;
				}
				// catch ANY_CHAR_WILDCARD parts that consist of nonwildcard chars 
				// and end with unescaped single char wildcard
				if ((anyCharWildcardPart.length()>1) 
						&& (anyCharWildcardPart.matches(
							ANY_CHAR_WILDCARD_REPLACEMENT_PATTERN + UNESCAPED_SINGLE_CHAR_WILDCARD))) {
					pattern.append(singleCharWildcardConc);
				}
			}
			else { // all underscores so just add the equivalent number of patterns
				for (int i=0; i<anyCharWildcardPart.length(); i++) {
					pattern.append(SINGLE_CHAR_WILDCARD_REPLACEMENT_PATTERN);
				}
			}
			anyCharWildcardConc = ANY_CHAR_WILDCARD_REPLACEMENT_PATTERN;
		}
		// catch wildcarTerms that consist of nonwildcard chars and end with ANY_CHAR_WILDCARD
		if ((wildcardTerm.length()>1) 
				&& (wildcardTerm.matches(
					ANY_CHAR_WILDCARD_REPLACEMENT_PATTERN + UNESCAPED_ANY_CHAR_WILDCARD))) {
			pattern.append(anyCharWildcardConc);
		}
		
		// validate
		return term.matches(pattern.toString());
	}
}
