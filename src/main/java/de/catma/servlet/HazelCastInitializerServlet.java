package de.catma.servlet;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.HazelcastClientFactory;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.impl.TopicService;

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

	HazelcastInstance hazalcastNode;
	
	@Override
    public void init() throws ServletException {
		super.init();
		
		log("Hazelcast initializing");
        // Start the Embedded Hazelcast Cluster Member.
		hazalcastNode = Hazelcast.newHazelcastInstance();
	    CacheManager manager = Caching.getCachingProvider().getCacheManager();
		
	    manager.createCache(HazelcastConfiguration.CACHE_KEY_SIGNUPTOKEN, HazelcastConfiguration.signupTokenConfiguration);
	    manager.createCache(HazelcastConfiguration.CACHE_KEY_INVITATIONS, HazelcastConfiguration.invitationConfiguration);

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
