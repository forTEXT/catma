package de.catma.ui.modules.main.signup;

import com.jsoniter.annotation.JsonCreator;
import com.jsoniter.annotation.JsonProperty;

/**
 * Signup token bean class. It can be marshalled to json and back. This is useful for verification purpose.
 * @author db
 *
 */
public class SignupToken {

	private final String requestDate;
	private final String email;
	private final String token;
	
	@JsonCreator
	public SignupToken(
			@JsonProperty("requestDate") String requestDate, 
			@JsonProperty("email")String email, 
			@JsonProperty("token")String token) {
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
