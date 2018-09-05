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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;


public class TermExtractor {

	private Map<String, List<TermInfo>> terms;
	private List<String> termsInOrder;

	public TermExtractor(String content, List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale) throws IOException {
		extractTermInfos(
			content,
			unseparableCharacterSequences, 
			userDefinedSeparatingCharacters, locale);
	}

	private void extractTermInfos(
			String content,
			List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale) throws IOException {
		
		terms = new HashMap<String, List<TermInfo>>();
		termsInOrder = new ArrayList<String>();
		
		if (locale == null) {
			locale = Locale.getDefault();
		}

		try (WhitespaceAndPunctuationAnalyzer analyzer = new WhitespaceAndPunctuationAnalyzer(
				unseparableCharacterSequences, userDefinedSeparatingCharacters, locale)) {
		
			TokenStream ts = analyzer.tokenStream(null, // our analyzer does not use
														// the fieldname
					new StringReader(content));
			ts.reset();
			int positionCounter = 0;
			while (ts.incrementToken()) {
				CharTermAttribute termAttr = (CharTermAttribute) ts
						.getAttribute(CharTermAttribute.class);
	
				OffsetAttribute offsetAttr = (OffsetAttribute) ts
						.getAttribute(OffsetAttribute.class);
	
				TermInfo ti = new TermInfo(termAttr.toString(),
						offsetAttr.startOffset(), offsetAttr.endOffset(),
						positionCounter);
	
				if (!terms.containsKey(ti.getTerm())) {
					terms.put(ti.getTerm(), new ArrayList<TermInfo>());
				}
				terms.get(ti.getTerm()).add(ti);
				positionCounter++;
				
				termsInOrder.add(ti.getTerm());
			}
		}
	}
	
	/**
	 * @return a map term->list of terminfo (range, tokenoffset)
	 */
	public Map<String, List<TermInfo>> getTerms() {
		return terms;
	}
	
	
	public List<String> getTermsInOrder() {
		return termsInOrder;
	}
}
