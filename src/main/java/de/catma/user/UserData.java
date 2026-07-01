package de.catma.user;

import java.util.Locale;

public class UserData {

	private String username;
	private String password;
	private String email;
	private String name;
	
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getEmail() {
		return email == null ? null : email.toLowerCase(Locale.ROOT);
	}
	public void setEmail(String email) {
        if (email == null) {
            this.email = null;
            return;
        }
		this.email = email.toLowerCase(Locale.ROOT);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

}