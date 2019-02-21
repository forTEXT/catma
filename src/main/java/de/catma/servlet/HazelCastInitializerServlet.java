package de.catma.servlet;

import static java.util.concurrent.TimeUnit.HOURS;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
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
		
	    MutableConfiguration<String, String> configuration = 
	    		new MutableConfiguration<String, String>()
	    		.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(HOURS, 4)));
	    // Get a Cache called "myCache" and configure with 1 minute expiry
	    manager.createCache("signuptokens", configuration);

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
