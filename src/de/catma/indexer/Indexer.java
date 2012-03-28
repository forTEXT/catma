package de.catma.indexer;

import java.util.List;
import java.util.Locale;

import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;

public interface Indexer {
	public void index(
			SourceDocument sourceDocument, 
			List<String> unseparableCharacterSequences,
            List<Character> userDefinedSeparatingCharacters,
            Locale locale) throws Exception;
	
}
