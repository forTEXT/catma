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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.hwpf.extractor.WordExtractor;

import de.catma.document.source.FileOSType;

/**
 * A content handler for MS Word Doc based {@link de.catma.document.source.SourceDocument}s.
 *
 * @author marco.petris@web.des
 *
 */
public class DOCContentHandler extends AbstractSourceContentHandler {

    /* (non-Javadoc)
     * @see de.catma.document.source.contenthandler.SourceContentHandler#load()
     */
    public void load() throws IOException {
        BufferedInputStream bis = null;
        try {
        	
            bis = new BufferedInputStream(
            		getSourceDocumentInfo().getTechInfoSet().getURI().toURL().openStream());

            load(bis);
        }
        finally {
            if (bis != null) {
				bis.close();
            }
        }
    }

    /* (non-Javadoc)
     * @see de.catma.document.source.contenthandler.SourceContentHandler#load(java.io.InputStream)
     */
    public void load(InputStream is) throws IOException {
    	WordExtractor we = new WordExtractor(is);
    	String buf = we.getText();
    	
    	//it's still microsoft after all
		if (FileOSType.getFileOSType(buf).equals(FileOSType.UNIX)) {
			buf = FileOSType.convertUnixToDos(buf);
		}
		
		setContent(buf);
    }
}
