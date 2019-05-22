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

package de.catma.document.source;

import java.io.IOException;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.universalchardet.UniversalDetector;

import de.catma.document.source.contenthandler.DOCContentHandler;
import de.catma.document.source.contenthandler.DOCXContentHandler;
import de.catma.document.source.contenthandler.HTMLContentHandler;
import de.catma.document.source.contenthandler.PDFContentHandler;
import de.catma.document.source.contenthandler.RTFContentHandler;
import de.catma.document.source.contenthandler.SourceContentHandler;
import de.catma.document.source.contenthandler.TEIContentHandler;
import de.catma.document.source.contenthandler.XML2ContentHandler;
import de.catma.document.source.contenthandler.XMLContentHandler;
import de.catma.document.source.contenthandler.StandardContentHandler;

/**
 * Handles the creation of {@link SourceDocument}s.<br>
 *
 *
 * @author marco.petris@web.de
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
                FileType.XPDF, PDFContentHandler.class );
        registerSourceContentHandler(
                FileType.HTML, HTMLContentHandler.class );
        registerSourceContentHandler(
                FileType.HTM, HTMLContentHandler.class );
        registerSourceContentHandler(
                FileType.DOC, DOCContentHandler.class );
        registerSourceContentHandler(
        		FileType.DOCX, DOCXContentHandler.class);
        registerSourceContentHandler(
        		FileType.XML2, XML2ContentHandler.class);
        registerSourceContentHandler(
        		FileType.XML, XMLContentHandler.class);
        registerSourceContentHandler(
        		FileType.TEI, TEIContentHandler.class);
	}
	
	/**
	 * Retrieves a mime type for the specified file
	 * @param fileName the name of the file
	 * @param urlConnection a link to the file's raw data
	 * @param defaultMimeType a default mime type if detection fails
	 * @return a detected mime type or the default mime type
	 */
	public String getMimeType(String fileName, URLConnection urlConnection, String defaultMimeType) {
		String contentType = urlConnection.getContentType();
		return getMimeType(fileName, contentType, defaultMimeType);
	}
	
	/**
	 * Retrieves a mime type for the specified file
	 * @param fileName the name of the file
	 * @param contentType the content type of the file
	 * @param defaultMimeType a default mime type if detection fails
	 * @return a detected mime type or the default mime type
	 */
	public String getMimeType(String fileName, String contentType, String defaultMimeType) {
		String mimeType = null;
		if ((contentType != null) && (!contentType.equals("content/unknown"))) {
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
	
	/**
	 * Retrieves a mime type for the specified file
	 * @param fileName the name of the file
	 * @param defaultMimeType a default mime type if detection fails
	 * @return a detected mime type or the default mime type
	 */
	public String getMimeType(String fileName, String defaultMimeType) {
		String mimeType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
		if (mimeType == null) {
			mimeType = defaultMimeType;
		}
		
		return mimeType;
	}
	
	/**
	 * Tries to detect the encoding of the specified file.
	 * @param urlConnection a link to the file's raw data
	 * @param rawData the raw data
	 * @param defaultEncoding a default encoding
	 * @return the detected encoding or the default encoding
	 */
	public String getEncoding(URLConnection urlConnection, byte[] rawData, String defaultEncoding) {
		String encoding = urlConnection.getContentEncoding();
		if (encoding==null) {
			return getEncoding
				(encoding, urlConnection.getContentType(), rawData, defaultEncoding);
		}
		return encoding;
	}

	/**
	 * Tries to detect the encoding of the specified file if the given encoding
	 * is null.
	 * @param encoding the given encoding
	 * @param rawData the raw data
	 * @param defaultEncoding a default encoding
	 * @return the fiven endcoding, the detected encoding or the default encoding
	 */
	public String getEncoding(
			String encoding, String contentType, byte[] rawData, String defaultEncoding) {
		if (encoding==null) {
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
				
				if (encoding.startsWith("=")) {
					encoding = encoding.substring(1);
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
	 * @param id the identifier of the source document
	 * @param sourceDocumentInfo the meta data of the source document
	 * @return the source document instance
	 * @throws IOException access failure
	 * @throws InstantiationException {@link SourceContentHandler} instantiation failure
	 * @throws IllegalAccessException {@link SourceContentHandler} instantiation failure
	 */
	public SourceDocument loadSourceDocument( 
			String id, SourceDocumentInfo sourceDocumentInfo)
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
		
		
		SourceDocument document = 
				new SourceDocument(id, handler);
		
		return document;
	}
	
	public SourceDocument loadSourceDocument( 
			String id, SourceContentHandler handler)
		throws IOException {
		
		SourceDocument document = 
				new SourceDocument(id, handler);
		
		return document;
	}
}
