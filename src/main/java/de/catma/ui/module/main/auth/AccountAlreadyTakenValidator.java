package de.catma.ui.module.main.auth;

import java.io.IOException;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.ui.UI;

import de.catma.repository.git.managers.interfaces.RemoteGitManagerCommon;
import de.catma.ui.module.main.ErrorHandler;

/**
 * checks if an account has been taken already
 * @author db
 *
 */
public class AccountAlreadyTakenValidator implements Validator<String> {
	private final RemoteGitManagerCommon commonGitManagerApi;

	public AccountAlreadyTakenValidator(RemoteGitManagerCommon commonGitManagerApi) {
		this.commonGitManagerApi = commonGitManagerApi;
	}
	
	@Override
	public ValidationResult apply(String value, ValueContext context) {
		if (value == null || value.isEmpty()) {
			return ValidationResult.error("Username or email address can't be empty");
		}
		
		try {
			if (commonGitManagerApi.existsUserOrEmail(value)) {
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
