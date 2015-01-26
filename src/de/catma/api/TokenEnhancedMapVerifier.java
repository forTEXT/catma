package de.catma.api;

import java.util.Properties;

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;
import org.restlet.security.MapVerifier;

import de.catma.document.repository.RepositoryPropertyKey;

public class TokenEnhancedMapVerifier extends MapVerifier {
	private Properties properties;

	public TokenEnhancedMapVerifier(Properties properties) {
		super();
		this.properties = properties;
	}
	
	@Override
	public int verify(String identifier, char[] secret) {
		int result = super.verify(identifier, secret);
		if (result != RESULT_VALID) {
			Totp totp = new Totp(
					properties.getProperty(RepositoryPropertyKey.otpSecret.name())+identifier, 
					new Clock(Integer.valueOf(
						properties.getProperty(
							RepositoryPropertyKey.otpDuration.name()))));
			if (totp.verify(new String(secret))) {
				return RESULT_VALID;
			}
		}
		return result;
	}

}
