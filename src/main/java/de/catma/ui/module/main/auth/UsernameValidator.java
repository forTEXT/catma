package de.catma.ui.module.main.auth;

import java.io.IOException;
import java.util.regex.Pattern;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.ui.UI;

import de.catma.repository.git.managers.interfaces.ICommonRemoteGitManager;
import de.catma.ui.module.main.ErrorHandler;

public class UsernameValidator implements Validator<String>{

	private final ICommonRemoteGitManager commonGitManagerApi;
	private final Pattern usernamePattern = Pattern.compile("[a-zA-Z0-9_-]+");

	public UsernameValidator(ICommonRemoteGitManager commonGitManagerApi) {
		this.commonGitManagerApi = commonGitManagerApi;
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
			if(commonGitManagerApi.existsUserOrEmail(value) ) {
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
