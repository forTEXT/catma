package de.catma.api.pre.oauth.interfaces;

public interface SessionStorageHandler {
	Object getAttribute(String key);

	void setAttribute(String key, Object value);
}
