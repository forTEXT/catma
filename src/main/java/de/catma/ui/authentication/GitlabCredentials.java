package de.catma.ui.authentication;

import java.time.LocalTime;
import java.util.Objects;

public class GitlabCredentials implements Credentials {

	private final String username;
	private final String email;
	private final String token;
	private final String name;
	private final LocalTime loginTime;
	
	
	public GitlabCredentials(String username, String email, String name, String token){
		this.username=Objects.requireNonNull(username);
		this.email=Objects.requireNonNull(email);
		this.name=Objects.requireNonNull(name);
		this.token=Objects.requireNonNull(token);
		this.loginTime=LocalTime.now();
	}
	
	@Override
	public String getImpersonationToken() {
		return token;
	}

	@Override
	public String getIdentifier() {
		return username;
	}

	@Override
	public LocalTime getLoginTime() {
		return loginTime;
	}

	@Override
	public String getEmail() {
		return email;
	}
	
	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getProvider() {
		return "catma";
	}

	@Override
	public String getName() {
		return name;
	}
}
