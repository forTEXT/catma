package de.catma.api.v1.oauth.interfaces;

public interface SessionStorageHandler {
	Object getAttribute(String key);

	void setAttribute(String key, Object value);
}
