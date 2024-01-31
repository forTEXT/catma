package de.catma.user.signup;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.Caching;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.mail.EmailException;

import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import de.catma.hazelcast.HazelcastConfiguration;
import de.catma.properties.CATMAPropertyKey;
import de.catma.user.Group;
import de.catma.user.UserData;
import de.catma.util.MailSender;

/**
 * Manages all operations with signup tokens
 * 
 * @author db
 *
 */
public class SignupTokenManager {
	
	public static enum TokenAction {
		none, // default, no action in path
		verify, // verify sign up for CATMA accounts
		joingroup, // join a user group
		;

		static TokenAction findAction(String pathAction) {
			for (TokenAction action : values()) {
				if (Objects.equals(action.name(), pathAction)) {
					return action;
				}
					
			}
			return none;
		}
	}
	
	public static interface TokenValidityHandler<T> {
		public void tokenValid(T token);
		public void tokenInvalid(String reason);
	}

	private final Logger logger = Logger.getLogger(this.getClass().getName());

    private final Cache<String, String> accountSignupTokenCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(
    				HazelcastConfiguration.CacheKeyName.ACCOUNT_SIGNUP_TOKEN.name());

    private final Cache<String, String> groupSignupTokenCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(
    				HazelcastConfiguration.CacheKeyName.GROUP_SIGNUP_TOKEN.name());

    /**
     * checks if a token exists
     * @param token
     * @return
     */
    public boolean containsAccountSignupToken(String token){
    	Objects.requireNonNull(token);
    	return accountSignupTokenCache.containsKey(token);
    }

    public boolean containsGroupSignupToken(String token){
    	Objects.requireNonNull(token);
    	return groupSignupTokenCache.containsKey(token);
    }
    
    /**
     * puts a token in memory cache
     * @param token
     */
    public void put(AccountSignupToken token){
    	Objects.requireNonNull(token);
    	accountSignupTokenCache.put(token.token(), new Gson().toJson(token));
    }

    /**
     * puts a token in memory cache
     * @param token
     */
    public void put(GroupSignupToken token){
    	Objects.requireNonNull(token);
    	groupSignupTokenCache.put(token.token(), new Gson().toJson(token));
    }
    
    /**
     * retrieves a token
     * 
     * @param tokenstring
     * @return token
     */
    public Optional<AccountSignupToken> getAccountSignupToken(String token){
    	logger.info("SignupTokenManager.getAccountSignupToken called with token " + token);
    	Objects.requireNonNull(token);
    	if(!containsAccountSignupToken(token)){
    		return Optional.empty();
    	}
    	String encodedToken = accountSignupTokenCache.getAndRemove(token);
    	try {
    		return Optional.of(new Gson().fromJson(encodedToken, AccountSignupToken.class));
	    } catch (JsonSyntaxException e){
	    	logger.log(Level.WARNING,"Signup token corrupt", e);
			return Optional.empty();
		}
    }

    public Optional<GroupSignupToken> getGroupSignupToken(String token){
    	logger.info("SignupTokenManager.getGroupSignupToken called with token " + token);
    	Objects.requireNonNull(token);
    	if(!containsGroupSignupToken(token)){
    		return Optional.empty();
    	}
    	String encodedToken = groupSignupTokenCache.getAndRemove(token);
    	try {
    		return Optional.of(new Gson().fromJson(encodedToken, GroupSignupToken.class));
	    } catch (JsonSyntaxException e){
	    	logger.log(Level.WARNING,"Signup token corrupt", e);
			return Optional.empty();
		}
    }
    /**
     * Checks whether the given path is for account sign up verification 
     * 
     * @param path
     * @return true if path ends with '{@link TokenAction#verify verify}', false otherwise
     */
    public TokenAction getTokenActionFromPath(String path){
		if(path != null ){
			Iterator<String> it = Splitter.on("/").split(path).iterator();
			String lastPart = "";
			while(it.hasNext()){
				lastPart = it.next();
			}
			
			return TokenAction.findAction(lastPart);
		}
		return TokenAction.none;
    }
    
    /**
     * Verifies a given token, if it's valid a {@link TokenValidityHandler#tokenValid(AccountSignupToken)} is called with the valid signup token 
     * otherwise {@link TokenValidityHandler#tokenInvalid(String)} is called with the reason for the invalidity.
     * 
     * @param token
     * @param tokenValidityHandler callback for verification result
     */
    public void validateAccountSignupToken(String token, TokenValidityHandler<AccountSignupToken> tokenValidityHandler) {
    	if(token == null || token.isEmpty() ){
    		tokenValidityHandler.tokenInvalid("Token is empty. Please sign up again!");
    		return;

    	}
    	if(!containsAccountSignupToken(token)){
    		tokenValidityHandler.tokenInvalid("Token is unknown, it can only be used once. Please sign up again!");
    		return;
    	}
    	Optional<AccountSignupToken> signupToken = getAccountSignupToken(token);
    	if(!signupToken.isPresent()){
    		tokenValidityHandler.tokenInvalid("Token is corrupt. Please sign up again!");
    		logger.warning("Token was found but content was null");
    		return;
    	}
    	  
    	tokenValidityHandler.tokenValid(signupToken.get());
    	return ;
    }

    public void validateGroupSignupToken(String token, TokenValidityHandler<GroupSignupToken> tokenValidityHandler) {
    	if(token == null || token.isEmpty() ){
    		tokenValidityHandler.tokenInvalid("Token is empty. Please contact the group owner to get a new link!");
    		return;

    	}
    	if(!containsGroupSignupToken(token)){
    		tokenValidityHandler.tokenInvalid("Token is unknown, it can only be used once. Please contact the group owner to get a new link!");
    		return;
    	}
    	Optional<GroupSignupToken> signupToken = getGroupSignupToken(token);
    	if(!signupToken.isPresent()){
    		tokenValidityHandler.tokenInvalid("Token is corrupt. Please contact the group owner to get a new link!");
    		logger.warning("Token was found but content was null");
    		return;
    	}
    	  
    	tokenValidityHandler.tokenValid(signupToken.get());
    	return ;
    }
    
    private String createHmacSha256Token(String digest) {
    	return new HmacUtils(HmacAlgorithms.HMAC_SHA_256, CATMAPropertyKey.SIGNUP_TOKEN_KEY.getValue()).hmacHex(digest);
    }
    
    /**
     * Sign up verification process:<br>
     * <ol>
     * <li>An HmacSha256 token is created that contains the users's email address as its digest.</li>
     * <li>A verification link to CATMA containing this token is sent to the user's email address.</li>
     * <li>When the user clicks on the link CATMA receives the token and verifies that the token is valid and has been issued by CATMA (handleVerify)</li>
     * <li>A valid token brings up the account creation dialog with the user's email being preset already.</li>
     *  </ol>
     * @param userData basically the user's email address
     * @throws EmailException in case something goes wrong when sending the email
     */
    public void sendAccountSignupVerificationEmail(UserData userData) throws EmailException {
		
		// create a token based on the user's email address
		String token = createHmacSha256Token(userData.getEmail());
		put(new AccountSignupToken(LocalTime.now().toString(), userData.getEmail(), token));

		String encToken = URLEncoder.encode(token, StandardCharsets.UTF_8); // although there are better alternatives, we stick to the java.net encoder to minimize dependencies
		String verificationUrl = CATMAPropertyKey.BASE_URL.getValue().trim() + TokenAction.verify.name() + "?token=" + encToken;

		// send verification link that contains the generated token
		// this verification link brings the user back to CATMA and it brings up the user account creation dialog if the token can be verified, see handleVerify above and CatmaApplication#handleRequestToken 
		MailSender.sendMail(userData.getEmail(), "CATMA Email Verification", "Please visit the following link in order to verify your email address and complete your sign up:\n" + verificationUrl);
		logger.info(
				String.format("Generated a new signup token for %s, the full verification URL is: %s", userData.getEmail(), verificationUrl)
		);

    }

	public void sendGroupSignupEmail(String address, Group group) throws EmailException {
		// create a token based on the user's email address
		Long groupId = group.getId();
		String token = createHmacSha256Token(address+groupId);
		put(new GroupSignupToken(LocalTime.now().toString(), address, group.getId(), token));

		String encToken = URLEncoder.encode(token, StandardCharsets.UTF_8); // although there are better alternatives, we stick to the java.net encoder to minimize dependencies
		String joinUrl = CATMAPropertyKey.BASE_URL.getValue().trim() + TokenAction.joingroup.name() + "?token=" + encToken;

		// send join-group-link that contains the generated token
		// this verification link brings the user back to CATMA and it brings up the user account creation dialog if the token can be verified, see handleVerify above and CatmaApplication#handleRequestToken 
		MailSender.sendMail(address, String.format("CATMA Join Group %s", group.getName()), String.format("Please visit the following link in order to join the group %s:\n%s", group.getName(), joinUrl));
		logger.info(
				String.format("Generated a new join-group-token for %s, the full join URL is: %s", address, joinUrl)
		);		
	}
}
