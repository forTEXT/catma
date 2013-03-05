/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
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
package de.catma.document.source.contenthandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import de.catma.document.source.FileType;
import de.catma.document.source.SourceDocumentHandler;

public class DefaultProtocolHandler implements ProtocolHandler {
	private String mimeType;
	private String encoding;
	private byte[] byteContent;
	
	public DefaultProtocolHandler(URI sourceDocURI, String mimeType) 
			throws IOException {
		this.mimeType = mimeType;
		handle(sourceDocURI);
	}
	
	private void handle(URI sourceDocURI) throws IOException {
		
		final String sourceDocURL = 
				sourceDocURI.toURL().toString();
		final String sourceURIPath = 
				sourceDocURI.getPath();
		
		SourceDocumentHandler sourceDocumentHandler = 
				new SourceDocumentHandler();
		
		URLConnection urlConnection = 
				new URL(sourceDocURL).openConnection();

		InputStream is = urlConnection.getInputStream();
		try {
			this.byteContent = IOUtils.toByteArray(is);
			if (this.mimeType == null) {
				this.mimeType = 
						sourceDocumentHandler.getMimeType(
								sourceURIPath, urlConnection, 
								FileType.TEXT.getMimeType());
			}
			
			this.encoding = Charset.defaultCharset().name();
			
			if (this.mimeType.equals(FileType.TEXT.getMimeType())
					||(this.mimeType.equals(FileType.HTML.getMimeType()))) {
				this.encoding = 
						sourceDocumentHandler.getEncoding(
								urlConnection, 
								byteContent, 
								Charset.defaultCharset().name());	
			}
		}
		finally {
			is.close();
		}
	
	}

	public byte[] getByteContent() {
		return this.byteContent;
	}

	public String getEncoding() {
		return this.encoding;
	}

	public String getMimeType() {
		return this.mimeType;
	}

}
