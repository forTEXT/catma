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

package de.catma.core.document.source.contenthandler;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;

import de.catma.core.ExceptionHandler;

/**
 * The standard content handler which handles text bases files.
 *
 * @author Marco Petris
 *
 */
public class StandardContentHandler extends AbstractSourceContentHandler {
	
	/**
	 * UTF-8 <b>B</b>yte<b>O</b>rder<b>M</b>ark: 0xEF 0xBB 0xBF
	 */
	public static final byte[] UTF_8_BOM = 
		new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF};
	
	private Charset charset;
	private String content;
	private long size;
	
	
	/* (non-Javadoc)
	 * @see org.catma.document.source.SourceContentHandler#getContent(long, long)
	 */
	public String getContent( long startPoint,
			long endPoint ) {
		if( content.length() > endPoint ) {
			return content.substring( (int)startPoint, (int)endPoint );
		}
		else {
			return content.substring( (int)startPoint );
		}
	}

	/* (non-Javadoc)
	 * @see org.catma.document.source.SourceContentHandler#getContent(long)
	 */
	public String getContent( long startPoint ) {
		return content.substring( (int)startPoint );
	}

	public void load() throws IOException {
		// read the content 
		
		this.charset = 
			getSourceDocumentInfo().getTechInfoSet().getCharset();
		
//        Log.text( "charset " + charset );

		StringBuilder contentBuffer = new StringBuilder(); 
		
		BufferedInputStream bis = null;

		try {
			File file = new File(getSourceDocumentInfo().getURI());
			size = file.length();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bis = new BufferedInputStream(new FileInputStream(file));
            byte[] byteBuffer = new byte[65536];
            int bCount = -1;
            while ((bCount=bis.read(byteBuffer)) != -1) {
                bos.write(byteBuffer, 0, bCount);
            }

            ByteArrayInputStream toCharBis = new ByteArrayInputStream(bos.toByteArray());

			InputStream fr = null;
			if (hasUTF8BOM(file)) {
				fr = new BOMFilterInputStream( toCharBis, charset );
			}
			else {
				fr = toCharBis;
			}

			BufferedReader reader = new BufferedReader(
					new InputStreamReader( fr, charset ) );
			
			char[] buf = new char[65536];
			int cCount = -1;
	        while((cCount=reader.read(buf)) != -1) {
	        	contentBuffer.append( buf, 0, cCount);
	        }

			content = contentBuffer.toString();
		}
		finally {
			if( bis != null ) {
				bis.close();
			}
		}
	}
	
	/**
	 * Loads the content between startpoint and endpont from the given file 
	 * with the given charset.
	 * 
	 * @param uri the URI of the Source Document
	 * @param charset the characterset to use
	 * @param startPoint the startpoint before the first character
	 * @param endPoint the endpoint after the last character
	 * @return the loaded content
	 * @throws IOException Source Document access failure
	 */
	public String loadContent(URI uri, 
			Charset charset, long startPoint, long endPoint) 
		throws IOException {
		
		StringBuilder buffer = new StringBuilder();
		
		BufferedReader reader = null;
		try {
			File file = new File( uri );
			FileInputStream fr = null; 
			
			if (hasUTF8BOM(file)) {
				fr = new BOMFilterFileInputStream( file, charset );
			}
			else {
				fr = new FileInputStream(file);
			}

			reader = new BufferedReader(
					new InputStreamReader( fr, charset ) );
			
			char[] buf = new char[65536];
			int cCount = -1;
	        while((cCount=reader.read(buf)) != -1) {
	        	buffer.append( buf, 0, cCount);
	        	if( ( endPoint > -1 ) && ( buffer.length() >= endPoint ) ) {
	        		return buffer.toString();
	        	}
	        }
			
			return buffer.toString();
		}
		finally {
			if( reader != null ) {
				reader.close();
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void showBytes( File file, int byteCount ) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream( file );
			for( int i=0; i<byteCount; i++ ) {
				System.out.printf( "%1$x\n", fis.read() );
			}
			
		}
		catch( Exception exc ) {
			exc.printStackTrace();
		}
		finally {
			if( fis != null ) {
				try {
					fis.close();
				} catch( IOException ignored) {}
			}
		}
	}
	
	/**
	 * Tests if the given file has a {@link FileManager#UTF_8_BOM UTF-8-BOM}.
	 * 
	 * @param file the file to test
	 * @return true->file has UTF-8-BOM
	 */
	public boolean hasUTF8BOM( File file ) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream( file );
			byte b1 = (byte)fis.read();
			byte b2 = (byte)fis.read();
			byte b3 = (byte)fis.read();
			
			if( ( b1 != UTF_8_BOM[0] ) || 
				( b2 != UTF_8_BOM[1]) ||
				( b3 != UTF_8_BOM[2]) ) {
				return false;
			}
			
			return true;
		}
		catch( Exception exc ) {
			ExceptionHandler.log( exc );
		}
		finally {
			if( fis != null ) {
				try {
					fis.close();
				} catch( IOException ignored) {}
			}
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see org.catma.document.source.SourceContentHandler#getSize()
	 */
	public long getSize() {
		return size;
	}
}
