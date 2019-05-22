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
package de.catma.document;

import de.catma.document.repository.Repository;

/**
 * Flags that indicates read or read/write access to shareable items of the 
 * {@link Repository}.
 * 
 * @author marco.petris@web.de
 *
 */
public enum AccessMode {
	READ(0),
	WRITE(1)
	;
	
	private int numericRepresentation;

	/**
	 * @param numericRepresentation has to be unique
	 */
	private AccessMode(int numericRepresentation) {
		this.numericRepresentation = numericRepresentation;
	}
	
	public int getNumericRepresentation() {
		return numericRepresentation;
	}
	
	public static AccessMode getAccessMode(int numericRepresentation) {
		for (AccessMode am : values()) {
			if (am.numericRepresentation == numericRepresentation) {
				return am;
			}
		}
		throw new IllegalArgumentException("there is no accessmode with number " + numericRepresentation);
	}

	public static AccessMode findValueOf(String name) {
		for (AccessMode am : values()) {
			if (am.name().equals(name)) {
				return am;
			}
		}
		
		return null;
	}
}
