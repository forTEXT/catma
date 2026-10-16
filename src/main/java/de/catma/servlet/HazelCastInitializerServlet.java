package de.catma.servlet;

import java.io.IOException;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.hazelcast.cache.HazelcastCachingProvider;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import de.catma.hazelcast.HazelcastConfiguration;
import de.catma.user.signup.SignupTokenManager;

/**
 * Initializes a Hazelcast node and caches to store account/group/project-signup and project invitation tokens.
 */
public class HazelCastInitializerServlet extends HttpServlet{
	private volatile HazelcastInstance hazelcastNode;

	@Override
    public void init() throws ServletException {
		super.init();

		// start the embedded Hazelcast cluster member
		log("Hazelcast initializing...");

		// we need this to avoid an exception being thrown, the cause being the fact that we have Xerces on the classpath (via xom, TODO: replace)
		// see https://github.com/hazelcast/hazelcast/issues/17839
		// the property only relaxes the XXE protection of the parser Hazelcast uses for its own XML configuration, which comes from the operator, so it
		// has no security impact; it doesn't apply to anything Hazelcast receives over the network, nor to the XML that CATMA parses itself
		System.setProperty("hazelcast.ignoreXxeProtectionFailures", "true");

		// the caches use the member below directly; the default JCache provider would start a separate client with the default configuration,
		// which can't connect to it
		System.setProperty("hazelcast.jcache.provider.type", "member");

		Config config = Config.load();
		config.setClusterName(HazelcastConfiguration.CLUSTER_NAME);

		// only reachable from this host: Hazelcast's open-source edition accepts member and client connections without authentication, and by
		// default listens on all interfaces
		config.setProperty("hazelcast.socket.bind.any", "false");
		config.getNetworkConfig().getInterfaces().setEnabled(true).addInterface(HazelcastConfiguration.MEMBER_HOST);
		config.getNetworkConfig().setPort(HazelcastConfiguration.MEMBER_PORT).setPortAutoIncrement(false);

		// otherwise Hazelcast sends usage statistics about this installation to hazelcast.com
		config.setProperty("hazelcast.phone.home.enabled", "false");

		// we do not operate a cluster, so we can switch off discovery
		config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);
		config.getAdvancedNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);
		
		hazelcastNode = Hazelcast.newHazelcastInstance(config);

		// later calls to getCacheManager() without arguments return this same manager, and therefore use this member too
		CacheManager cacheManager = Caching.getCachingProvider().getCacheManager(
				null, null, HazelcastCachingProvider.propertiesByInstanceItself(hazelcastNode)
		);
		cacheManager.createCache(
				HazelcastConfiguration.CacheKeyName.ACCOUNT_SIGNUP_TOKEN.name(),
				HazelcastConfiguration.ACCOUNT_SIGNUP_TOKEN_CONFIG
		);
		cacheManager.createCache(
				HazelcastConfiguration.CacheKeyName.GROUP_PROJECT_SIGNUP_TOKEN.name(),
				HazelcastConfiguration.GROUP_PROJECT_SIGNUP_TOKEN_CONFIG
		);			
		cacheManager.createCache(
				HazelcastConfiguration.CacheKeyName.PROJECT_INVITATION.name(),
				HazelcastConfiguration.PROJECT_INVITATION_CONFIG
		);
		
		SignupTokenManager signupTokenManager = new SignupTokenManager();
		
		try {			
			signupTokenManager.initGroupProjectSignupTokenCacheTransactionLog();
		}
		catch (IOException e) {
			throw new ServletException("Unable to initialize the transaction log for the group/project singup tokens", e);
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					hazelcastNode.shutdown();
				}
				catch (Exception e) {
					log("Failed to shut down Hazelcast", e);
				}
			}
		});
	}

	@Override
	public void destroy() {
		super.destroy();
		hazelcastNode.shutdown();
	}
}
