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

package de.catma.serialization.tei;

import java.util.regex.Pattern;

import de.catma.core.document.Range;

/**
 * A handler for values of attributes of {@link TeiElementName#ptr}, namely 
 * {@link Attribute#ptr_target} and {@link Attribute#ptr_type}. 
 *
 * @author Marco Petris
 *
 */
public class PtrValueHandler implements AttributeValueHandler {
	
	/**
	 * The {@link Pattern} that describes a {@link Attribute#ptr_target}.
	 */
	public static final String TARGETPATTERN = 
		".+#range\\(\\s*/\\.\\d+\\s*,\\s*/\\.\\d+\\s*\\)\\s*";
	
	/**
	 * A container for the values of {@link Attribute#ptr_target}. 
	 */
	public static class TargetValues {
		private String uri;
		private long startPoint;
		private long endPoint;
		/**
		 * @return the point after the last character of a range of text pointed
		 * to by a {@link Attribute#ptr_target}.
		 */
		public long getEndPoint() {
			return endPoint;
		}
		/**
		 * @return the URI of the target 
		 */
		public String getURI() {
			return uri;
		}
		/**
		 * @return the point before the first character of a range of text pointed
		 * to by a {@link Attribute#ptr_target}.
		 */
		public long getStartPoint() {
			return startPoint;
		}
	}
	
	/**
	 * Creates a target string with the given range and the given filename.
	 * @param range the range of the text string 
	 * @param fileName the filename <b>(deprecated!)</b>
	 * @return the new target string
	 */
	public String makeTargetFrom( Range range, String fileName ) {
		StringBuilder targetValue = new StringBuilder( fileName );
		targetValue.append( "#range( /." );
		targetValue.append( range.getStartPoint() );
		targetValue.append( ", /." );
		targetValue.append( range.getEndPoint() );
		targetValue.append( ")" );
		
		return targetValue.toString();
	}
	
	/**
	 * @return the value for {@link Attribute#ptr_type}.
	 */
	public String makePtrType() {
		return AttributeValue.type_inclusion.getValueName();
	}
	
	/**
	 * Extracts the target values from the given target string. 
	 * @param target the target string 
	 * @return the values of the target string
	 */
	public TargetValues getTargetValuesFrom( String target ) {
		
		TargetValues result = new TargetValues();
		
		if( target == null ) {
			throw new IllegalArgumentException( 
					"The attribute target in <ptr> must not be null" );
		}
		else if( !Pattern.matches( TARGETPATTERN, target ) ) {
			throw new IllegalArgumentException(
					"The attribut target " + target 
					+ " is malformed!\n It does not match regular expression "
					+ TARGETPATTERN );
		}
		
		
		String[] uri_points = target.split( "#" );
		result.uri = uri_points[0].trim();
		String[] points = uri_points[1].split( "/." );
		
		result.startPoint =
			Long.valueOf( 
				points[1].substring( 0, points[1].indexOf( ',' ) ).trim() );
		
		result.endPoint = 
			Long.valueOf( 
				points[2].substring( 0, points[2].indexOf( ')' ) ).trim() );

		return result;
	}
	
}
