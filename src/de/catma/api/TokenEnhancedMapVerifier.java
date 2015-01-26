package de.catma.api;

import javax.naming.NamingException;

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;
import org.restlet.security.MapVerifier;

import de.catma.document.repository.RepositoryPropertyKey;

public class TokenEnhancedMapVerifier extends MapVerifier {

	public TokenEnhancedMapVerifier() throws NamingException {
	}
	
	@Override
	public int verify(String identifier, char[] secret) {
		int result = super.verify(identifier, secret);
		if (result != RESULT_VALID) {
			Totp totp = new Totp(RepositoryPropertyKey.otpsecret.getValue()+identifier, new Clock(120));
			if (totp.verify(new String(secret))) {
				return RESULT_VALID;
			}
		}
		return result;
	}

}
