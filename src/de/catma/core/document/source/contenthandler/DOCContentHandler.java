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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.zip.CRC32;

import org.apache.poi.hwpf.extractor.WordExtractor;

import de.catma.core.document.source.SourceDocumentInfo;

/**
 * A content handler for MS Word Doc based {@link de.catma.document.source.SourceDocument}s.
 *
 * @author Marco Petris
 *
 */
public class DOCContentHandler implements SourceContentHandler {

    private String content;

    public long load(
    		SourceDocumentInfo sourceDocumentInfo,
            URI uri, ProgressListener progressListener) throws IOException {

        File file = new File(uri);
        if( progressListener != null ) {
            progressListener.setIndeterminate(true,
                "FileManager.loadingFile", file.getName() );
        }
        CRC32 checksum = new CRC32();

        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));

            WordExtractor we = new WordExtractor(bis);
            content = we.getText();
            checksum.update(content.getBytes());

        }
        finally {
            if (bis != null) {
                bis.close();
            }
        }


        return checksum.getValue();
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
