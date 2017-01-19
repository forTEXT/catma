package de.catma.heureclea.autotagger;

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;

import de.catma.document.repository.RepositoryPropertyKey;


public class GenerationOptions {
	
	private String corpusId;
	private TagsetIdentification tagsetIdentification;
	private String token;
	private String identifier;
	private String sourceDocId;

	public GenerationOptions(String corpusId, String identifier) {
		super();
		this.corpusId = corpusId;
		
		Totp totp = new Totp(
			RepositoryPropertyKey.otpSecret.getValue()+identifier, 
			new Clock(Integer.valueOf(RepositoryPropertyKey.otpDuration.getValue())));
		
		this.identifier = identifier;
		this.token = totp.now();
	}
	
	public GenerationOptions(String sourceDocId, String corpusId, String identifier) {
		this(corpusId, identifier);
		this.sourceDocId = sourceDocId;
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
	
	
	public String getToken() {
		return token;
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	public String getApiURL() {
		return RepositoryPropertyKey.BaseURL.getValue().trim()+"api";
	}
	public String getSourceDocId() {
		return sourceDocId;
	}
}
