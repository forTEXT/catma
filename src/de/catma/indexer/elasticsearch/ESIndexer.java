package de.catma.indexer.elasticsearch;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import de.catma.core.document.source.SourceDocument;
import de.catma.indexer.Indexer;
import de.catma.indexer.TermInfo;
import de.catma.indexer.WhitespaceAndPunctuationAnalyzer;
import de.catma.indexer.unseparablecharactersequence.CharTree;
import de.catma.indexer.unseparablecharactersequence.CharTreeFactory;

public class ESIndexer implements Indexer {

	private ESCommunication esComm; 
	
	public ESIndexer() {
		esComm = new ESCommunication();
	}
	public void index(SourceDocument sourceDoc,
			List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale) 
						throws Exception{
        // just in case something went wrong, better use the default than nothing 
        if (locale == null) {
            locale = Locale.getDefault();
        }

        CharTreeFactory ctf = new CharTreeFactory();
        CharTree unseparableCharSeqTree =
                ctf.createCharMap(unseparableCharacterSequences); 

        WhitespaceAndPunctuationAnalyzer analyzer =
                new WhitespaceAndPunctuationAnalyzer(
                        unseparableCharSeqTree,
                        buildPatternFrom(userDefinedSeparatingCharacters),
                        locale);

        TokenStream ts =
                analyzer.tokenStream(
                        null, // our analyzer does not use the fieldname 
                        new StringReader(sourceDoc.getContent()));

        Map<String,List<TermInfo>> terms = new HashMap<String,List<TermInfo>>();
        
        int positionCounter = 0;
        while(ts.incrementToken()) {
            CharTermAttribute termAttr =
                    (CharTermAttribute)ts.getAttribute(CharTermAttribute.class);
            
            OffsetAttribute offsetAttr =
                    (OffsetAttribute)ts.getAttribute(OffsetAttribute.class);

            
            
            TermInfo ti =  new TermInfo(termAttr.toString(),
                offsetAttr.startOffset(), offsetAttr.endOffset(),positionCounter);
            
            if(! terms.containsKey(ti.getTerm())){
            	terms.put(ti.getTerm(), new ArrayList<TermInfo>());
            }
            terms.get(ti.getTerm()).add(ti);
            positionCounter++;
        }
        esComm.addToIndex(sourceDoc.getID(),terms);
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

}
