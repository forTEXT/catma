package de.catma.api.crypto;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import de.catma.util.Pair;

public class TokenGenerator {
	private AES aes;
	private Cache<String, String> tokens;
	private Logger logger = Logger.getLogger(TokenGenerator.class.getName());
	
	public TokenGenerator() {
		aes = new AES();
		tokens = CacheBuilder.newBuilder()
						.expireAfterWrite(1, TimeUnit.MINUTES)
						.build();
	}
	
	public String generateKey(String value) {
		String key = aes.encrypt(value);
		tokens.put(key, value);
		
		return key;
	}
	
	public boolean isValid(String value, char[] key) {
		String checkKey = aes.encrypt(value);
		tokens.cleanUp();
		if (new String(key).equals(checkKey)) {
			if (tokens.getIfPresent(checkKey) != null) {
				tokens.invalidate(checkKey);
				
				if (tokens.size() == 0) {
					aes = new AES();
				}
				
				return true;
			}
			else {
				logger.warning("received unknown key for " + value);
			}
		}
		else {
			logger.warning("received invalid key for " + value);
		}
		
		return false;
	}

	public Pair<String,String> generateTimestampedKey(String identifier) {
		String timestampedIdentifier = new Date().getTime() + ".." + identifier;
		return new Pair<>(timestampedIdentifier, generateKey(timestampedIdentifier));
	}
	
	public String getIdentifier(String timestampedValue) {
		if (timestampedValue.contains("..")) {
			return timestampedValue.substring(timestampedValue.indexOf("..")+2);
		}
		else {
			return timestampedValue;
		}
	}
}
