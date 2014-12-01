package de.catma.heureclea.autotagger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import de.catma.api.crypto.TokenGenerator;
import de.catma.api.crypto.TokenGeneratorName;
import de.catma.util.Pair;


public class GenerationOptions {
	
	private String corpusId;
	private TagsetIdentification tagsetIdentification;
	private String token;
	private String identifier;
	
	public GenerationOptions(String corpusId, String identifier) throws NamingException {
		super();
		this.corpusId = corpusId;
		
		InitialContext context = new InitialContext();
		TokenGenerator tokenGenerator = 
				(TokenGenerator) context.lookup(TokenGeneratorName.TOKENGENERATOR.name());
		Pair<String,String> generationResult = tokenGenerator.generateTimestampedKey(identifier);
		this.identifier = generationResult.getFirst();
		this.token = generationResult.getSecond();
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
}
