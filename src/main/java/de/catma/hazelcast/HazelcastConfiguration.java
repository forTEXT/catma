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
public class HazelcastConfiguration {

	public final static String CACHE_KEY_SIGNUPTOKEN = "signupToken";
	public final static String CACHE_KEY_INVITATIONS = "projectInvitation";
	public final static String TOPIC_PROJECT_INVITATIONS = "projectInvitations";
	public final static String TOPIC_PROJECT_JOINED = "projectJoined";

	
    public final static Configuration<String, String> signupTokenConfiguration = 
    		new MutableConfiguration<String, String>()
    		.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(HOURS, 4)));

    public final static Configuration<Integer, String> invitationConfiguration = 
    		new MutableConfiguration<Integer, String>()
    		.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, 30)));

    public final static ClientConfig clientConfig = new ClientConfig().setExecutorPoolSize(1);
    
}
