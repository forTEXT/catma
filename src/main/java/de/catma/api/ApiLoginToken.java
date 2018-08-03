package de.catma.api;

import de.catma.repository.LoginToken;

public class ApiLoginToken implements LoginToken {
	
	private String user;
	
	public ApiLoginToken(String user) {
		this.user = user;
	}

	public void close() {
		/** noop **/
	}

	public Object getUser() {
		return user;
	}

}
