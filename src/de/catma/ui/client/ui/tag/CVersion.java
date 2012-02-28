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

package de.catma.ui.client.ui.tag;


/**
 *
 * @see CVersionable
 *
 * @author Marco Petris
 *
 */
public class CVersion {
	
	private String version;
	
	public CVersion( String version ) {
		this.version = version;
	}
	
	/**
	 * Constructor.<br>
	 * Version number is set to 1.
	 */
	public CVersion() {
		version = "1";
	}
	
	public CVersion increment() {
		return new CVersion(); //FIXME
	}
	

	/**
	 * @return the string representation of this {@link CVersion}
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
    public boolean isNewer(CVersion other) {
        return true; //FIXME
    }
}
