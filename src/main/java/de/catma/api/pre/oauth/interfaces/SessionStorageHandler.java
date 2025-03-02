package de.catma.api.pre.oauth.interfaces;

public interface SessionStorageHandler {
	
	public void put(String key, Object value);
	public Object get(String key);

}
