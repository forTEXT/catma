package de.catma.ui.modules.main.signup;

import java.io.IOException;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.ui.UI;

import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.ui.modules.main.ErrorHandler;

public class UsernameValidator implements Validator<String>{

	private final IRemoteGitManagerPrivileged gitManagerPrivileged;

	public UsernameValidator(IRemoteGitManagerPrivileged gitManagerPrivileged) {
		this.gitManagerPrivileged = gitManagerPrivileged;
	}
	
	@Override
	public ValidationResult apply(String value, ValueContext context) {
		if (value == null || value.isEmpty()) {
			return ValidationResult.error("Username can't be empty");
		}
		try {
			if(gitManagerPrivileged.existsUserOrEmail(value) ) {
				return ValidationResult.error("username or E-Mail already taken");
			}else {
				return ValidationResult.ok();
			}
		} catch (IOException e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("username can't be checked", e);
			return ValidationResult.error("username can't be checked");
		}
	}

}
