package de.catma.queryengine;

import java.util.List;
import java.util.Locale;

public class QueryOptions {
	
	private List<String> documentIds;
	private List<String> unseparableCharacterSequences;
	private List<Character> userDefinedSeparatingCharacters;
	private Locale locale;

	public QueryOptions(List<String> documentIds,
			List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale) {
		this.documentIds = documentIds;
		this.unseparableCharacterSequences = unseparableCharacterSequences;
		this.userDefinedSeparatingCharacters = userDefinedSeparatingCharacters;
		this.locale = locale;
	}
	
	public List<String> getDocumentIds() {
		return documentIds;
	}

	public List<String> getUnseparableCharacterSequences() {
		return unseparableCharacterSequences;
	}

	public List<Character> getUserDefinedSeparatingCharacters() {
		return userDefinedSeparatingCharacters;
	}

	public Locale getLocale() {
		return locale;
	}
	
	
	
	

}
