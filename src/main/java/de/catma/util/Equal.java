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
package de.catma.util;

/**
 * Null safe testing.
 * 
 * @author marco.petris@web.de
 */
public class Equal {

	/**
	 * @param o1 can be <code>null</code>
	 * @param o2 can be <code>null</code>
	 * @return <code>true</code> if o1 and o2 are <code>null</code> or if both are
	 * {@link Object#equals(Object) equal}.
	 */
	public static boolean nullSave(Object o1, Object o2) {
		if (o1 == null) {
			return (o2 == null);
		}
		else {
			if (o2 == null) {
				return false;
			}
			else {
				return o1.equals(o2);
			}
		}
	}
	
	/**
	 * @param o1 can be <code>null</code>
	 * @param o2 can be <code>null</code>
	 * @return <code>false</code> if o1 or o2 or both are <code>null</code> or
	 * if they are not {@link Object#equals(Object) equal}.
	 */
	public static boolean nonNull(Object o1, Object o2) {
		if ((o1 == null) || (o2 == null)) {
			return false;
		}
		else {
			return o1.equals(o2);
		}
	}
}
