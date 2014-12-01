package de.catma.api;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.restlet.security.MapVerifier;

import de.catma.api.crypto.TokenGenerator;
import de.catma.api.crypto.TokenGeneratorName;

public class TokenEnhancedMapVerifier extends MapVerifier {
	
	private TokenGenerator tokenGenerator;

	public TokenEnhancedMapVerifier() throws NamingException {
		InitialContext context = new InitialContext();
		this.tokenGenerator = 
			(TokenGenerator) context.lookup(TokenGeneratorName.TOKENGENERATOR.name());
	}
	
	@Override
	public int verify(String identifier, char[] secret) {
		int result = super.verify(identifier, secret);
		if (result != RESULT_VALID) {
			if (tokenGenerator.isValid(identifier, secret)) {
				return RESULT_VALID;
			}
		}
		return result;
	}

}
