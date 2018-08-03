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

import org.antlr.runtime.RecognitionException;

import de.catma.document.Range;
import de.catma.serialization.tei.pointer.TextFragmentIdentifier;
import de.catma.serialization.tei.pointer.TextFragmentIdentifierFactory;

/**
 * A handler for values of attributes of {@link TeiElementName#ptr}, namely 
 * {@link Attribute#ptr_target} and {@link Attribute#ptr_type}. 
 *
 * @author Marco Petris
 *
 */
public class PtrValueHandler implements AttributeValueHandler {
	
	/**
	 * A container for the values of {@link Attribute#ptr_target}. 
	 */
	public static class TargetValues {
		private String uri;
		private Range range;

		/**
		 * @return the URI of the target 
		 */
		public String getURI() {
			return uri;
		}
		
		public Range getRange() {
			return range;
		}
	}
	
	
	public String makeTargetFrom( Range range, String uri ) {
		StringBuilder targetValue = new StringBuilder( uri );
		targetValue.append( "#char=" );
		targetValue.append( range.getStartPoint() );
		targetValue.append( "," );
		targetValue.append( range.getEndPoint() );
		
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
		
		if( target == null ) {
			throw new IllegalArgumentException( 
					"The attribute target in <ptr> must not be null" );
		}
		
		TargetValues result = new TargetValues();
		TextFragmentIdentifierFactory factory = new TextFragmentIdentifierFactory();
		String[] uri_fragementIdent = target.split( "#" );
		result.uri = uri_fragementIdent[0].trim();
		String fragmentIdentifier = uri_fragementIdent[1].trim();
		
		try {
			TextFragmentIdentifier textFragmentIdentifier = 
					factory.createTextFragmentIdentifier(fragmentIdentifier);
			
			result.range = textFragmentIdentifier.getRange();
			
			return result;
		} catch (RecognitionException e) {
			
			throw new IllegalArgumentException(
					"The fragment identifier " + fragmentIdentifier 
					+ " is malformed!\n It does not match RFC 5147", e );
		}
	}
	
}
