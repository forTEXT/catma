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

import com.vaadin.v7.data.Validator;

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