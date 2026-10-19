package de.catma.hazelcast;

import javax.cache.CacheManager;

/**
 * Holds the {@link CacheManager} bound to the embedded Hazelcast member, as set up by {@link de.catma.servlet.HazelCastInitializerServlet}.
 * <p>
 * Use this rather than <code>Caching.getCachingProvider().getCacheManager()</code>. That lookup depends on the calling thread's context class
 * loader, and under a different one it starts a second Hazelcast member with the default configuration (cluster name <code>dev</code>, listening
 * on all interfaces), whose caches are empty.
 */
public final class CacheManagerProvider {
	private static volatile CacheManager cacheManager;

	private CacheManagerProvider() {
	}

	public static void set(CacheManager cacheManager) {
		CacheManagerProvider.cacheManager = cacheManager;
	}

	public static CacheManager get() {
		if (cacheManager == null) {
			throw new IllegalStateException("The Hazelcast cache manager hasn't been set up yet");
		}
		return cacheManager;
	}
}
