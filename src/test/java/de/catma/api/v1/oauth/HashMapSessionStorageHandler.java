package de.catma.api.v1.oauth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.catma.api.v1.oauth.interfaces.SessionStorageHandler;

public class HashMapSessionStorageHandler implements SessionStorageHandler {
	private static final Map<String, Object> STORAGE = new ConcurrentHashMap<>();

	@Override
	public Object getAttribute(String key) {
		return STORAGE.get(key);
	}

	@Override
	public void setAttribute(String key, Object value) {
		if (value == null) {
			STORAGE.remove(key);
		}
		else {
			STORAGE.put(key, value);
		}
	}
}
