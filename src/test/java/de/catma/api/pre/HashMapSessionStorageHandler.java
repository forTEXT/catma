package de.catma.api.pre;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.catma.api.pre.oauth.interfaces.SessionStorageHandler;

public class HashMapSessionStorageHandler implements SessionStorageHandler {
	
	private final static Map<String, Object> STORAGE = new ConcurrentHashMap<String, Object>();
	
	@Override
	public void put(String key, Object value) {
		if (value == null) {
			STORAGE.remove(key);
		}
		else {
			STORAGE.put(key, value);
		}
	}

	@Override
	public Object get(String key) {
		return STORAGE.get(key);
	}

}
