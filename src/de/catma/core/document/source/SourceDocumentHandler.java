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
import java.util.HashMap;
import java.util.Map;

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
	
	// for creating document infos, we will load just a portion of the content
	private static final int PREVIEW_CONTENT_SIZE = 2000;
	
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
		
		SourceDocument document = new SourceDocument(handler);
		
		return document;
	}

	/**
	 * The given SourceDocumentInfo will be provided with the  
	 * {@link FileOSType} and the preview content.
	 * @param sourceDocumentInfo the sourcedoc info to be updated
	 * @throws IOException Source Document access failure
	 */
	//FIXME:
//	public void updateSourceDocumentInfo( 
//			SourceDocumentInfo sourceDocumentInfo ) throws IOException {
//		Charset charset = sourceDocumentInfo.getCharset();
//
//		StandardContentHandler standardContentHandler = 
//			new StandardContentHandler();
//
//		String previewContent = 
//			standardContentHandler.loadContent( 
//					sourceDocumentInfo.getURI(), 
//					charset, 0, PREVIEW_CONTENT_SIZE, null );
//		
//		FileOSType fileOSType = FileOSType.getFileOSType( previewContent );
//		
//		sourceDocumentInfo.setPreviewContent( previewContent );
//		sourceDocumentInfo.setFileOSType( fileOSType );
//	}

	
	/**
	 * Creates a SourceDocumentInfo for the Source Document at the given location.
	 * @param fullPath the full path to the Source Document including the file name.
	 * @return the SourceDocumentInfo instance
	 * @throws IOException Source Document access failure
	 */
	//FIXME:
//	public SourceDocumentInfo getSourceDocumentInfo( URI uri ) 
//		throws IOException {
//		
//		File file = new File( uri );
//		
//		FileType fileType = FileType.getFileType( file );
//		
//		if( !fileType.equals( FileType.TEXT ) ) {
//			return new SourceDocumentInfo( fileType, uri );
//		}
//		else {
//			Charset charset = Charset.defaultCharset();
//			StandardContentHandler standardContentHandler = 
//				new StandardContentHandler();
//			
//			if( standardContentHandler.hasUTF8BOM( file ) ) {
//				charset = Charset.forName( "UTF8" );
//				//TODO: handle non BOM UTF8
//			}
//			
//			String previewContent = 
//				standardContentHandler.loadContent( 
//					uri, charset, 0,  PREVIEW_CONTENT_SIZE, null );
//			
//			FileOSType fileOSType = FileOSType.getFileOSType( previewContent );
//
//            LanguageDetector ld = new LanguageDetector();
//            Locale locale = ld.getLocale(ld.detect(previewContent));
//
//			return new SourceDocumentInfo( 
//					fileType, fileOSType, charset, locale, previewContent, uri );
//		}
//	}
	
}
