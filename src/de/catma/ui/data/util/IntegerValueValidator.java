package de.catma.ui.data.util;

import com.vaadin.data.Validator;

public class IntegerValueValidator implements Validator {

	private boolean allowZero;
	private boolean allowNegative;
	
	public IntegerValueValidator() {
		this(true, true);
	}
	
	public IntegerValueValidator(boolean allowZero, boolean allowNegative) {
		this.allowZero = allowZero;
		this.allowNegative = allowNegative;
	}

	public boolean isValid(Object value) {
		try {
			if (value == null) {
				return false;
			}
			
			int intVal = Integer.valueOf(value.toString());
			if (!allowZero && intVal == 0) {
				return false;
			}
			if (!allowNegative && (intVal < 0)) {
				return false;
			}
			
			return true;
		}
		catch(NumberFormatException nfe) {
			return false;
		}
	}
	
	public void validate(Object value) throws InvalidValueException {
		if (!isValid(value)) {
			StringBuilder builder = new StringBuilder("The value has to be a");
			
			if (!allowZero) {
				if (allowNegative) {
					builder.append(" non zero integer");
				}
				else {
					builder.append("n integer greater than zero");
				}
			}
			else {
				if (allowNegative) {
					builder.append("n integer");
				}
				else {
					builder.append("n integer value greater or equal zero");
				}
			}
			builder.append("!");
			
			throw new InvalidValueException(builder.toString());
		}
	}
}
