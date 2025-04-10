package de.catma.api.pre.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import de.catma.api.pre.PreProject;

public class ProjectCache {
	
	public static record CacheKey(String identifier, String namespace, String catmaProjectId) {}; 
	
	
	private final Cache<CacheKey, PreProject> projects = CacheBuilder.newBuilder().maximumSize(30).expireAfterWrite(1, TimeUnit.HOURS).build();
	
	public void put(CacheKey key, PreProject project) {
		projects.put(key, project);
	}
	
	public PreProject get(CacheKey key, Callable<PreProject> loader) throws ExecutionException {
		return projects.get(key, loader);
	}
	
	public void invalidate(CacheKey key) {
		projects.invalidate(key);
	}
}
