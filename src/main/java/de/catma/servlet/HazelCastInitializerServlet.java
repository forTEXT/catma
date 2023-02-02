package de.catma.servlet;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import de.catma.hazelcast.HazelcastConfiguration;
/**
 * Initializes a hazelcast node and cache to store signup tokens.
 * Tokens are valid for 4 hours.
 * 
 * @author db
 *
 */
@WebServlet(name = "HazelCast", urlPatterns = "/hazelcast", loadOnStartup = 2)
public class HazelCastInitializerServlet extends HttpServlet{

	private volatile HazelcastInstance hazalcastNode;
	
	@Override
    public void init() throws ServletException {
		super.init();

		// start the embedded Hazelcast cluster member
		log("Hazelcast initializing");

		// we need this to avoid an exception being thrown, the cause being the fact that we have Xalan on the classpath (via xom, TODO: replace)
		// see https://github.com/hazelcast/hazelcast/issues/17839
		// we don't use Hazelcast to pass any XML messages, and certainly not anything that comes from outside, so there shouldn't be any security impact
		System.setProperty("hazelcast.ignoreXxeProtectionFailures", "true");

		hazalcastNode = Hazelcast.newHazelcastInstance();

	    CacheManager manager = Caching.getCachingProvider().getCacheManager();
		
	    manager.createCache(
	    		HazelcastConfiguration.CacheKeyName.SIGNUP_TOKEN.name(), 
	    		HazelcastConfiguration.SIGNUP_TOKEN_CONFIG);
	    manager.createCache(
	    		HazelcastConfiguration.CacheKeyName.PROJECT_INVITATION.name(),
	    		HazelcastConfiguration.PROJECT_INVITATION_CONFIG);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					hazalcastNode.shutdown();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}
	
	@Override
	public void destroy() {
		super.destroy();
		hazalcastNode.shutdown();
	}
}
