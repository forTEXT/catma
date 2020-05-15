package de.catma.ui.module.main.signup;

/**
 * Signup token bean class. It can be marshalled to json and back. This is useful for verification purpose.
 * @author db
 *
 */
public class SignupToken {

	private String requestDate;
	private String email;
	private String token;
	
	public SignupToken() {
	}
	
	public SignupToken(
			String requestDate, 
			String email, 
			String token) {
		this.requestDate = requestDate;
		this.email = email;
		this.token = token;
	}

	public String getRequestDate() {
		return requestDate;
	}

	public String getEmail() {
		return email;
	}

	public String getToken() {
		return token;
	}


	@Override
	public String toString() {
		return "SignupToken [requestDate=" + requestDate + ", email=" + email + ", token=" + token + "]";
	}
}
