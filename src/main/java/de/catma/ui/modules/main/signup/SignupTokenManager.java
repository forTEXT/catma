package de.catma.ui.modules.main.signup;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.Caching;

import com.google.common.base.Splitter;
import com.google.common.eventbus.EventBus;
import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.JsonException;

import de.catma.config.HazelcastConfiguration;
import de.catma.ui.events.TokenInvalidEvent;
import de.catma.ui.events.TokenValidEvent;

/**
 * {@link SignupTokenManager} manages all operations with tokens
 * 
 * @author db
 *
 */
public class SignupTokenManager {

	private final Logger logger = Logger.getLogger(this.getClass().getName());

    private final Cache<String, String> tokenCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(HazelcastConfiguration.CACHE_KEY_SIGNUPTOKEN);

    /**
     * checks if a token exists
     * @param token
     * @return
     */
    public boolean containsToken(String token){
    	Objects.requireNonNull(token);
    	return tokenCache.containsKey(token);
    }
    
    /**
     * puts a token in memory cache
     * @param token
     */
    public void put(SignupToken token){
    	Objects.requireNonNull(token);
    	tokenCache.put(token.getToken(), JsonStream.serialize(token));
    }
    
    /**
     * retrieves a token
     * 
     * @param tokenstring
     * @return token
     */
    public Optional<SignupToken> get(String token){
    	logger.log(Level.INFO, "called with token " + token);
    	Objects.requireNonNull(token);
    	if(!containsToken(token)){
    		return Optional.empty();
    	}
    	String encodedToken = tokenCache.getAndRemove(token);
    	try {
    		return Optional.of(JsonIterator.deserialize(encodedToken, SignupToken.class));
	    } catch (JsonException e){
	    	logger.log(Level.INFO,"Signuptoken corrupt", e);
			return Optional.empty();
		}
    }
    
    /**
     * Parses a verification uri 
     * 
     * @param path
     * @return if URI looks good
     */
    public boolean parseUri(String path){
		if(path != null ){
			Iterator<String> it = Splitter.on("/").split(path).iterator();
			String lastpart = "";
			while(it.hasNext()){
				lastpart = it.next();
			}
			if(lastpart.startsWith("verify")){
				return true;
			}
		}
		return false;
    }
    
    /**
     * Verifies a given token, if it's valid a {@link TokenValidEvent} is send to the eventbus.
     * A {@link TokenInvalidEvent} in case it's invalid.
     * 
     * @param token
     * @param eventBus
     */
    public void handleVerify(String token, EventBus eventBus) {
    	if(token == null || token.isEmpty() ){
    		eventBus.post(new TokenInvalidEvent("token is empty"));
    		return;

    	}
    	if(!containsToken(token)){
    		eventBus.post(new TokenInvalidEvent("token is unknown, it can only be used once."));
    		return;
    	}
    	Optional<SignupToken> signupToken = get(token);
    	if(!signupToken.isPresent()){
    		eventBus.post(new TokenInvalidEvent("tokendata is empty, internal error"));
    		logger.log(Level.WARNING, "token was found but content was null");
    		return;
    	}
    	  
    	eventBus.post(new TokenValidEvent(signupToken.get()));
    	return ;
    }
	
}
