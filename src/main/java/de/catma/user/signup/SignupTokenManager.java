package de.catma.user.signup;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.FileHandler;
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
import de.catma.project.ProjectReference;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACRole;
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
		joinproject, // join a project
		;

		public static TokenAction findAction(String actionIdentifier) {
			for (TokenAction action : values()) {
				if (Objects.equals(action.name(), actionIdentifier)) {
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
	
	private final static Logger CACHETRANSACTIONLOGGER = 
			Logger.getLogger(HazelcastConfiguration.CacheKeyName.class.getName()+"."+HazelcastConfiguration.CacheKeyName.GROUP_PROJECT_SIGNUP_TOKEN);
	private final static char ADD_TRANSACTION_FLAG = '1';
	private final static char REMOVE_TRANSACTION_FLAG = '0';
	private final static char GROUP_TRANSACTION_FLAG = 'G';
	private final static char PROJECT_TRANSACTION_FLAG = 'P';
	private final static char TRANSACTION_COLUMN_SEPARATOR = ';';
	
    private final Cache<String, String> accountSignupTokenCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(
    				HazelcastConfiguration.CacheKeyName.ACCOUNT_SIGNUP_TOKEN.name());

    private final Cache<String, String> groupProjectSignupTokenCache = 
    		Caching.getCachingProvider().getCacheManager().getCache(
    				HazelcastConfiguration.CacheKeyName.GROUP_PROJECT_SIGNUP_TOKEN.name());

    private final HashSet<String> consumedTokens = new HashSet<String>();
    
    /**
     * checks if a token exists
     * @param token
     * @return
     */
    private boolean containsAccountSignupToken(String token){
    	Objects.requireNonNull(token);
    	return accountSignupTokenCache.containsKey(token);
    }

    private boolean containsGroupOrProjectSignupToken(String token){
    	Objects.requireNonNull(token);
    	return groupProjectSignupTokenCache.containsKey(token);
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
    	String encodedToken = new Gson().toJson(token);
    	put(token.token(), encodedToken, GROUP_TRANSACTION_FLAG);
    }
    
    private void put(String key, String encodedToken, char groupOrProjectTransactionFlag) {
    	groupProjectSignupTokenCache.put(key, encodedToken);
		consumedTokens.remove(key); // the token might have been re-added and can be consumed again
    	CACHETRANSACTIONLOGGER.info(String.format("%c%c%c%c%s", groupOrProjectTransactionFlag, TRANSACTION_COLUMN_SEPARATOR, ADD_TRANSACTION_FLAG, TRANSACTION_COLUMN_SEPARATOR, encodedToken));    	
    }
    
    /**
     * puts a token in memory cache
     * @param token
     */
    public void put(ProjectSignupToken token){
    	Objects.requireNonNull(token);
    	String encodedToken = new Gson().toJson(token);
    	put(token.token(), encodedToken, PROJECT_TRANSACTION_FLAG);
    }
    
    /**
     * retrieves a token
     * 
     * @param tokenstring
     * @return token
     */
    private Optional<AccountSignupToken> getAccountSignupToken(String token){
    	logger.info("SignupTokenManager.getAccountSignupToken called with token " + token);
    	Objects.requireNonNull(token);
    	if(!containsAccountSignupToken(token)){
    		return Optional.empty();
    	}
    	String encodedToken = accountSignupTokenCache.getAndRemove(token);
		// prevent the same token from being processed again, eg: on refresh, as request parameters remain even though we call replaceState to update the URL
		// (see RequestTokenHandler)
    	consumedTokens.add(token);
    	
    	try {
    		return Optional.of(new Gson().fromJson(encodedToken, AccountSignupToken.class));
	    } catch (JsonSyntaxException e){
	    	logger.log(Level.WARNING,"Signup token corrupt", e);
			return Optional.empty();
		}
    }

    private Optional<GroupSignupToken> getGroupSignupToken(String token){
    	logger.info("SignupTokenManager.getGroupSignupToken called with token " + token);
    	Objects.requireNonNull(token);
    	if(!containsGroupOrProjectSignupToken(token)){
    		return Optional.empty();
    	}
    	String encodedToken = groupProjectSignupTokenCache.getAndRemove(token);
		// prevent the same token from being processed again, eg: on refresh, as request parameters remain even though we call replaceState to update the URL
		// (see RequestTokenHandler)
    	consumedTokens.add(token);
    	
    	CACHETRANSACTIONLOGGER.info(String.format("%c%c%c%c%s", GROUP_TRANSACTION_FLAG, TRANSACTION_COLUMN_SEPARATOR, REMOVE_TRANSACTION_FLAG, TRANSACTION_COLUMN_SEPARATOR, token));

    	try {
    		return Optional.of(new Gson().fromJson(encodedToken, GroupSignupToken.class));
	    } catch (JsonSyntaxException e){
	    	logger.log(Level.WARNING,"Signup token corrupt", e);
			return Optional.empty();
		}
    }
    
    private Optional<ProjectSignupToken> getProjectSignupToken(String token){
    	logger.info("SignupTokenManager.getProjectSignupToken called with token " + token);
    	Objects.requireNonNull(token);
    	if(!containsGroupOrProjectSignupToken(token)){
    		return Optional.empty();
    	}
    	String encodedToken = groupProjectSignupTokenCache.getAndRemove(token);
		// prevent the same token from being processed again, eg: on refresh, as request parameters remain even though we call replaceState to update the URL
		// (see RequestTokenHandler)
    	consumedTokens.add(token);
    	
    	CACHETRANSACTIONLOGGER.info(String.format("%c%c%c%c%s", PROJECT_TRANSACTION_FLAG, TRANSACTION_COLUMN_SEPARATOR, REMOVE_TRANSACTION_FLAG, TRANSACTION_COLUMN_SEPARATOR, token));

    	try {
    		return Optional.of(new Gson().fromJson(encodedToken, ProjectSignupToken.class));
	    } catch (JsonSyntaxException e){
	    	logger.log(Level.WARNING,"Signup token corrupt", e);
			return Optional.empty();
		}
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
    	
    	if (consumedTokens.contains(token)) {
    		return; //ignore silently
			// TODO: consider adding another function to TokenValidityHandler like 'noOp' or 'finally' so that the caller can handle these cases and act
			//       appropriately, eg: call replaceState to clean up the URL (which doesn't currently happen if a token URL is pasted into a session where
			//       the token has already been consumed; also applies to other validate... functions below)
    	}
    	
    	if(!containsAccountSignupToken(token)){
    		tokenValidityHandler.tokenInvalid("Token is unknown, it can only be used once. Please sign up again!");
			consumedTokens.add(token); // prevent the same message from being shown again, eg: on refresh, also see getters above
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
    	
    	if (consumedTokens.contains(token)) {
    		return; //ignore silently
    	}
    	
    	if(!containsGroupOrProjectSignupToken(token)){
    		tokenValidityHandler.tokenInvalid("Token is unknown, it can only be used once. Please contact the group owner to get a new link!");
			consumedTokens.add(token); // prevent the same message from being shown again, eg: on refresh, also see getters above
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

    
    public void validateProjectSignupToken(String token, TokenValidityHandler<ProjectSignupToken> tokenValidityHandler) {
    	if(token == null || token.isEmpty() ){
    		tokenValidityHandler.tokenInvalid("Token is empty. Please contact the project owner/maintainer to get a new link!");
    		return;

    	}
    	
    	if (consumedTokens.contains(token)) {
    		return; //ignore silently
    	}
    	
    	if(!containsGroupOrProjectSignupToken(token)){
    		tokenValidityHandler.tokenInvalid("Token is unknown, it can only be used once. Please contact the project owner/maintainer to get a new link!");
			consumedTokens.add(token); // prevent the same message from being shown again, eg: on refresh, also see getters above
    		return;
    	}
    	Optional<ProjectSignupToken> signupToken = getProjectSignupToken(token);
    	if(!signupToken.isPresent()){
    		tokenValidityHandler.tokenInvalid("Token is corrupt. Please contact the project owner/maintainer to get a new link!");
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
		String verificationUrl = CATMAPropertyKey.BASE_URL.getValue().trim() +"?action=" + TokenAction.verify.name() + "&token=" + encToken;

		// send verification link that contains the generated token
		// this verification link brings the user back to CATMA and it brings up the user account creation dialog if the token can be verified, see handleVerify above and CatmaApplication#handleRequestToken 
		MailSender.sendMail(userData.getEmail(), "CATMA Email Verification", "Please visit the following link in order to verify your email address and complete your sign up:\n" + verificationUrl);
		logger.info(
				String.format("Generated a new signup token for %s, the full verification URL is: %s", userData.getEmail(), verificationUrl)
		);

    }

	public void sendGroupSignupEmail(String address, Group group, LocalDate expiresAt) throws EmailException {
		// create a token based on the user's email address
		Long groupId = group.getId();
		String token = createHmacSha256Token(address+groupId);
		put(new GroupSignupToken(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), address, group.getId(), group.getName(), token, expiresAt==null?null:expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE)));

		String encToken = URLEncoder.encode(token, StandardCharsets.UTF_8); // although there are better alternatives, we stick to the java.net encoder to minimize dependencies
		// action is send as a param to keep redirect_url management in oauth simple
		String joinUrl = CATMAPropertyKey.BASE_URL.getValue().trim() + "?action=" +TokenAction.joingroup.name() + "&token=" + encToken;

		// send join-group-link that contains the generated token
		// this verification link brings the user back to CATMA and it brings up the user account creation dialog if the token can be verified, see handleVerify above and CatmaApplication#handleRequestToken 
		MailSender.sendMail(address, String.format("CATMA Join Group %s", group.getName()), String.format("Please visit the following link in order to join the group %s:\n%s", group.getName(), joinUrl));
		logger.info(
				String.format("Generated a new join-group-token for %s, the full join URL is: %s", address, joinUrl)
		);		
	}
	
	public void sendProjectSignupEmail(String address, ProjectReference project, RBACRole role, LocalDate expiresAt) throws EmailException {
		// create a token based on the user's email address
		String projectId = project.getProjectId();
		String token = createHmacSha256Token(address+projectId);
		put(new ProjectSignupToken(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), address, project.getNamespace(), projectId, project.getName(), role, token, expiresAt==null?null:expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE)));

		String encToken = URLEncoder.encode(token, StandardCharsets.UTF_8); // although there are better alternatives, we stick to the java.net encoder to minimize dependencies
		// action is send as a param to keep redirect_url management in oauth simple
		String joinUrl = CATMAPropertyKey.BASE_URL.getValue().trim() + "?action=" + TokenAction.joinproject.name() + "&token=" + encToken;

		// send join-group-link that contains the generated token
		// this verification link brings the user back to CATMA and it brings up the user account creation dialog if the token can be verified, see handleVerify above and CatmaApplication#handleRequestToken 
		MailSender.sendMail(address, String.format("CATMA Join Project %s", project.getName()), String.format("Please visit the following link in order to join the project %s:\n%s", project.getName(), joinUrl));
		logger.info(
				String.format("Generated a new join-project-token for %s, the full join URL is: %s", address, joinUrl)
		);		
	}

	public void initGroupProjectSignupTokenCacheTransactionLog() throws IOException {
		String groupProjectSignupTokenTransactionLogPath = CATMAPropertyKey.GROUP_PROJECT_SIGNUP_TOKEN_CACHE_TRANSACTION_LOG_PATH_PATTERN.getValue();

		Path tLogPathPattern = Paths.get(groupProjectSignupTokenTransactionLogPath);
		
		Map<String, Runnable> tokenReAddMap = new HashMap<String, Runnable>();
		
		Path tLogPathDir = tLogPathPattern.toAbsolutePath().getParent();
		int namePartEnd = tLogPathPattern.getFileName().toString().indexOf('%');
		namePartEnd = (namePartEnd==-1)?tLogPathPattern.getFileName().toString().length():namePartEnd;
		String nameStart = tLogPathPattern.getFileName().toString().substring(0, namePartEnd);
		
		if (tLogPathDir.toFile().exists()) {
			String[] transactionLogFiles = tLogPathDir.toFile().list((dir, name) -> name.startsWith(nameStart));

			for (String transactionLogFileName : transactionLogFiles) {
				File transactionLogFile = new File(tLogPathDir.toString(), transactionLogFileName);
				List<String> transactions = Files.readAllLines(Path.of(transactionLogFile.toURI()));
				LocalDateTime now = LocalDateTime.now();
				for (String transaction : transactions) {
					try {
						boolean isRemoval = transaction.charAt(2) == REMOVE_TRANSACTION_FLAG;
						boolean isGroupTransaction = transaction.charAt(0) == GROUP_TRANSACTION_FLAG;
						String encodedToken = transaction.substring(4);
						if (isRemoval) {
							String token = transaction.substring(4);
							tokenReAddMap.remove(token);
						}
						else if (isGroupTransaction) {
							GroupSignupToken groupSignupToken = new Gson().fromJson(encodedToken, GroupSignupToken.class);
							if (LocalDateTime.parse(groupSignupToken.requestDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).plus(7, ChronoUnit.DAYS).isAfter(now)) {
								tokenReAddMap.put(groupSignupToken.token(), () -> put(groupSignupToken.token(), encodedToken, GROUP_TRANSACTION_FLAG));
							}
						}
						else {
							ProjectSignupToken projectSignupToken = new Gson().fromJson(encodedToken, ProjectSignupToken.class);
							if (LocalDateTime.parse(projectSignupToken.requestDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).plus(7, ChronoUnit.DAYS).isAfter(now)) {
								tokenReAddMap.put(projectSignupToken.token(), () -> put(projectSignupToken.token(), encodedToken, PROJECT_TRANSACTION_FLAG));
							}
						}
					}
					catch(Exception e) {
						logger.warning(String.format("Found unexpected line during scanning of transaction log: %s", transaction));
					}
				}
				transactionLogFile.delete();
			}

			
			
		}			
		
		FileHandler transactionLogHandler = new FileHandler(
				groupProjectSignupTokenTransactionLogPath, 
				CATMAPropertyKey.GROUP_PROJECT_SIGNUP_TOKEN_CACHE_TRANSACTION_LOG_SIZE_LIMIT_IN_BYTES.getIntValue(),
				CATMAPropertyKey.GROUP_PROJECT_SIGNUP_TOKEN_CACHE_TRANSACTION_LOG_COUNT.getIntValue());
		
		transactionLogHandler.setFormatter(new GroupProjectSignupTokenCacheTransactionLogFormatter());
		CACHETRANSACTIONLOGGER.addHandler(transactionLogHandler);
		
		
		tokenReAddMap.values().forEach(reAddAction -> reAddAction.run());
	}
}
