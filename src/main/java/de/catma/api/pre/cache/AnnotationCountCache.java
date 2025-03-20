package de.catma.api.pre.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class AnnotationCountCache {
	
	public static record CacheKey(String identifier, String namespace, String catmaProjectId, String collectionId, String rootRevisionHash) {
		public CacheKey setCollectionId(String collectionId) {
			return new CacheKey(identifier, namespace, catmaProjectId, collectionId, rootRevisionHash);
		}
	}
	
	
	private final Cache<CacheKey, Integer> projects = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
	
	public void put(CacheKey key, Integer annotationCount) {
		projects.put(key, annotationCount);
	}
	
	public Integer get(CacheKey key, Callable<Integer> loader) throws ExecutionException {
		return projects.get(key, loader);
	}
}
