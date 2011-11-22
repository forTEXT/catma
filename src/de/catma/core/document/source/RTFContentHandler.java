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

package de.catma.core.document.source;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;

import org.apache.commons.io.FileUtils;

/**
 * A content handler for RTF based {@link de.catma.document.source.SourceDocument}s.
 *
 * @author Marco Petris
 *
 */
public class RTFContentHandler implements SourceContentHandler {

    private String content;

    public long load(
    		SourceDocumentInfo sourceDocumentInfo,
            URI uri, ProgressListener progressListener) throws IOException {

        RTFEditorKit rtf = new RTFEditorKit();

        Document doc = rtf.createDefaultDocument();

        File file = new File(uri);

        long checksum = FileUtils.checksumCRC32(file);

        BufferedReader input = new BufferedReader(new FileReader(file));

        try {
            if( progressListener != null ) {
                progressListener.setIndeterminate(true,
                    "FileManager.loadingFile", file.getName() );
            }

            rtf.read(input,doc,0);
            content = doc.getText(0,doc.getLength()).trim();
        }
        catch(BadLocationException ble) {
            try {
                if (input != null) {
                    input.close();
                }
            }
            catch(Throwable ignored) {}

            throw new IOException(ble);
        }
        finally {
            if (input != null) {
                input.close();
            }
        }

        return checksum;
    }

    public String getContent(long startPoint, long endPoint) {
        return content.substring((int)startPoint, (int)endPoint);
    }

    public String getContent(long startPoint) {
        return content.substring((int)startPoint);
    }

    public long getSize() {
        return content.length();
    }
}
