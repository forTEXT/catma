package de.catma.queryengine;

import java.util.List;
import java.util.Locale;

import de.catma.core.document.repository.Repository;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.Indexer;

public class QueryOptions {
	
	private List<String> relevantSourceDocumentIDs;
	private List<String> relevantUserMarkupCollIDs;
	private List<String> relevantStaticMarkupCollIDs;
	private List<String> unseparableCharacterSequences;
	private List<Character> userDefinedSeparatingCharacters;
	private Locale locale;
	private IndexedRepository repository;
	
	public QueryOptions(List<String> relevantSourceDocumentIDs,
			List<String> relevantUserMarkupCollIDs,
			List<String> relevantStaticMarkupCollIDs,
			List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale,
			IndexedRepository repository) {
		this.relevantSourceDocumentIDs = relevantSourceDocumentIDs;
		this.relevantUserMarkupCollIDs = relevantUserMarkupCollIDs;
		this.relevantStaticMarkupCollIDs = relevantStaticMarkupCollIDs;
		this.unseparableCharacterSequences = unseparableCharacterSequences;
		this.userDefinedSeparatingCharacters = userDefinedSeparatingCharacters;
		this.locale = locale;
		this.repository = repository;
	}

	public List<String> getRelevantSourceDocumentIDs() {
		return relevantSourceDocumentIDs;
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
	
	public List<String> getRelevantStaticMarkupCollIDs() {
		return relevantStaticMarkupCollIDs;
	}
	
	public List<String> getRelevantUserMarkupCollIDs() {
		return relevantUserMarkupCollIDs;
	}
	
	public Repository getRepository() {
		return repository;
	}
	
	public Indexer getIndexer() {
		return repository.getIndexer();
	}
}
