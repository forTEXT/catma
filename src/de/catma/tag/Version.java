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

package de.catma.tag;

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
	
	private int version;
	private int hashCode;
	private String uid;
	
	/**
	 * @param versionString This string consists of a simple version number and 
	 * a {@link UUID} of the CATMA-application that set the version number.
	 */
	public Version( String versionString ) {
		String[] versionParts = versionString.split("_");
		version = Integer.valueOf( versionParts[0] );
		if( versionParts.length > 1 ) {
			uid = versionParts[1];
		}
		else {
			//FIXME: provide instanceUID
//			uid = TagManager.SINGLETON.getCatmaInstanceUID();
			uid = "1";
		}
		computeHashCode();
	}
	
	/**
	 * Constructor.<br>
	 * Version number is set to 1.
	 */
	public Version() {
		version = 1;
		//FIXME: provide UID
		uid = "1";
		//uid = TagManager.SINGLETON.getCatmaInstanceUID();
		computeHashCode();
	}
	
	private Version( int version, String uid ) {
		this.version = version;
		this.uid = uid;
	}
	
	/**
	 * computes the hashcode using the {@link #toString() version-string}.
	 */
	private void computeHashCode() {
		hashCode = this.toString().hashCode();
	}
	
	/**
	 * Increments the version number by 1, but note: <code>this</code> Version 
	 * is immutible, a new Version is created and returned
	 * @return the new Version
	 */
	public Version increment() {
		int newVersion = version+1;
		// FIXME: provide UID
		//String newUid = TagManager.SINGLETON.getCatmaInstanceUID();
		String newUid = "1";
		return new Version(newVersion,newUid);
	}
	
	/**
	 * Marks the {@link Versionable} as deleted. The deleted mark is a negative 
	 * version number, but note: <code>this</code> Version 
	 * is immutible, a new Version is created and returned
	 * @param deleted true->{@link Versionable} is marked deleted,
	 * false-> deleted-mark is removed, i. e. the version number gets reconverted 
	 * to its positive value.    
	 * @return a new Version if the setDeleted call had any effect ot this if
	 * the Version remained the same
	 */
	public Version setDeleted( boolean deleted ) {
		if( ( ( deleted ) && ( version > 0 ) ) 
				|| ( ( !deleted ) && ( version < 0 ) ) ) { 
			int newVersion = version*(-1);
			return new Version(newVersion,uid);
		}
		return this;
	}

	/**
	 * @return true if this Version marks its {@link Versionable} as deleted.
	 */
	public boolean isDeleted() {
		return (version < 0);		
	}
	
	/**
	 * equality is tested with {@link #toString()}
	 * @param obj can be null
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals( Object obj ) {
		if( ( obj == null ) || !( obj instanceof Version ) ) {
			return false;
		}
		return this.toString().equals( obj.toString() );
	}
	
	/**
	 * a hashcode is computed by using {@link #toString()}
	 * @return the hashcode of this version
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.hashCode;
	}

	/**
	 * @return the string representation of this {@link Version}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.valueOf( version ) 
		+ "_"
		+ uid;
	}

    /**
     * @param other the version to compare with
     * @return true if the given version number is greater than the this version number
     */
    public boolean isNewer(Version other) {
        return this.version > other.version;
    }
}
