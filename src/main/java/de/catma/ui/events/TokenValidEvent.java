package de.catma.ui.events;

import de.catma.ui.module.main.auth.SignupToken;

/**
 * Event fired when a valid token has been submitted.
 * @author db
 *
 */
public class TokenValidEvent {

	private final SignupToken signupToken;
	
	public TokenValidEvent(SignupToken signupToken ) {
		this.signupToken = signupToken;
	}
	
	public SignupToken getSignupToken() {
		return signupToken;
	}
	
}
