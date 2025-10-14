package de.catma.ui.module.main.auth;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.ui.UI;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.ui.module.main.ErrorHandler;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Checks if the given username is valid and whether an account with that username already exists.
 */
public class UsernameValidator implements Validator<String>{
	private final RemoteGitManagerPrivileged remoteGitManagerPrivileged;
	private final Pattern usernamePattern = Pattern.compile("[a-zA-Z0-9_-]+");

	public UsernameValidator(RemoteGitManagerPrivileged remoteGitManagerPrivileged) {
		this.remoteGitManagerPrivileged = remoteGitManagerPrivileged;
	}

	@Override
	public ValidationResult apply(String value, ValueContext context) {
		if (!usernamePattern.matcher(value).matches()) {
			return ValidationResult.error("Username must be alphanumeric");
		}

		try {
			if (remoteGitManagerPrivileged.emailOrUsernameExists(value) ) {
				return ValidationResult.error("Username already taken");
			}

			return ValidationResult.ok();
		}
		catch (IOException e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Failed to check username", e);
			return ValidationResult.error("Failed to check username");
		}
	}
}
