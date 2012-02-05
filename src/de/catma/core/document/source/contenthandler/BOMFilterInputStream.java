/*
 * CATMA Computer Aided Text Markup and Analysis
 *
 *    Copyright (C) 2008-2010  University Of Hamburg
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.catma.core.document.source.contenthandler;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

/**
 * A {@link java.io.FilterInputStream} which filters the UTF-ByteOrderMark (BOM). A call
 * to {@link java.io.FilterInputStream#read()} will return the first byte after the BOM.
 *
 * @author Marco Petris
 *
 */
public class BOMFilterInputStream extends FilterInputStream {
	
	public static final byte[] UTF_8_BOM = 
			new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF};

    private final static Charset UTF8 = Charset.forName( "UTF-8" );
    private final static Charset UTF16 = Charset.forName( "UTF-16" );
    private final static Charset UTF16BE = Charset.forName( "UTF-16BE" );
    private final static Charset UTF16LE = Charset.forName( "UTF-16LE" );
    
    public BOMFilterInputStream(InputStream in, Charset charset) throws IOException {
        super(in);
		handleBOM( charset );
    }

    /**
     * Skips the BOM from <code>this</code> stream.
     * @param charset the charset of the stream
     * @throws IOException access failure to the stream
     */
    private void handleBOM( Charset charset ) throws IOException {
        if( charset.equals( UTF8 ) ) {
    		skip(3);
        }
        else if(
            charset.equals( UTF16 )
            || charset.equals( UTF16BE )
            || charset.equals( UTF16LE ) ) {
            skip( 2 );
        }
    }

    public static boolean hasBOM(URI uri) throws IOException {
    	
		URL url = uri.toURL();
		URLConnection targetURLConnection = url.openConnection();
		InputStream targetInputStream = targetURLConnection.getInputStream();
		try {
			byte[] buf = new byte[3];
			int readCount = targetInputStream.read(buf, 0, 3);
			if (readCount == 3) {
				return hasBOM(buf);
			}
		}
		finally {
			targetInputStream.close();
		}
		return false;
    }
    
    public static boolean hasBOM(byte[] buf)  {
    	if ((buf[0]==UTF_8_BOM[0]) && (buf[1]==UTF_8_BOM[1]) && (buf[2]==UTF_8_BOM[2])) {
			return true;
		}
    	
    	return false;
    }
}
