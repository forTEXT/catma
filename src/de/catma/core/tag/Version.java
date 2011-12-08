/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009  University Of Hamburg
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

package de.catma.core.tag;

import java.util.UUID;

/**
 * A version information of a {@link Versionable}. E.g. a {@link TagDefinition} or 
 * a {@link Tagset}.<br>
 * A version string has the format: 
 * {@link Integer versionNumber}_{@link UUID}<br>
 * A negative version number marks the {@link Versionable} as deleted.<br>
 * <br>
 * Note this class is immutable! Version changes return a new Version!
 *
 * @see Versionable
 *
 * @author Marco Petris
 *
 */
public class Version {
	
	private String version;
	
	public Version( String version ) {
		this.version = version;
	}
	
	/**
	 * Constructor.<br>
	 * Version number is set to 1.
	 */
	public Version() {
		version = "1";
	}
	
	public Version increment() {
		return new Version(); //FIXME
	}
	

	/**
	 * @return the string representation of this {@link Version}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return version;
	}

    /**
     * @param other the version to compare with
     * @return true if the given version number is greater than the this version number
     */
    public boolean isNewer(Version other) {
        return true; //FIXME
    }
}
