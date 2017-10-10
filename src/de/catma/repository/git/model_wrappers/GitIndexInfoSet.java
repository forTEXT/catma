package de.catma.repository.git.model_wrappers;

import com.jsoniter.annotation.JsonIgnore;
import de.catma.document.source.IndexInfoSet;

import java.util.List;
import java.util.Locale;

public class GitIndexInfoSet {
	private IndexInfoSet indexInfoSet;

	public GitIndexInfoSet() {
		this.indexInfoSet = new IndexInfoSet();
	}

	public GitIndexInfoSet(IndexInfoSet indexInfoSet) {
		this.indexInfoSet = indexInfoSet;
	}

	@JsonIgnore
	public IndexInfoSet getIndexInfoSet() {
		return this.indexInfoSet;
	}

	public String getLocale() {
		Locale locale = this.indexInfoSet.getLocale();
		return locale.toLanguageTag();
	}

	public void setLocale(String languageTag) {
		this.indexInfoSet.setLocale(Locale.forLanguageTag(languageTag));
	}

	public List<String> getUnseparableCharacterSequences() {
		return this.indexInfoSet.getUnseparableCharacterSequences();
	}

	public void setUnseparableCharacterSequences(List<String> unseparableCharacterSequences) {
		this.indexInfoSet.setUnseparableCharacterSequences(unseparableCharacterSequences);
	}

	public List<Character> getUserDefinedSeparatingCharacters() {
		return this.indexInfoSet.getUserDefinedSeparatingCharacters();
	}

	public void setUserDefinedSeparatingCharacters(List<Character> userDefinedSeparatingCharacters) {
		this.indexInfoSet.setUserDefinedSeparatingCharacters(userDefinedSeparatingCharacters);
	}
}
