package de.catma.queryengine;

import java.util.List;
import java.util.Locale;

import de.catma.core.document.repository.Repository;
import de.catma.indexer.Indexer;

public class QueryOptions {
	
	private List<String> relevantSourceDocumentIDs;
	private List<String> relevantUserMarkupCollIDs;
	private List<String> relevantStaticMarkupCollIDs;
	private List<String> unseparableCharacterSequences;
	private List<Character> userDefinedSeparatingCharacters;
	private Locale locale;
	private Repository repository;
	private Indexer indexer;
	
	public QueryOptions(List<String> relevantSourceDocumentIDs,
			List<String> relevantUserMarkupCollIDs,
			List<String> relevantStaticMarkupCollIDs,
			List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale,
			Repository repository,
			Indexer indexer) {
		this.relevantSourceDocumentIDs = relevantSourceDocumentIDs;
		this.relevantUserMarkupCollIDs = relevantUserMarkupCollIDs;
		this.relevantStaticMarkupCollIDs = relevantStaticMarkupCollIDs;
		this.unseparableCharacterSequences = unseparableCharacterSequences;
		this.userDefinedSeparatingCharacters = userDefinedSeparatingCharacters;
		this.locale = locale;
		this.repository = repository;
		this.indexer = indexer;
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
		return indexer;
	}
}
