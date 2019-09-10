package de.catma.ui.module.main.signup;

import java.io.IOException;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.ui.UI;

import de.catma.repository.git.interfaces.ICommonRemoteGitManager;
import de.catma.ui.module.main.ErrorHandler;

/**
 * checks if an account has been taken already
 * @author db
 *
 */
public class AccountAlreadyTakenValidator implements Validator<String>{

	private final ICommonRemoteGitManager commonGitManagerApi;

	public AccountAlreadyTakenValidator(ICommonRemoteGitManager commonGitManagerApi) {
		this.commonGitManagerApi = commonGitManagerApi;
	}
	
	@Override
	public ValidationResult apply(String value, ValueContext context) {
		if (value == null || value.isEmpty()) {
			return ValidationResult.error("Username or email can't be empty");
		}
		
		try {
			if(commonGitManagerApi.existsUserOrEmail(value) ) {
				return ValidationResult.error("username or email already taken");
			}else {
				return ValidationResult.ok();
			}
		} catch (IOException e) {
			((ErrorHandler)UI.getCurrent()).showAndLogError("username or email can't be checked", e);
			return ValidationResult.error("username or email can't be checked");
		}
	}

}
