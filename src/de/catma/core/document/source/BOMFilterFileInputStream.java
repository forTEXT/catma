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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * A {@link FileInputStream} which filters the UTF-ByteOrderMark (BOM). A call
 * to {@link FileInputStream#read()} will return the first byte after the BOM.
 *
 * @author Marco Petris
 *
 */
public class BOMFilterFileInputStream extends FileInputStream {
	
	private final static Charset UTF8 = Charset.forName( "UTF-8" );
	private final static Charset UTF16 = Charset.forName( "UTF-16" );
	private final static Charset UTF16BE = Charset.forName( "UTF-16BE" );
	private final static Charset UTF16LE = Charset.forName( "UTF-16LE" );
	
	/**
	 * Creates a stream for the given file. If the given charset is UTF based
	 * the BOM will be stripped from the stream.
	 * 
	 * @param fullFilePath the full path including file name
	 * @param charset the charset of that file
	 * @throws IOException access failure to that file
	 */
	public BOMFilterFileInputStream( 
			String fullFilePath, Charset charset )
		throws IOException {
		super( fullFilePath );
		handleBOM( charset );
	}
	
	/**
	 * Creates a stream for the given file. If the given charset is UTF based
	 * the BOM will be stripped from the stream.
	 * 
	 * @param file the file we are trying to access
	 * @param charset the charset of that file
	 * @throws IOException access failure to that file
	 */
	public BOMFilterFileInputStream( File file, Charset charset ) 
		throws IOException {
		super( file );
		handleBOM( charset );
	}
	
	/**
	 * Skips the BOM from <code>this</code> stream.
	 * @param charset the charset of the stream
	 * @throws IOException access failure to the stream
	 */
	private void handleBOM( Charset charset ) throws IOException {
		if( charset.equals( UTF8 ) ) {
			skip( 3 );
		}
		else if( 
			charset.equals( UTF16 ) 
			|| charset.equals( UTF16BE )
			|| charset.equals( UTF16LE ) ) {
			skip( 2 );
		}
	}
}