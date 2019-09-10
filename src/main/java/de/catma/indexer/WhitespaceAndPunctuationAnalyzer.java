/*
 *    CATMA Computer Aided Text Markup and Analysis
 * 
 *    Copyright (C) 2009  University Of Hamburg
 * 
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 * 
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.catma.indexer;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

import de.catma.indexer.unseparablecharactersequence.CharTree;
import de.catma.indexer.unseparablecharactersequence.CharTreeFactory;

/**
 * This anaylizer is like the classic {@link org.apache.lucene.analysis.WhitespaceAnalyzer}
 * but it respects punctuation, user defined separating characters and unseparable character
 * sequences.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class WhitespaceAndPunctuationAnalyzer extends Analyzer {

    private CharTree unseparableCharacterSequences;
    private Pattern userDefSeparatingPunctuationPattern;
    private Locale locale;
  
    public WhitespaceAndPunctuationAnalyzer(
    		List<String> unseparableCharacterSequencesList,
			List<Character> userDefinedSeparatingCharactersList, Locale locale) {
        CharTreeFactory ctf = new CharTreeFactory();
        this.unseparableCharacterSequences  =
                ctf.createCharMap(unseparableCharacterSequencesList); 
        this.userDefSeparatingPunctuationPattern = 
        		buildPatternFrom(userDefinedSeparatingCharactersList);
        this.locale = locale;
    }

    /**
     * Creates an OR-ed regex pattern from the list of user defined separating characters.
     * @param userDefinedSeparatingCharacters the list of user defined separating characters
     * @return the pattern
     */
    private Pattern buildPatternFrom(
    		List<Character> userDefinedSeparatingCharacters) {

        if (userDefinedSeparatingCharacters.isEmpty()) {
            return null;
        }
        
        StringBuilder patternBuilder = new StringBuilder();
        String conc = "";

        for (Character c : userDefinedSeparatingCharacters) {
            patternBuilder.append(conc);
            patternBuilder.append(Pattern.quote(c.toString()));
            conc = "|"; // OR
        }

        return Pattern.compile(patternBuilder.toString());
    }

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		Tokenizer source = new CatmaWhitespaceTokenizer(unseparableCharacterSequences);
		TokenStream result = new PunctuationTokenizer(
				source, 
				unseparableCharacterSequences,
				userDefSeparatingPunctuationPattern, locale);
		
		return new TokenStreamComponents(source, result);
	}
    
    

}
