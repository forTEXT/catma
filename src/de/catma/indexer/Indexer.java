package de.catma.indexer;

import java.util.List;
import java.util.Locale;

public interface Indexer {
	public void index(
			String content, 
			List<String> unseparableCharacterSequences,
            List<Character> userDefinedSeparatingCharacters,
            Locale locale) throws Exception;
	
}
