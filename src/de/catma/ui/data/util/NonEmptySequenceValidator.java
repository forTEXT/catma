package de.catma.ui.data.util;

import com.vaadin.data.Validator;

public class NonEmptySequenceValidator implements Validator {
	private String message;
	
	
	public NonEmptySequenceValidator(String message) {
		this.message = message;
	}

	/* (non-Javadoc)
	 * @see com.vaadin.data.Validator#isValid(java.lang.Object)
	 */
	public boolean isValid(Object value) {
		return (value != null) && !value.toString().isEmpty();
	}

	/* (non-Javadoc)
	 * @see com.vaadin.data.Validator#validate(java.lang.Object)
	 */
	public void validate(Object value) throws InvalidValueException {
		if ((value == null) || value.toString().isEmpty()) {
			throw new InvalidValueException(message);
		}
	}
}