package de.catma.heureclea.autotagger;


public class GenerationOptions {
	
	public String corpusId;
	public TagsetIdentification tagsetIdentification;
	
	public GenerationOptions(String corpusId) {
		super();
		this.corpusId = corpusId;
	}
	public String getCorpusId() {
		return corpusId;
	}
	public void setCorpusId(String corpusId) {
		this.corpusId = corpusId;
	}
	public TagsetIdentification getTagsetIdentification() {
		return tagsetIdentification;
	}
	public void setTagsetIdentification(TagsetIdentification tagsetIdentification) {
		this.tagsetIdentification = tagsetIdentification;
	}
}
