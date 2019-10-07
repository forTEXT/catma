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
@WebServlet(name = "HazelCast", urlPatterns = "/hazelcast", loadOnStartup = 3)
public class HazelCastInitializerServlet extends HttpServlet{

	private volatile HazelcastInstance hazalcastNode;
	
	@Override
    public void init() throws ServletException {
		super.init();
		
		log("Hazelcast initializing");
        // Start the Embedded Hazelcast Cluster Member.
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
