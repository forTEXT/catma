package de.catma.api.v1.oauth;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import de.catma.api.v1.oauth.interfaces.SessionStorageHandler;

public class HttpServletRequestSessionStorageHandler implements SessionStorageHandler {
	@Context
	private HttpServletRequest servletRequest;

	@Override
	public Object getAttribute(String key) {
		return servletRequest.getSession().getAttribute(key);
	}

	@Override
	public void setAttribute(String key, Object value) {
		servletRequest.getSession().setAttribute(key, value);
	}
}
