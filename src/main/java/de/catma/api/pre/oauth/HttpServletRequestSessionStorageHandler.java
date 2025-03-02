package de.catma.api.pre.oauth;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import de.catma.api.pre.oauth.interfaces.SessionStorageHandler;

public class HttpServletRequestSessionStorageHandler implements SessionStorageHandler {
	
	@Context
	private HttpServletRequest servletRequest;    

	@Override
	public void put(String key, Object value) {
		servletRequest.getSession().setAttribute(key, value);
	}

	@Override
	public Object get(String key) {
		return servletRequest.getSession().getAttribute(key);
	}

}
