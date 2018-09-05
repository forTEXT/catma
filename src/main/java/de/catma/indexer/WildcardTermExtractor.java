/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import de.catma.indexer.wildcardparser.WildcardParser;

public class WildcardTermExtractor {
	
	private List<String> orderedTerms;
	
	public WildcardTermExtractor(String content, List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale) throws IOException {
		extractTermInfosWithWildcards(
			content,
			unseparableCharacterSequences, 
			userDefinedSeparatingCharacters, locale);
	}
	
	private void extractTermInfosWithWildcards(
			String content,
			List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale) throws IOException {
		
		if (locale == null) {
			locale = Locale.getDefault();
		}

		try (WhitespaceAndPunctuationAnalyzer analyzer = new WhitespaceAndPunctuationAnalyzer(
				unseparableCharacterSequences, userDefinedSeparatingCharacters, locale)) {
		
			TokenStream ts = analyzer.tokenStream(null, // our analyzer does not use
													// the fieldname
				new StringReader(content));
			ts.reset();

			WildcardParser wildcardParser = new WildcardParser();
			while (ts.incrementToken()) {
				CharTermAttribute termAttr = (CharTermAttribute) ts
						.getAttribute(CharTermAttribute.class);
	
				OffsetAttribute offsetAttr = (OffsetAttribute) ts
						.getAttribute(OffsetAttribute.class);
				wildcardParser.handle(termAttr, offsetAttr);
			}
			wildcardParser.finish();
			
			orderedTerms = new ArrayList<String>();
			for (TermInfo ti : wildcardParser.getOrderedTermInfos()) {
				orderedTerms.add(ti.getTerm());
			}
		}		
	}
	
	
	public List<String> getOrderedTerms() {
		return orderedTerms;
	}
	
}
