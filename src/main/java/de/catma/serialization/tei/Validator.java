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

/**
 * Validates/Converts strings to
 * <a href="http://www.w3.org/TR/REC-xml/#dt-name">xml names</a>
 *
 * <br><br>This class is a Singleton.
 * @author Marco Petris
 *
 */
public enum Validator {
	SINGLETON;
	
	// pattern for the first character of a xml name
	private static final String NAME_START_CHAR = 
		"[:]" +
		"|[_]" +
		"|[A-Z]" +
		"|[a-z]" +
		"|[\\xC0-\\xD6]" +
		"|[\\xD8-\\xF6]" +
		"|[\\u00F8-\\u02FF]" +
		"|[\\u0370-\\u037D]" +
		"|[\\u037F-\\u1FFF]" +
		"|[\\u200C-\\u200D]" +
		"|[\\u2070-\\u218F]" +
		"|[\\u2C00-\\u2FEF]" +
		"|[\\u3001-\\uD7FF]" +
		"|[\\uF900-\\uFDCF]" +
		"|[\\uFDF0-\\uFFFD]";
//		+
//		"|[\\U00010000-\\u000EFFFF]";
	
	// pattern for characters of a xml name
	private static final String NAME_CHAR =
		NAME_START_CHAR +
		"|[-]" +
		"|[\\.]" +
		"|[0-9]" + 
		"|[\\xB7]" +
		"|[\\u0300-\\u036F]" +
		"|[\\u203F-\\u2040]";
	
	// pattern for non valid first characters of a xml name
	private static final String NOT_NAME_START_CHAR =
		"["+NAME_START_CHAR.replace( "|", "&&" ).replace( "[", "[^")+"]";
		
	// pattern for non valid characters of a xml name
	private static final String NOT_NAME_CHAR =
		"["+NAME_CHAR.replace( "|", "&&" ).replace( "[", "[^")+"]";
	
	private Pattern namePattern;
	
	/**
	 * Constructor.
	 */
	private Validator() {
		namePattern =
			Pattern.compile(
				"(" + NAME_START_CHAR + "){1}+(" + NAME_CHAR + ")*" ); 
	}
	
	/**
	 * @param nameToTest name to test
	 * @return true if the name is a valid xml name
	 */
	public boolean checkXMLName( String nameToTest ) {
		return namePattern.matcher( nameToTest ).matches();
	}
	
	/**
	 * Converts the given string to a valid xml name: all non valid characters
	 * are converted to an underscore.
	 * @param origName the original name
	 * @return the converted name
	 */
	public String convertToXMLName( String origName ) {
		
		if( ( origName != null ) && checkXMLName( origName ) ) {
			return origName;
		}
		
		if( ( origName == null ) 
				|| ( origName.equals( "" ) ) 
				|| ( origName.length() == 1 ) ) {
			return "_";
		}
		return origName.substring( 0, 1 ).replaceAll( 
				NOT_NAME_START_CHAR, "_" ) + 
				origName.substring(1,origName.length()).replaceAll(
						NOT_NAME_CHAR, "_" );
	}
}
