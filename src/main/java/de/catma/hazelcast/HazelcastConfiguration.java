package de.catma.hazelcast;

import static java.util.concurrent.TimeUnit.HOURS;

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
		SIGNUP_TOKEN,
		PROJECT_INVITATION,
		;
	}

	public static enum TopicName {
		PROJECT_INVITATION,
		PROJECT_JOINED,
		COMMENT,
		;
	}

	public final static Configuration<String, String> SIGNUP_TOKEN_CONFIG = 
    		new MutableConfiguration<String, String>()
    		.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(HOURS, 4)));

    public final static Configuration<Integer, String> PROJECT_INVITATION_CONFIG = 
    		new MutableConfiguration<Integer, String>()
    		.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, 30)));

    public final static ClientConfig CLIENT_CONFIG = new ClientConfig().setProperty("hazelcast.client.internal.executor.pool.size", "1");
    
}
