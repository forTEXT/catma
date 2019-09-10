package de.catma.ui.module.main.signup;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;

/**
 * Checks if password is acceptable. Currently only length is checked.
 * @author db
 *
 */
public class ChangePasswordValidator implements Validator<String> {

	private int min;
	
	public ChangePasswordValidator(int min) {
		this.min = min;
	}
	
	private static final long serialVersionUID = -966187315348144448L;

	@Override
	public ValidationResult apply(String value, ValueContext context) {
		if(value != null && ! value.isEmpty() && value.length() < min ){
			return ValidationResult.error("Password must be longer than " + (min-1) + " characters");
		}
		return ValidationResult.ok();
	}

}
