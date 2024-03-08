package de.catma.hazelcast;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.concurrent.TimeUnit;

import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;

import com.hazelcast.client.config.ClientConfig;

/**
 * Central Hazelcast configuration
 * 
 * @author db
 *
 */
public final class HazelcastConfiguration {
	
	public static enum CacheKeyName {
		/**
		 * account sign up token cache 
		 */
		ACCOUNT_SIGNUP_TOKEN,
		/**
		 * group sign up token cache <br>
		 * project sign up token cache (issued by the inviting user, handled by the invited user, as opposed to the PROJECT_INVITATIONs), no inter-session communication needed
		 *
		 */
		GROUP_PROJECT_SIGNUP_TOKEN,
		/**
		 * cache for project invitations accepted by the invited user that needs to be handled by the inviting user, needs inter-session communication
		 */
		PROJECT_INVITATION,
		API_AUTH,
		;
	}

	/**
	 * publish/subscribe topics for inter-session communication
	 */
	public static enum TopicName {
		// joining projects by code in a live session
		PROJECT_INVITATION, // invited user sends join request -> inviting user executes the necessary permission changes
		PROJECT_JOINED, // inviting user sends info about successful permission changes -> invited user gets informed about having joined a project
		
		// informing other active users about new comments on a document
		COMMENT,
		;
	}

	public final static Configuration<String, String> ACCOUNT_SIGNUP_TOKEN_CONFIG = 
    		new MutableConfiguration<String, String>()
    		.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(HOURS, 4)));

	public final static Configuration<String, String> GROUP_PROJECT_SIGNUP_TOKEN_CONFIG = 
    		new MutableConfiguration<String, String>()
    		.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(DAYS, 7)));

    public final static Configuration<Integer, String> PROJECT_INVITATION_CONFIG = 
    		new MutableConfiguration<Integer, String>()
    		.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(MINUTES, 30)));
    
    public final static Configuration<Integer, String> API_AUTH_CONFIG = 
    		new MutableConfiguration<Integer, String>()
    		.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(HOURS, 12))); //cache entry expiration corresponds to JWT expiration

    public final static ClientConfig CLIENT_CONFIG = new ClientConfig().setProperty("hazelcast.client.internal.executor.pool.size", "1");
    
}
