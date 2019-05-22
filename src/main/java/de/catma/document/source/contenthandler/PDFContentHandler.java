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

package de.catma.document.source.contenthandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * A content handler for PDF based {@link de.catma.document.source.SourceDocument}s.
 *
 * @author marco.petris@web.de
 *
 */
public class PDFContentHandler extends AbstractSourceContentHandler {

	/* (non-Javadoc)
	 * @see de.catma.document.source.contenthandler.SourceContentHandler#load(java.io.InputStream)
	 */
	public void load(InputStream is) throws IOException {
        PDDocument document = null;
        try {
            document = PDDocument.load(is, false);

            if (document.isEncrypted()) {
                throw new IOException("can not open pdf document because it is encrypted");
            }
            
            AccessPermission ap = document.getCurrentAccessPermission();
            if( ! ap.canExtractContent() )
            {
                throw new IOException( "You do not have permission to extract text" );
            }

            PDFTextStripper stripper = new PDFTextStripper("UTF-8");
            
            stripper.setForceParsing( false );
            stripper.setSortByPosition( false );
            stripper.setShouldSeparateByBeads( true );
            stripper.setStartPage( 1 );
            stripper.setEndPage( Integer.MAX_VALUE );

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Writer w = new OutputStreamWriter(os);
            try {
	            stripper.writeText(document, w);
            }
            finally {
            	w.close();
            }
            // some pdfs seem to include non valid unicode characters
            // and this causes problems when converting text to HTML
            // for GUI delivery and during indexing 
            setContent(os.toString().replaceAll(
            	"[^\\x09\\x0A\\x0D\\x20-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF]", "?"));
        }
        finally {
            if (document != null) {
				document.close();
            }
        }		
	}
	
    /* (non-Javadoc)
     * @see de.catma.document.source.contenthandler.SourceContentHandler#load()
     */
    public void load() throws IOException {
    	
        try {
        	InputStream is = getSourceDocumentInfo().getTechInfoSet().getURI().toURL().openStream();
        	try {
        		load(is);
        	}
        	finally {
        		is.close();
        	}
        }
        catch (Exception e) {
        	throw new IOException(e);
        }
    }

}
