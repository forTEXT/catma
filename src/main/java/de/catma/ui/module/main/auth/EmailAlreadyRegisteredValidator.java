package de.catma.ui.module.main.auth;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.ui.UI;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.ui.module.main.ErrorHandler;

import java.io.IOException;

/**
 * Checks whether an account with the given email address already exists.
 */
public class EmailAlreadyRegisteredValidator implements Validator<String> {
	private final RemoteGitManagerPrivileged remoteGitManagerPrivileged;

	public EmailAlreadyRegisteredValidator(RemoteGitManagerPrivileged remoteGitManagerPrivileged) {
		this.remoteGitManagerPrivileged = remoteGitManagerPrivileged;
	}

	@Override
	public ValidationResult apply(String value, ValueContext context) {
		try {
			if (remoteGitManagerPrivileged.emailOrUsernameExists(value)) {
				return ValidationResult.error("Email address is already registered");
			}

			return ValidationResult.ok();
		}
		catch (IOException e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Failed to check email address", e);
			return ValidationResult.error("Failed to check email address");
		}
	}
}
