package de.catma.ui.module.main.auth;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.ui.UI;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerCommon;
import de.catma.ui.module.main.ErrorHandler;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Checks if the given username is valid and whether an account with that username already exists.
 */
public class UsernameValidator implements Validator<String>{
	private final RemoteGitManagerCommon remoteGitManagerCommon;
	private final Pattern usernamePattern = Pattern.compile("[a-zA-Z0-9_-]+");

	public UsernameValidator(RemoteGitManagerCommon remoteGitManagerCommon) {
		this.remoteGitManagerCommon = remoteGitManagerCommon;
	}

	@Override
	public ValidationResult apply(String value, ValueContext context) {
		if (value == null || value.isEmpty()) {
			return ValidationResult.error("Username can't be empty");
		}

		if (!usernamePattern.matcher(value).matches()) {
			return ValidationResult.error("Username must be alphanumeric");
		}

		try {
			if (remoteGitManagerCommon.existsUserOrEmail(value) ) {
				return ValidationResult.error("Username or email address already taken");
			}
			else {
				return ValidationResult.ok();
			}
		}
		catch (IOException e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("Failed to check username", e);
			return ValidationResult.error("Failed to check username");
		}
	}
}
