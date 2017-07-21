/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
			if (!allowZero) {
				if (allowNegative) {
					throw new InvalidValueException(Messages.getString("IntegerValueValidator.nonZeroInteger")); //$NON-NLS-1$
				}
				else {
					throw new InvalidValueException(Messages.getString("IntegerValueValidator.greaterThanZeroInteger")); //$NON-NLS-1$
				}
			}
			else {
				if (allowNegative) {
					throw new InvalidValueException(Messages.getString("IntegerValueValidator.hasToBeAnInteger")); //$NON-NLS-1$
				}
				else {
					throw new InvalidValueException(Messages.getString("IntegerValueValidator.greaterOrEqualToZeroInteger")); //$NON-NLS-1$
				}
			}
		}
	}
}
