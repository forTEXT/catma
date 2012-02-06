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

import java.io.IOException;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.universalchardet.UniversalDetector;

import de.catma.core.document.source.contenthandler.DOCContentHandler;
import de.catma.core.document.source.contenthandler.HTMLContentHandler;
import de.catma.core.document.source.contenthandler.PDFContentHandler;
import de.catma.core.document.source.contenthandler.RTFContentHandler;
import de.catma.core.document.source.contenthandler.SourceContentHandler;
import de.catma.core.document.source.contenthandler.StandardContentHandler;

/**
 * Handles the creation of {@link SourceDocument}s.<br>
 *
 *
 * @author Marco Petris
 *
 */
public class SourceDocumentHandler {
	
	// mapping of file types -> source content handlers
	private Map<FileType, Class<? extends SourceContentHandler>> typeHandlerMap;
	
	/**
	 * Setup.
	 */
	public SourceDocumentHandler() {
		typeHandlerMap = new HashMap<FileType, Class<? extends SourceContentHandler>>();
		registerSourceContentHandler(
				FileType.TEXT, StandardContentHandler.class );
        registerSourceContentHandler(
                FileType.RTF, RTFContentHandler.class );
        registerSourceContentHandler(
                FileType.PDF, PDFContentHandler.class );
        registerSourceContentHandler(
                FileType.HTML, HTMLContentHandler.class );
        registerSourceContentHandler(
                FileType.HTM, HTMLContentHandler.class );
        registerSourceContentHandler(
                FileType.DOC, DOCContentHandler.class );
	}
	
	public String getMimeType(String fileName, URLConnection urlConnection, String defaultMimeType) {
		String contentType = urlConnection.getContentType();
		String mimeType = null;
		if (contentType != null) {
			String[] contentTypeAttributes = contentType.split(";");
			if (contentTypeAttributes.length > 0) {
				mimeType  = contentTypeAttributes[0];
			}
		}
		if (mimeType == null) {
			mimeType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
			if (mimeType == null) {
				mimeType = defaultMimeType;
			}
		}
		return mimeType;
	}
	
	public String getEncoding(URLConnection urlConnection, byte[] rawData, String defaultEncoding) {
		String encoding = urlConnection.getContentEncoding();
		if (encoding==null) {
			String contentType = urlConnection.getContentType();
			if (contentType.contains("charset")) {
				String[] contentTypeAttributes = contentType.split(";");
				String charsetAttribute = null;
				for (String attribute : contentTypeAttributes) {
					if (attribute.trim().startsWith("charset")) {
						charsetAttribute = attribute;
					}
				}
				if (charsetAttribute != null) {
					encoding = charsetAttribute.trim().substring(
							charsetAttribute.indexOf("=")).toUpperCase();
				}
			}
			if (encoding == null) {
				UniversalDetector detector = new UniversalDetector(null);
				detector.handleData(rawData, 0, rawData.length);
				encoding = detector.getDetectedCharset();
				
				if (encoding == null) {
					encoding = defaultEncoding;
				}
			}
		}
		return encoding;
	}

	/**
	 * Registers the {@link SourceContentHandler} with the givent {@link FileType}.
	 * @param type the type we want to register a handler for
	 * @param contentHandlerClass The class of the content handler. 
	 * <b> SourceContentHandler need to have a default no arg constructor!</b>
	 */
	public void registerSourceContentHandler( 
		FileType type, Class<? extends SourceContentHandler> contentHandlerClass ) {
		typeHandlerMap.put(  type, contentHandlerClass );
	}
	
	/**
	 * Constructs a Source Document.
	 * 
	 * @param sourceDocumentInfo the metat data for this document
	 * @param fullPath the path to the Source Document.
	 * @param progressListener a listener which may be notified about the construction progress
	 * @return the Source Document instance
	 * @throws IOException access failure
	 * @throws InstantiationException {@link SourceContentHandler} instantiation failure
	 * @throws IllegalAccessException {@link SourceContentHandler} instantiation failure
	 */
	public SourceDocument loadSourceDocument( 
			SourceDocumentInfo sourceDocumentInfo)
		throws IOException, InstantiationException, IllegalAccessException {
		
		FileType fileType = 
				sourceDocumentInfo.getTechInfoSet().getFileType();
		
		if( fileType == null ) {
			throw new IllegalStateException( 
					"I don't know the type of this file!" );
		}
		
		SourceContentHandler handler = 
			typeHandlerMap.get( fileType ).newInstance();
		handler.setSourceDocumentInfo(sourceDocumentInfo);
		
		String title = sourceDocumentInfo.getContentInfoSet().getTitle();
		
		if ((title == null) || (title.equals("empty"))) {
			title = sourceDocumentInfo.getTechInfoSet().getURI().toString();
		}
		
		SourceDocument document = 
				new SourceDocument(title, handler);
		
		return document;
	}
	
}
