package de.catma.api.pre;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

public class PreSecurityContext implements SecurityContext {
	
	private static class PreSecurityPrincipal implements Principal {

		private final String name;
		
		public PreSecurityPrincipal(String name) {
			super();
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}
		
	}
	
	private final Principal principal;
	private final boolean secure;
	private final String authenticationScheme;
	
	public PreSecurityContext(String identifier, boolean secure, String authenticationScheme) {
		super();
		this.principal = new PreSecurityPrincipal(identifier);
		this.secure = secure;
		this.authenticationScheme = authenticationScheme;
	}

	@Override
	public Principal getUserPrincipal() {
		return principal;
	}

	@Override
	public boolean isUserInRole(String role) {
		return true; // we have project/group based roles which cannot be answered by this context
	}

	@Override
	public boolean isSecure() {
		return secure;
	}

	@Override
	public String getAuthenticationScheme() {
		return authenticationScheme;
	}

}
