package de.catma.indexer.elasticsearch;

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

import de.catma.indexer.TermInfo;
import de.catma.indexer.WhitespaceAndPunctuationAnalyzer;

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
		
		if (locale == null) {
			locale = Locale.getDefault();
		}

		WhitespaceAndPunctuationAnalyzer analyzer = new WhitespaceAndPunctuationAnalyzer(
				unseparableCharacterSequences, userDefinedSeparatingCharacters, locale);
		
		TokenStream ts = analyzer.tokenStream(null, // our analyzer does not use
													// the fieldname
				new StringReader(content));

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
	
	public Map<String, List<TermInfo>> getTerms() {
		return terms;
	}
	
	
	public List<String> getTermsInOrder() {
		return termsInOrder;
	}
}
