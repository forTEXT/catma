package de.catma.repository.git.managers.interfaces;

import java.io.IOException;

public interface GitUserInformationProvider {
	String getUsername();

	String getPassword();

	String getEmail();

	void refreshUserCredentials() throws IOException;
}
