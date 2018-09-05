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

import java.io.Serializable;
import java.util.Comparator;

public class PropertyToTrimmedStringCIComparator implements Comparator<Object>,
		Serializable {

	public int compare(Object o1, Object o2) {
        int r = 0;
        // Normal non-null comparison
        if (o1 != null && o2 != null) {
            r = o1.toString().trim().toLowerCase().compareTo(
            		o2.toString().trim().toLowerCase());
        } else if (o1 == o2) {
            // Objects are equal if both are null
            r = 0;
        } else {
            if (o1 == null) {
                r = -1; // null is less than non-null
            } else {
                r = 1; // non-null is greater than null
            }
        }
        return r;
	}

}
