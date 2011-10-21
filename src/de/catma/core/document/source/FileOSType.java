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

package de.catma.core.document.source;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An enumeration of possible file's operating system types. A type provides the line 
 * separator of a file under the type's operating system.
 *
 * @author Marco Petris
 *
 */
public enum FileOSType {
	/**
	 * Line separator is \r\n.
	 */
	DOS("\r\n"),
	/**
	 * Line separator is \n. 
	 */
	UNIX("\n"),
	/**
	 * Line separator is \r. 
	 */
	MAC("\r"),
	/**
	 * type for system independent files (pdf,html...). 
	 * (line separator is the systems line separator)
	 */
	INDEPENDENT(System.getProperty( "line.separator" ));
	
	private String lineSeparator;
	
	/**
	 * @param lineSeparator the line separator of this type
	 */
	private FileOSType( String lineSeparator ) {
		this.lineSeparator = lineSeparator;
	}
	
	/**
	 * @return the line separator of this type
	 */
	public String getLineSeparator() {
		return lineSeparator;
	}
	
	/**
	 * Tries to determine the type by counting the dos, unix and mac line separators in
	 * the given content. The one with the most hits wins and determines the type for
	 * the given content.
	 * 
	 * @param fileContent the content to analyze
	 * @return the type of the content.
	 */
	public static FileOSType getFileOSType( String fileContent ) {
		
		int dosPatternCounter = getPatternCount( "\\r\\n", fileContent );
		int unixPatternCounter = getPatternCount( "[^\\r]\\n", fileContent );
		int macPatternCounter = getPatternCount( "\\r[^\\n]", fileContent );
		
		if( dosPatternCounter >= unixPatternCounter ) {
			if ( dosPatternCounter >= macPatternCounter ) {
				return DOS;
			}
			else {
				return MAC;
			}
		}
		else {
			if( unixPatternCounter >= macPatternCounter ) {
				return UNIX;
			}
			else {
				return MAC;
			}
		}
	}
	
	/**
	 * counts the occurrences of the given  pattern in the given content
	 * @param patternString the pattern to count
	 * @param fileContent the content ot analyze
	 * @return number of occurrences
	 */
	private static int getPatternCount( 
			String patternString, String fileContent ) {
		
		Pattern pattern = Pattern.compile( patternString );
		
		Matcher patternMatcher = 
			pattern.matcher( fileContent );
		
		int patternCounter = 0;
		
		while( patternMatcher.find() ) {
			patternCounter++;
		}
		
		return patternCounter;
	}

    /**
     * Converts old McOS 9 linefeeds to unix linfeeds.
     * @param input the content to convert
     * @return the converted content
     */
    public static String convertMacToUnix(String input) {
        Pattern linefeedPattern = Pattern.compile("\\r[^\\n]");
        StringBuilder converter = new StringBuilder(input);
        Matcher matcher = linefeedPattern.matcher(input);
        while(matcher.find()) {
            converter.replace(matcher.start(),matcher.start()+1,UNIX.getLineSeparator());
        }

        return converter.toString();
    }
}
