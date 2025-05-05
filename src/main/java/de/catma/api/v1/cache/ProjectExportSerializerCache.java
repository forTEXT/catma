package de.catma.api.v1.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import de.catma.api.v1.serialization.ProjectExportSerializer;

public class ProjectExportSerializerCache {
	
	public static record CacheKey(String identifier, String namespace, String catmaProjectId) {}; 
	
	
	private final Cache<CacheKey, ProjectExportSerializer> projects = CacheBuilder.newBuilder().maximumSize(30).expireAfterWrite(1, TimeUnit.HOURS).build();
	
	public void put(CacheKey key, ProjectExportSerializer project) {
		projects.put(key, project);
	}
	
	public ProjectExportSerializer get(CacheKey key, Callable<ProjectExportSerializer> loader) throws ExecutionException {
		return projects.get(key, loader);
	}
	
	public void invalidate(CacheKey key) {
		projects.invalidate(key);
	}
}
