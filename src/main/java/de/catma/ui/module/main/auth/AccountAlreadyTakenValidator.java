package de.catma.ui.module.main.auth;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.ui.UI;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerCommon;
import de.catma.ui.module.main.ErrorHandler;

import java.io.IOException;

/**
 * Checks if an account with the given username or email address already exists.
 */
public class AccountAlreadyTakenValidator implements Validator<String> {
	private final RemoteGitManagerCommon remoteGitManagerCommon;

	public AccountAlreadyTakenValidator(RemoteGitManagerCommon remoteGitManagerCommon) {
		this.remoteGitManagerCommon = remoteGitManagerCommon;
	}

	@Override
	public ValidationResult apply(String value, ValueContext context) {
		if (value == null || value.isEmpty()) {
			return ValidationResult.error("Username or email address can't be empty");
		}

		try {
			if (remoteGitManagerCommon.existsUserOrEmail(value)) {
				return ValidationResult.error("Username or email address already taken");
			}
			else {
				return ValidationResult.ok();
			}
		}
		catch (IOException e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("Failed to check username or email address", e);
			return ValidationResult.error("Failed to check username or email address");
		}
	}
}
