package de.catma.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.Cache.Entry;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.google.gson.Gson;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import de.catma.hazelcast.HazelcastConfiguration;
import de.catma.properties.CATMAPropertyKey;
import de.catma.user.signup.GroupProjectSignupTokenCacheTransactionLogFormatter;
import de.catma.user.signup.GroupSignupToken;
import de.catma.user.signup.ProjectSignupToken;
import de.catma.user.signup.SignupTokenManager;

/**
 * Initializes a Hazelcast node and caches to store account/group/project-signup and project invitation tokens.
 */
@WebServlet(name = "HazelCast", urlPatterns = "/hazelcast", loadOnStartup = 2)
public class HazelCastInitializerServlet extends HttpServlet{
	private volatile HazelcastInstance hazelcastNode;

	@Override
    public void init() throws ServletException {
		super.init();

		// start the embedded Hazelcast cluster member
		log("Hazelcast initializing...");

		// we need this to avoid an exception being thrown, the cause being the fact that we have Xalan on the classpath (via xom, TODO: replace)
		// see https://github.com/hazelcast/hazelcast/issues/17839
		// we don't use Hazelcast to pass any XML messages, and certainly not anything that comes from outside, so there shouldn't be any security impact
		System.setProperty("hazelcast.ignoreXxeProtectionFailures", "true");

		Config config = Config.load();
		
		// we do not operate a cluster, so we can switch of network configuration
		config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);
		config.getAdvancedNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);
		
		hazelcastNode = Hazelcast.newHazelcastInstance(config);

		CacheManager cacheManager = Caching.getCachingProvider().getCacheManager();
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
		
		cacheManager.createCache(
				HazelcastConfiguration.CacheKeyName.API_AUTH.name(), 
				HazelcastConfiguration.API_AUTH_CONFIG);

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
