package de.catma.api.v1.cache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import de.catma.api.v1.backend.interfaces.RemoteGitManagerRestrictedProvider;

public class RemoteGitManagerRestrictedProviderCache {
	
	private final Cache<String, RemoteGitManagerRestrictedProvider> providerCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
	
	public void put(String identifier, RemoteGitManagerRestrictedProvider provider) {
		providerCache.put(identifier, provider);
	}
	
	public RemoteGitManagerRestrictedProvider get(String identifier) throws ExecutionException {
		return providerCache.getIfPresent(identifier);
	}
}
